package net.vulkanmod.vulkan.memory;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class IndexBuffer extends Buffer {

    public IndexType indexType;

    public IndexBuffer(int size, MemoryType type) {
        this(size, type, IndexType.SHORT);
    }

    public IndexBuffer(int size, MemoryType type, IndexType indexType) {
        super(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, type);
        this.indexType = indexType;

        this.createBuffer(size);
    }

    public void copyBuffer(ByteBuffer buffer) {
        int size = buffer.remaining();

        if(size > this.bufferSize - this.usedBytes) {
            throw new RuntimeException("Trying to write buffer beyond max size.");
        }
        else {
            this.type.copyToBuffer(this, size, buffer);
            offset = usedBytes;
            usedBytes += size;
        }
    }

    public enum IndexType {
        SHORT(2, VK_INDEX_TYPE_UINT16),
        INT(4, VK_INDEX_TYPE_UINT32);

        public final int size;
        public final int type;

        IndexType(int size, int type) {
            this.size = size;
            this.type = type;
        }
    }


}
