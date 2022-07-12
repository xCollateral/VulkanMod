package net.vulkanmod.vulkan.memory;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;

public class IndexBuffer extends Buffer {

    public IndexType indexType = IndexType.SHORT;

    public IndexBuffer(int size) {
        this(size, MemoryTypes.HOST_MEM);
    }

    public IndexBuffer(int size, MemoryType type) {
        super(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, type);
        createIndexBuffer(size);
    }

    //TODO: use createBuffer instead
    private void createIndexBuffer(int size) {
        this.createBuffer(size);
    }

    public void copyBuffer(ByteBuffer buffer) {
        int size = buffer.remaining();

        //debug
//        this.idxs = new short[buffer.remaining() / 2];
//        buffer.asShortBuffer().get(idxs);

        if(size > this.bufferSize - this.usedBytes) {
            //TODO
            throw new RuntimeException("trying to write buffer beyond max size.");
            //createIndexBuffer(vertexSize, vertexCount, byteBuffer);
        }
        else {
            this.type.copyToBuffer(this, size, buffer);
            offset = usedBytes;
            usedBytes += size;
        }
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
        createIndexBuffer(newSize);

        System.out.println("resized vertexBuffer to: " + newSize);
    }

    public long getOffset() { return  offset; }


    public enum IndexType {
        SHORT(2),
        INT(4);

        public final int size;

        IndexType(int size) {
            this.size = size;
        }
    }


}
