package net.vulkanmod.vulkan.memory;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class VertexBuffer extends Buffer {

    public VertexBuffer(int size, MemoryType type) {
        super(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, type);
        this.createBuffer(size);

    }

    public void copyToVertexBuffer(long vertexSize, long vertexCount, ByteBuffer byteBuffer) {
        //TODO: Skip Redundant/duplicated uploads
        // Or do upload/Copy Batching here as well
        int bufferSize = (int) (vertexSize * vertexCount);
//        long bufferSize = byteBuffer.limit();

        if(bufferSize > this.bufferSize - this.usedBytes) {
            resizeBuffer((this.bufferSize + bufferSize) * 2);
        }

        this.type.copyToBuffer(this, bufferSize, byteBuffer);
        offset = usedBytes;
        usedBytes += bufferSize;

    }

    private void resizeBuffer(int newSize) {
        this.type.freeBuffer(this);
        this.createBuffer(newSize);

//        System.out.println("resized vertexBuffer to: " + newSize);
    }

}
