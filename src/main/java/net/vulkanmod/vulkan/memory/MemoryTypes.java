package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.vulkan.VkMemoryType;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryTypes {
    public static MemoryType GPU_MEM;
    public static MemoryType HOST_MEM;

    public static void createMemoryTypes() {

        for(int i = 0; i < Vulkan.memoryProperties.memoryTypeCount(); ++i) {
            VkMemoryType memoryType = Vulkan.memoryProperties.memoryTypes(i);

            //GPU only Memory
            if(memoryType.propertyFlags() == VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) {
                GPU_MEM = new DeviceLocalMemory();
                //TODO type inside own class
                GPU_MEM.type = MemoryType.Type.DEVICE_LOCAL;
            }

            if(memoryType.propertyFlags() == (VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT)) {
                HOST_MEM = new HostLocalCachedMemory();
            }
        }

        if(GPU_MEM != null && HOST_MEM != null) return;

        //Could not find 1 or more MemoryTypes, need to use fallback
        if(HOST_MEM == null) {
            HOST_MEM = new HostLocalFallbackMemory();
            if(GPU_MEM != null) return;
        }

        for(int i = 0; i < Vulkan.memoryProperties.memoryTypeCount(); ++i) {
            VkMemoryType memoryType = Vulkan.memoryProperties.memoryTypes(i);

            //gpu-cpu shared memory
            if((memoryType.propertyFlags() & (VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)) != 0) {
                GPU_MEM = new HostDeviceSharedMemory();
                return;
            }
        }

        //Could not find device memory, fallback to host memory
        GPU_MEM = HOST_MEM;
    }

    private static class DeviceLocalMemory extends MemoryType {

        @Override
        void createBuffer(Buffer buffer, int size) {
            MemoryManager.getInstance().createBuffer(buffer, size,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | buffer.usage,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT);
        }

        @Override
        void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
            stagingBuffer.copyBuffer((int) bufferSize, byteBuffer);

            copyStagingtoLocalBuffer(stagingBuffer.id, stagingBuffer.offset, buffer.getId(), buffer.getUsedBytes(), bufferSize);
        }

        @Override
        void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {

        }

        @Override
        void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer) {
            int bufferSize = byteBuffer.remaining();
            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
            stagingBuffer.copyBuffer(bufferSize, byteBuffer);

            copyStagingtoLocalBuffer(stagingBuffer.id, stagingBuffer.offset, buffer.getId(), 0, bufferSize);

        }

        @Override
        boolean mappable() {
            return false;
        }
    }

    private static abstract class MappableMemory extends MemoryType {
        @Override
        void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
            VUtil.memcpy(buffer.data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, (int) bufferSize, buffer.getUsedBytes());
        }

        @Override
        void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
            VUtil.memcpy(byteBuffer, buffer.data.getByteBuffer(0, (int) buffer.bufferSize), 0);
        }

        @Override
        void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer) {
            VUtil.memcpy(buffer.data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, byteBuffer.remaining(), 0);
        }
    }

    private static class HostLocalCachedMemory extends MappableMemory {
        @Override
        void createBuffer(Buffer buffer, int size) {

            MemoryManager.getInstance().createBuffer(buffer, size,
                    buffer.usage,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT);
        }

        @Override
        boolean mappable() {
            return true;
        }
    }

    private static class HostLocalFallbackMemory extends MappableMemory {
        @Override
        void createBuffer(Buffer buffer, int size) {
            MemoryManager.getInstance().createBuffer(buffer, size,
                    buffer.usage,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }

        @Override
        boolean mappable() {
            return true;
        }
    }

    private static class HostDeviceSharedMemory extends MappableMemory {
        @Override
        void createBuffer(Buffer buffer, int size) {
            MemoryManager.getInstance().createBuffer(buffer, size,
                    buffer.usage,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        }

        @Override
        boolean mappable() {
            return true;
        }
    }
}
