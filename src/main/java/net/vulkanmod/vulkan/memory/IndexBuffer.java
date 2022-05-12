package net.vulkanmod.vulkan.memory;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;

public class IndexBuffer extends Buffer {

    private long offset;

    public IndexType indexType = IndexType.SHORT;

    public IndexBuffer(int size) {
        this(size, Type.HOST_LOCAL);
    }

    public IndexBuffer(int size, Type type) {
        super(VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
        this.type = type;
        createIndexBuffer(size);
    }

    private void createIndexBuffer(int size) {
        this.type.createBuffer(this, size);
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
