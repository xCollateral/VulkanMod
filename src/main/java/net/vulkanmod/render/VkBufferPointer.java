package net.vulkanmod.render;

public class VkBufferPointer {
    public final int i2;
    public int sizes;

    public long allocation;

    public VkBufferPointer(long allocation, int offset, int size) {

        this.allocation = allocation;
        this.sizes=size;
        this.i2=offset;
    }
}
