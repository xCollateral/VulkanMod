package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static net.vulkanmod.vulkan.memory.MemoryManager.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Buffer {
    protected long id;
    protected long allocation;

    protected int bufferSize;
    protected int usedBytes;

    protected VertexBuffer.Type type;
    protected int usage;
    protected PointerBuffer data;

    protected Buffer(int usage) {
        //TODO: check usage
        this.usage = usage;
    }

    public void freeBuffer() {
        MemoryManager.addToFreeable(this);
    }

    public void reset() { usedBytes = 0; }

    public long getAllocation() { return allocation; }

    public long getUsedBytes() { return usedBytes; }

    public long getId() {return id; }

    protected void setBufferSize(int size) { this.bufferSize = size; }

    protected void setId(long id) { this.id = id; }

    protected void setAllocation(long allocation) {this.allocation = allocation; }

    public enum Type {
        HOST_LOCAL{
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

            void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                Copy(buffer.data, (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) buffer.bufferSize), byteBuffer, (int) bufferSize, buffer.getUsedBytes()));
            }

            void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                Copy(buffer.data, (data) -> VUtil.memcpy(byteBuffer, data.getByteBuffer(0, (int) buffer.bufferSize), 0));
            }
        },
        DEVICE_LOCAL{
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

            void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {
                try(MemoryStack stack = stackPush()) {

                    StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
                    stagingBuffer.copyBuffer((int) bufferSize, byteBuffer);

                    copyStagingtoLocalBuffer(stagingBuffer.id, stagingBuffer.offset, buffer.getId(), buffer.getUsedBytes(), bufferSize);

                }
            }

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
//                    MemoryManager.addToFreeable(stagingBuffer, stagingAllocation);
                }


            }
        };

        void createBuffer(Buffer buffer, int size) {};
        void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {};
        void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer) {};
    }
}
