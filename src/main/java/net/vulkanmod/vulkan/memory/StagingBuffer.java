package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.util.VUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class StagingBuffer extends Buffer {

    public StagingBuffer(int bufferSize) {
        super(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryTypes.HOST_MEM);
        this.usedBytes = 0;
        this.offset = 0;

        this.createBuffer(bufferSize);
    }

    public void copyBuffer(int size, ByteBuffer byteBuffer) {

        if(size > this.bufferSize - this.usedBytes) {
            resizeBuffer((this.bufferSize + size) * 2);
        }

        VUtil.memcpy(this.data.getByteBuffer(0, this.bufferSize), byteBuffer, this.usedBytes);

        offset = usedBytes;
        usedBytes += size;

        //createVertexBuffer(vertexSize, vertexCount, byteBuffer);
    }

    private void resizeBuffer(int newSize) {
        MemoryManager.getInstance().addToFreeable(this);
        this.createBuffer(newSize);

        System.out.println("resized staging buffer to: " + newSize);
    }

    public void reset() {
        usedBytes = 0;
    }

    public long getOffset() {
        return offset;
    }

    public long getBufferId() {
        return id;
    }
}
