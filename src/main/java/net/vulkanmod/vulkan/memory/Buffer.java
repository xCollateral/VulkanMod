package net.vulkanmod.vulkan.memory;

import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;

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

    public void uploadToBuffer(int bufferSize, ByteBuffer byteBuffer)
    {
        MemoryTypes.GPU_MEM.copyToBuffer(this, bufferSize, byteBuffer);
    }

    public void freeBuffer() {
        MemoryManager.getInstance().addToFreeable(this);
    }

    public void reset() { usedBytes = 0; }

    public int getTotalSize() { return bufferSize-usedBytes; }

    public int getUsedBytes() { return usedBytes; }

    public long getId() {return id; }

    protected void setBufferSize(int size) { this.bufferSize = size; }

    protected void setId(long id) { this.id = id; }

    protected void setAllocation(long allocation) {this.allocation = allocation; }

    public BufferInfo getBufferInfo() { return new BufferInfo(this.id, this.allocation, this.bufferSize, this.type.getType()); }

    public record BufferInfo(long id, long allocation, long bufferSize, MemoryType.Type type) {

    }
}
