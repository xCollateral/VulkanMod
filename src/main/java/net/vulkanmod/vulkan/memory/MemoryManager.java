package net.vulkanmod.vulkan.memory;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import jdk.jfr.StackTrace;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.Validate;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryManager {
    private static final boolean DEBUG = false;

    private static MemoryManager INSTANCE;

    private static final Long2ReferenceOpenHashMap<Buffer> buffers = new Long2ReferenceOpenHashMap<>();
    private static final Long2ReferenceOpenHashMap<VulkanImage> images = new Long2ReferenceOpenHashMap<>();

    private static final VkDevice device = Vulkan.getDevice();
    private static final long allocator = Vulkan.getAllocator();
    static int Frames;

    private static long deviceMemory = 0;
    private static long nativeMemory = 0;

    private int currentFrame = 0;

    private ObjectArrayList<Buffer.BufferInfo>[] freeableBuffers = new ObjectArrayList[Frames];
    private ObjectArrayList<VulkanImage>[] freeableImages = new ObjectArrayList[Frames];

    private ObjectArrayList<Runnable>[] frameOps = new ObjectArrayList[Frames];

    //debug
    private ObjectArrayList<StackTraceElement[]>[] stackTraces;

    public static MemoryManager getInstance() {
        return INSTANCE;
    }

    public static void createInstance(int frames) {
        Frames = frames;

        INSTANCE = new MemoryManager();
    }

    public static int getFrames() {
        return Frames;
    }

    MemoryManager() {
        for(int i = 0; i < Frames; ++i) {
            freeableBuffers[i] = new ObjectArrayList<>();
            freeableImages[i] = new ObjectArrayList<>();

            frameOps[i] = new ObjectArrayList<>();
        }

        if(DEBUG) {
            stackTraces = new ObjectArrayList[Frames];
            for(int i = 0; i < Frames; ++i) {
                stackTraces[i] = new ObjectArrayList<>();
            }
        }
    }

    public synchronized void initFrame(int frame) {
        this.setCurrentFrame(frame);
        this.freeBuffers(frame);
        this.doFrameOps(frame);
    }

    public void setCurrentFrame(int frame) {
        Validate.isTrue(frame < Frames, "Out of bounds frame index");
        this.currentFrame = frame;
    }

    public void freeAllBuffers() {
        for(int frame = 0; frame < Frames ; ++frame) {
            this.freeBuffers(frame);
            this.doFrameOps(frame);
        }

//        buffers.values().forEach(buffer -> freeBuffer(buffer.getId(), buffer.getAllocation()));
//        images.values().forEach(image -> image.doFree(this));
    }

    public void createBuffer(long size, int usage, int properties, LongBuffer pBuffer, PointerBuffer pBufferMemory) {

        try(MemoryStack stack = stackPush()) {

            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            //bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
//
            VmaAllocationCreateInfo allocationInfo  = VmaAllocationCreateInfo.callocStack(stack);
            //allocationInfo.usage(VMA_MEMORY_USAGE_CPU_ONLY);
            allocationInfo.requiredFlags(properties);

            int result = vmaCreateBuffer(allocator, bufferInfo, allocationInfo, pBuffer, pBufferMemory, null);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer:" + result);
            }

        }
    }

    public synchronized void createBuffer(Buffer buffer, int size, int usage, int properties) {

        try (MemoryStack stack = stackPush()) {
            buffer.setBufferSize(size);

            LongBuffer pBuffer = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

            this.createBuffer(size, usage, properties, pBuffer, pAllocation);

            buffer.setId(pBuffer.get(0));
            buffer.setAllocation(pAllocation.get(0));

            if((properties & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) > 0) {
                deviceMemory += size;
            } else {
                nativeMemory += size;
            }

            buffers.putIfAbsent(buffer.getId(), buffer);
        }
    }

    public static synchronized void createImage(int width, int height, int mipLevels, int format, int tiling, int usage, int memProperties,
                                   LongBuffer pTextureImage, PointerBuffer pTextureImageMemory) {

        try(MemoryStack stack = stackPush()) {

            VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(mipLevels);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
//            imageInfo.sharingMode(VK_SHARING_MODE_CONCURRENT);
            imageInfo.pQueueFamilyIndices(stack.ints(0,1));

            VmaAllocationCreateInfo allocationInfo  = VmaAllocationCreateInfo.callocStack(stack);
            //allocationInfo.usage(VMA_MEMORY_USAGE_CPU_ONLY);
            allocationInfo.requiredFlags(memProperties);

            vmaCreateImage(allocator, imageInfo, allocationInfo, pTextureImage, pTextureImageMemory, null);

        }
    }

    public static void addImage(VulkanImage image) {
        images.putIfAbsent(image.getId(), image);
    }

    public void MapAndCopy(long allocation, long bufferSize, Consumer<PointerBuffer> consumer){

        try(MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);

            vmaMapMemory(allocator, allocation, data);
            consumer.accept(data);
            vmaUnmapMemory(allocator, allocation);
        }

    }

    public PointerBuffer Map(long allocation) {
        PointerBuffer data = MemoryUtil.memAllocPointer(1);

        vmaMapMemory(allocator, allocation, data);

        return data;
    }

    public static void freeBuffer(long buffer, long allocation) {
        vmaDestroyBuffer(allocator, buffer, allocation);

        buffers.remove(buffer);
    }

    private static void freeBuffer(Buffer.BufferInfo bufferInfo) {
        vmaDestroyBuffer(allocator, bufferInfo.id(), bufferInfo.allocation());

        if(bufferInfo.type() == MemoryType.Type.DEVICE_LOCAL) {
            deviceMemory -= bufferInfo.bufferSize();
        } else {
            nativeMemory -= bufferInfo.bufferSize();
        }

        buffers.remove(bufferInfo.id());
    }

    public static void freeImage(long image, long allocation) {
        vmaDestroyImage(allocator, image, allocation);

        images.remove(image);
    }

    public synchronized void addToFreeable(Buffer buffer) {
        Buffer.BufferInfo bufferInfo = buffer.getBufferInfo();

        checkBuffer(bufferInfo);

        freeableBuffers[currentFrame].add(bufferInfo);

        if(DEBUG)
            stackTraces[currentFrame].add(new Throwable().getStackTrace());
    }

    public synchronized void addToFreeable(VulkanImage image) {
        freeableImages[currentFrame].add(image);
    }

    public synchronized void addFrameOp(Runnable runnable) {
        this.frameOps[currentFrame].add(runnable);
    }

    public void doFrameOps(int frame) {

        for(Runnable runnable : this.frameOps[frame]) {
            runnable.run();
        }

        this.frameOps[frame].clear();
    }

    private void freeBuffers(int frame) {

        List<Buffer.BufferInfo> bufferList = freeableBuffers[frame];
        for(Buffer.BufferInfo bufferInfo : bufferList) {

            freeBuffer(bufferInfo);
        }

        bufferList.clear();

        if(DEBUG)
            stackTraces[frame].clear();

        this.freeImages();
    }

    private void freeImages() {
        List<VulkanImage> bufferList = freeableImages[currentFrame];
        for(VulkanImage image : bufferList) {

            image.doFree(this);
        }

        bufferList.clear();
    }

    private void checkBuffer(Buffer.BufferInfo bufferInfo) {
        if(buffers.get(bufferInfo.id()) == null){
            throw new RuntimeException("trying to free not present buffer");
        }

    }

    public static int findMemoryType(int typeFilter, int properties) {

        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.mallocStack();
        vkGetPhysicalDeviceMemoryProperties(Vulkan.getDevice().getPhysicalDevice(), memProperties);

        for(int i = 0;i < memProperties.memoryTypeCount();i++) {
            if((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }

    public int getNativeMemoryMB() { return (int) (nativeMemory / 1048576L); }

    public int getDeviceMemoryMB() { return (int) (deviceMemory / 1048576L); }
}
