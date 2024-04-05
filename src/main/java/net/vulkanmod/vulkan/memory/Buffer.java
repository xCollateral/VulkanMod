package net.vulkanmod.vulkan.memory;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

public class Buffer {
    protected long id;
    protected long allocation;

    protected int bufferSize;
    protected int usedBytes;
    protected int offset;

    protected final MemoryType type;
    protected final int usage;
    public final PointerBuffer data;

    protected Buffer(int usage, MemoryType type) {
        //TODO: check usage
        this.usage = usage;
        this.type = type;
        this.data = type.mappable() ? MemoryUtil.memAllocPointer(1) : null;
    }

    protected void createBuffer(int bufferSize) {
        this.type.createBuffer(this, bufferSize);

        if(this.type.mappable()) {
            MemoryManager.getInstance().Map(this.allocation, this.data);
        }
    }

    public void freeBuffer() {
        this.type.freeBuffer(this);
    }

    public void reset() { usedBytes = 0; }

    public long getAllocation() { return allocation; }

    public int getUsedBytes() { return usedBytes; }

    public int getOffset() { return offset; }

    public long getId() { return id; }

    public int getBufferSize() { return bufferSize; }

    protected void setBufferSize(int size) { this.bufferSize = size; }

    protected void setId(long id) { this.id = id; }

    protected void setAllocation(long allocation) {this.allocation = allocation; }

    public BufferInfo getBufferInfo() { return new BufferInfo(this.id, this.allocation, this.bufferSize); }

    public record BufferInfo(long id, long allocation, long bufferSize) {

    }
}
