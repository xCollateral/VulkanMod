package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryType;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static net.vulkanmod.vulkan.memory.MemoryManager.Copy;
import static net.vulkanmod.vulkan.memory.MemoryManager.MapAndCopy;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryTypes {
    public static MemoryType GPU_MEM;
    public static MemoryType HOST_MEM;

    public static void createMemoryTypes() {

        for(int i = 0; i < Vulkan.memoryProperties.memoryTypeCount(); ++i) {
            VkMemoryType memoryType = Vulkan.memoryProperties.memoryTypes(i);

            //GPU only Memory
            if(memoryType.propertyFlags() == VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) {
                GPU_MEM = new MemoryType() {
                    @Override
                    void createBuffer(Buffer buffer, int size) {
                        try(MemoryStack stack = stackPush()) {
                            buffer.setBufferSize(size);

                            LongBuffer pBuffer = stack.mallocLong(1);
                            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

                            MemoryManager.createBuffer(size,
                                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | buffer.usage,
                                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                                    pBuffer,
                                    pAllocation);

                            buffer.setId(pBuffer.get(0));
                            buffer.setAllocation(pAllocation.get(0));
                        }
                    }

                    @Override
                    void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
                        stagingBuffer.copyBuffer((int) bufferSize, byteBuffer);

                        copyStagingtoLocalBuffer(stagingBuffer.id, stagingBuffer.offset, buffer.getId(), buffer.getUsedBytes(), bufferSize);
                    }

                    @Override
                    void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                        try(MemoryStack stack = stackPush()) {

                            VkDevice device = Vulkan.getDevice();

                            LongBuffer pBuffer = stack.mallocLong(1);
                            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

                            MemoryManager.createBuffer(buffer.bufferSize,
                                    VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                                    pBuffer,
                                    pAllocation);

                            long stagingBuffer = pBuffer.get(0);
                            long stagingAllocation = pAllocation.get(0);

                            copyStagingtoLocalBuffer(buffer.getId(), stagingBuffer, 0, buffer.bufferSize);

                            MapAndCopy(stagingAllocation, bufferSize,
                                    (data) -> VUtil.memcpy(byteBuffer, data.getByteBuffer(0, (int) buffer.bufferSize), 0)
                            );

                            MemoryManager.freeBuffer(stagingBuffer, stagingAllocation);
//                              MemoryManager.addToFreeable(stagingBuffer, stagingAllocation);
                        }
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
                };
                GPU_MEM.type = MemoryType.Type.DEVICE_LOCAL;
            }

            if(memoryType.propertyFlags() == (VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT)) {
                HOST_MEM = new MemoryType() {
                    @Override
                    void createBuffer(Buffer buffer, int size) {
                        try(MemoryStack stack = stackPush()) {
                            buffer.setBufferSize(size);

                            LongBuffer pBuffer = stack.mallocLong(1);
                            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

                            MemoryManager.createBuffer(size,
                                    buffer.usage,
                                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                                    pBuffer,
                                    pAllocation);

                            buffer.setId(pBuffer.get(0));
                            buffer.setAllocation(pAllocation.get(0));
                        }
                    }

                    @Override
                    void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                        Copy(buffer.data, (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, (int) bufferSize, buffer.getUsedBytes()));
                    }

                    @Override
                    void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                        Copy(buffer.data, (data) -> VUtil.memcpy(byteBuffer, data.getByteBuffer(0, (int) buffer.bufferSize), 0));
                    }

                    @Override
                    void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer) {
                        Copy(buffer.data, (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, byteBuffer.remaining(), 0));
                    }

                    @Override
                    boolean mappable() {
                        return true;
                    }
                };
            }
        }

        if(GPU_MEM != null && HOST_MEM != null) return;

        //Could not find 1 or more MemoryTypes, need to use fallback
        if(HOST_MEM == null) {
            HOST_MEM = new MemoryType() {
                @Override
                void createBuffer(Buffer buffer, int size) {
                    try (MemoryStack stack = stackPush()) {
                        buffer.setBufferSize(size);

                        LongBuffer pBuffer = stack.mallocLong(1);
                        PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

                        MemoryManager.createBuffer(size,
                                buffer.usage,
                                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                                pBuffer,
                                pAllocation);

                        buffer.setId(pBuffer.get(0));
                        buffer.setAllocation(pAllocation.get(0));
                    }
                }

                @Override
                void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                    Copy(buffer.data, (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, (int) bufferSize, buffer.getUsedBytes()));
                }

                @Override
                void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                    Copy(buffer.data, (data) -> VUtil.memcpy(byteBuffer, data.getByteBuffer(0, (int) buffer.bufferSize), 0));
                }

                @Override
                void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer) {
                    Copy(buffer.data, (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, byteBuffer.remaining(), 0));
                }

                @Override
                boolean mappable() {
                    return true;
                }
            };
            return;
        }

        for(int i = 0; i < Vulkan.memoryProperties.memoryTypeCount(); ++i) {
            VkMemoryType memoryType = Vulkan.memoryProperties.memoryTypes(i);

            //gpu-cpu shared memory
            if((memoryType.propertyFlags() & (VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)) != 0) {
                GPU_MEM = new MemoryType() {
                    @Override
                    void createBuffer(Buffer buffer, int size) {
                        try (MemoryStack stack = stackPush()) {
                            buffer.setBufferSize(size);

                            LongBuffer pBuffer = stack.mallocLong(1);
                            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

                            MemoryManager.createBuffer(size,
                                    buffer.usage,
                                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                                    pBuffer,
                                    pAllocation);

                            buffer.setId(pBuffer.get(0));
                            buffer.setAllocation(pAllocation.get(0));
                        }
                    }

                    @Override
                    void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                        Copy(buffer.data, (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, (int) bufferSize, buffer.getUsedBytes()));
                    }

                    @Override
                    void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                        Copy(buffer.data, (data) -> VUtil.memcpy(byteBuffer, data.getByteBuffer(0, (int) buffer.bufferSize), 0));
                    }

                    @Override
                    void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer) {
                        Copy(buffer.data, (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, byteBuffer.remaining(), 0));
                    }

                    @Override
                    boolean mappable() {
                        return true;
                    }

                };
            }
        }

        //Could not find device memory, fallback to host memory
        GPU_MEM = HOST_MEM;
    }
}
