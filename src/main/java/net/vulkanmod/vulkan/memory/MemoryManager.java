package net.vulkanmod.vulkan.memory;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.Vulkan;
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
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryManager {
    private static MemoryManager INSTANCE;

    private final VkDevice device = Vulkan.getDevice();
    private final long allocator = Vulkan.getAllocator();
    private final int Frames = Vulkan.getSwapChainImages().size();

    private int currentFrame = 0;

    private ObjectArrayList<Buffer.BufferInfo>[] freeableBuffers2 = new ObjectArrayList[Frames];
//    private static Map<Long, Boolean[]> buffersInUse = new HashMap<>();

//    private final LongOpenHashSet buffers = new LongOpenHashSet();
    private final Long2ReferenceOpenHashMap<Buffer> buffers = new Long2ReferenceOpenHashMap<>();

    private long deviceMemory = 0;
    private long nativeMemory = 0;

    public MemoryManager() {
        INSTANCE = this;

        for(int i = 0; i < Frames; ++i) {
            freeableBuffers2[i] = new ObjectArrayList<>();
        }
    }

    public static MemoryManager getInstance() {
        return INSTANCE;
    }

    public static void addMemBarrier(VkCommandBuffer handle, int vkAccessShaderReadBit, int vkAccessShaderWriteBit, long storageBuffer, int i, MemoryStack stack) {

        VkBufferMemoryBarrier.Buffer vkBufferMemoryBarrier = VkBufferMemoryBarrier.callocStack(1, stack);
        vkBufferMemoryBarrier.get(0).sType$Default()
                .buffer(storageBuffer)
                .size(854*480*4)
                .srcAccessMask(vkAccessShaderReadBit)
                .dstAccessMask(vkAccessShaderWriteBit)
                .dstQueueFamilyIndex(0)
                .offset(0);

        vkCmdPipelineBarrier(handle, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, null, vkBufferMemoryBarrier, null);
    }

    public void setCurrentFrame(int frame) {
        Validate.isTrue(frame < Frames, "Out of bounds frame index");
        this.currentFrame = frame;
    }

    public void createBuffer(long size, int usage, int properties, LongBuffer pBuffer, PointerBuffer pBufferMemory, int allocationUsage) {

        try(MemoryStack stack = stackPush()) {

            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            //bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
//
            VmaAllocationCreateInfo allocationInfo  = VmaAllocationCreateInfo.callocStack(stack);
            allocationInfo.usage(allocationUsage);
            allocationInfo.requiredFlags(properties);

            int result = vmaCreateBuffer(allocator, bufferInfo, allocationInfo, pBuffer, pBufferMemory, null);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer:" + result);
            }

//            LongBuffer pBufferMem = MemoryUtil.memLongBuffer(MemoryUtil.memAddressSafe(pBufferMemory), 1);
//
//            if(vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to create vertex buffer");
//            }
//
//            VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
//            vkGetBufferMemoryRequirements(device, pBuffer.get(0), memRequirements);
//
//            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
//            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
//            allocInfo.allocationSize(memRequirements.size());
//            allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties));
//
//            if(vkAllocateMemory(device, allocInfo, null, pBufferMem) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to allocate vertex buffer memory");
//            }
//
//            vkBindBufferMemory(device, pBuffer.get(0), pBufferMem.get(0), 0);

        }
    }

    public void createBuffer(Buffer buffer, int size, int usage, int properties) {

        try (MemoryStack stack = stackPush()) {
            buffer.setBufferSize(size);

            LongBuffer pBuffer = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

            this.createBuffer(size, usage, properties, pBuffer, pAllocation, VMA_MEMORY_USAGE_CPU_ONLY);

            buffer.setId(pBuffer.get(0));
            buffer.setAllocation(pAllocation.get(0));

            if((properties & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) > 0) {
                deviceMemory += size;
            } else {
                nativeMemory += size;
            }

            this.buffers.putIfAbsent(buffer.getId(), buffer);
        }
    }

    public void createImage(int width, int height, int format, int usage, int memProperties,
                            LongBuffer pTextureImage, PointerBuffer pTextureImageMemory, boolean host) {

        try(MemoryStack stack = stackPush()) {

            VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
            imageInfo.sType$Default();
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(host ? VK_IMAGE_TILING_LINEAR : VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(host ? VK_IMAGE_LAYOUT_PREINITIALIZED : VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.queueFamilyIndexCount(2);
            imageInfo.pQueueFamilyIndices(stack.ints(0,1));

            final VmaAllocationCreateInfo allocationInfo = host
                    ? VmaAllocationCreateInfo.callocStack(stack)
                    .flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT | VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT )
                    .usage(VMA_MEMORY_USAGE_AUTO_PREFER_HOST)
                    .requiredFlags(memProperties)
                    .pool(NULL)

                    : VmaAllocationCreateInfo.callocStack(stack)
                    .flags(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT )
                    .usage(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE)
                    .requiredFlags(memProperties)
                    .memoryTypeBits(0)
                    .pool(NULL);

            int i = vmaCreateImage(allocator, imageInfo, allocationInfo, pTextureImage, pTextureImageMemory, null);
            if(i !=VK_SUCCESS)
            {   throw new RuntimeException("Failed to create image: "+i);

            }

//            if(vkCreateImage(device, imageInfo, null, pTextureImage) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to create image");
//            }
//
//            VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
//            vkGetImageMemoryRequirements(device, pTextureImage.get(0), memRequirements);
//
//            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
//            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
//            allocInfo.allocationSize(memRequirements.size());
//            allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), memProperties));
//
//            if(vkAllocateMemory(device, allocInfo, null, pTextureImageMemory) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to allocate image memory");
//            }
//
//            vkBindImageMemory(device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
        }
    }

    public void MapAndCopy(long allocation, long bufferSize, Consumer<PointerBuffer> consumer){

        try(MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);

//            vkMapMemory(Vulkan.getDevice(), allocation, 0, bufferSize, 0, data);
//            consumer.accept(data);
//            vkUnmapMemory(Vulkan.getDevice(), allocation);

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

    public void Copy(PointerBuffer data, Consumer<PointerBuffer> consumer) {
        consumer.accept(data);
    }

    public void freeBuffer(long buffer, long allocation) {
//            vkFreeMemory(device, allocation, null);
//            vkDestroyBuffer(device, buffer, null);

        vmaDestroyBuffer(allocator, buffer, allocation);
    }

    private void freeBuffer(Buffer.BufferInfo bufferInfo) {
        vmaDestroyBuffer(allocator, bufferInfo.id(), bufferInfo.allocation());

        if(bufferInfo.type() == MemoryType.Type.DEVICE_LOCAL) {
            deviceMemory -= bufferInfo.bufferSize();
        } else {
            nativeMemory -= bufferInfo.bufferSize();
        }

        this.buffers.remove(bufferInfo.id());
    }

    public void freeImage(long image, long allocation) {
//        vkFreeMemory(device, allocation, null);
//        vkDestroyBuffer(device, buffer, null);

        vmaDestroyImage(allocator, image, allocation);
    }

    public void addToFreeable(Buffer buffer) {
        Buffer.BufferInfo bufferInfo = buffer.getBufferInfo();

        checkBuffer(bufferInfo);

        freeableBuffers2[currentFrame].add(bufferInfo);

    }

    public void freeBuffers() {

        List<Buffer.BufferInfo> bufferList = freeableBuffers2[currentFrame];
        for(Buffer.BufferInfo bufferInfo : bufferList) {

            freeBuffer(bufferInfo);
        }

        bufferList.clear();
    }

    private void checkBuffer(Buffer.BufferInfo bufferInfo) {
        if(this.buffers.get(bufferInfo.id()) == null){
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
