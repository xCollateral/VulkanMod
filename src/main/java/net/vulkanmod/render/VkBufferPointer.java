package net.vulkanmod.render;

public class VkBufferPointer {
    public final int i2;
    private final int actualSize;
    public int size_t;

    public long allocation;

    public VkBufferPointer(long allocation, int offset, int size, int actualSize) {

        this.allocation = allocation;
        this.size_t =size;
        this.i2=offset;
        this.actualSize = actualSize;
    }
}
