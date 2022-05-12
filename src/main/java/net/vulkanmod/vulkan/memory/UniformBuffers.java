package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.vulkan.Vulkan.*;
import static net.vulkanmod.vulkan.Vulkan.getSwapChainImages;
import static net.vulkanmod.vulkan.memory.MemoryManager.*;
import static net.vulkanmod.vulkan.memory.MemoryManager.MapAndCopy;
import static net.vulkanmod.vulkan.memory.MemoryManager.createBuffer;
import static net.vulkanmod.vulkan.util.VUtil.align;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_CACHED_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class UniformBuffers {
    private List<Long> uniformBuffers;
    private List<Long> allocations;

    private long bufferSize;
    private long usedBytes;

    private final static int minOffset = (int) deviceProperties.limits().minUniformBufferOffsetAlignment();
    private final int imagesSize = getSwapChainImages().size();

    private List<PointerBuffer> mappedAdresses;

    public UniformBuffers(long size) {
        createUniformBuffers(size);
    }

    private void createUniformBuffers(long size) {
        this.bufferSize = size;

        try (MemoryStack stack = stackPush()) {

            uniformBuffers = new ArrayList<>(imagesSize);
            allocations = new ArrayList<>(imagesSize);
            mappedAdresses = new ArrayList<>(imagesSize);

            LongBuffer pBuffer = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);
//            LongBuffer pAllocation = stack.mallocLong(1);

            for (int i = 0; i < imagesSize; i++) {
                createBuffer(size,
                        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                        pBuffer,
                        pAllocation);

                uniformBuffers.add(pBuffer.get(0));
                allocations.add(pAllocation.get(0));

                mappedAdresses.add(Map(pAllocation.get(0)));
            }
        }
    }

    public void uploadUBO(ByteBuffer buffer, long offset, int frame)  {
        int size = buffer.remaining();
        int alignedSize = align(size, minOffset);
        if (alignedSize > this.bufferSize - this.usedBytes) {
            resizeBuffer((int) ((this.bufferSize + alignedSize) * 2));
        } else {
            uploadUBORaw(buffer, offset, frame);
            usedBytes += alignedSize;
        }
    }

    private void resizeBuffer(int newSize) {

        for(int i = 0; i < uniformBuffers.size(); ++i) {
            MemoryManager.addToFreeable(this.uniformBuffers.get(i), this.allocations.get(i));
        }

        createUniformBuffers(newSize);

        System.out.println("resized UniformBuffer to: " + newSize);
    }

    public void uploadUBORaw(ByteBuffer buffer, long offset, int frame) {
        Copy(mappedAdresses.get(frame) ,(data) -> VUtil.memcpy(data.getByteBuffer(0, (int) bufferSize), buffer, offset));
    }

    public void reset() {
        usedBytes = 0;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public long getId(int i) {
        return uniformBuffers.get(i);
    }

}
