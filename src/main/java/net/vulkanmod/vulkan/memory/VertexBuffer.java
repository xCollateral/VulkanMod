package net.vulkanmod.vulkan.memory;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static net.vulkanmod.vulkan.memory.MemoryManager.*;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class VertexBuffer extends Buffer {
    private int offset;

    public VertexBuffer(int size) {
        this(size, Type.HOST_LOCAL);
    }

    public VertexBuffer(int size, Type type) {
        super(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        this.type = type;
        createVertexBuffer(size);

    }

    private void createVertexBuffer(int size) {
        this.type.createBuffer(this, size);

        if(type == Type.HOST_LOCAL) {
            data = Map(this.allocation);
        }
    }

    public void copyToVertexBuffer(long vertexSize, long vertexCount, ByteBuffer byteBuffer) {
        int bufferSize = (int) (vertexSize * vertexCount);
//        long bufferSize = byteBuffer.limit();

        //Debug vertexBuffer
//        float floats[] = new float[(int) vertexSize / 4];
//        byteBuffer.asFloatBuffer().get(floats);

        if(bufferSize > this.bufferSize - this.usedBytes) {
            resizeBuffer((this.bufferSize + bufferSize) * 2);
        }

        copyToVertexBuffer(bufferSize, byteBuffer);
        offset = usedBytes;
        usedBytes += bufferSize;

    }

    private void copyToVertexBuffer(long bufferSize, ByteBuffer byteBuffer) {
        this.type.copyToBuffer(this, bufferSize, byteBuffer);
    }

    public void uploadWholeBuffer(ByteBuffer byteBuffer) {
        int bufferSize = (int) (byteBuffer.remaining());

        if(bufferSize > this.bufferSize - this.usedBytes) {
            resizeBuffer((this.bufferSize + bufferSize) * 2);
        }

        this.type.uploadBuffer(this, byteBuffer);
    }

    private void resizeBuffer(int newSize) {
        MemoryManager.addToFreeable(this);
        createVertexBuffer(newSize);

//        System.out.println("resized vertexBuffer to: " + newSize);
    }

    public long getOffset() { return  offset; }

}
