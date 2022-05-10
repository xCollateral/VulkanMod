package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.memory.MemoryManager.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class StagingBuffer {
    long stagingBuffer;
    long stagingAllocation;

    long bufferSize;
    long usedBytes;
    long offset;

    private PointerBuffer mappedAddress;

    public StagingBuffer(int bufferSize) {
        this.bufferSize = bufferSize;
        this.usedBytes = 0;
        this.offset = 0;

        this.createStagingBuffer(bufferSize);
        this.map();
    }

    private void createStagingBuffer(int bufferSize) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);

            createBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                    pBuffer,
                    pAllocation);

            stagingBuffer = pBuffer.get(0);
            stagingAllocation = pAllocation.get(0);
        }

    }

    public void copyBuffer(int size, ByteBuffer byteBuffer) {

        if (size > this.bufferSize - this.usedBytes) {
            //TODO
            throw new RuntimeException("trying to write buffer beyond max size.");
            //createVertexBuffer(vertexSize, vertexCount, byteBuffer);
        } else {

            try (MemoryStack stack = stackPush()) {

                Copy(this.mappedAddress,
                        (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) this.bufferSize), byteBuffer, this.usedBytes)
                );
            }

            offset = usedBytes;
            usedBytes += size;
        }

        //createVertexBuffer(vertexSize, vertexCount, byteBuffer);
    }

    private void map() {
        mappedAddress = Map(stagingAllocation);
    }

    public void reset() {
        usedBytes = 0;
    }

    public long getOffset() {
        return offset;
    }

    public long getBufferId() {
        return stagingBuffer;
    }
}
