package net.vulkanmod.vulkan.memory;

import org.lwjgl.PointerBuffer;

public abstract class Buffer {
    protected long id;
    protected long allocation;

    protected int bufferSize;
    protected int usedBytes;
    protected int offset;

    protected MemoryType type;
    protected int usage;
    protected PointerBuffer data;

    protected Buffer(int usage, MemoryType type) {
        //TODO: check usage
        this.usage = usage;
        this.type = type;

    }

    protected void createBuffer(int bufferSize) {
        this.type.createBuffer(this, bufferSize);

        if(this.type.mappable()) {
            this.data = MemoryManager.getInstance().Map(this.allocation);
        }
    }

    public void freeBuffer() {
        MemoryManager.getInstance().addToFreeable(this);
    }

    public void reset() { usedBytes = 0; }

    public long getAllocation() { return allocation; }

    public long getUsedBytes() { return usedBytes; }

    public long getOffset() { return offset; }

    public long getId() { return id; }

    public int getBufferSize() { return bufferSize; }

    protected void setBufferSize(int size) { this.bufferSize = size; }

    protected void setId(long id) { this.id = id; }

    protected void setAllocation(long allocation) {this.allocation = allocation; }

    public BufferInfo getBufferInfo() { return new BufferInfo(this.id, this.allocation, this.bufferSize, this.type.getType()); }

    public record BufferInfo(long id, long allocation, long bufferSize, MemoryType.Type type) {

    }
}
