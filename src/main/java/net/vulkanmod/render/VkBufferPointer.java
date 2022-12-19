package net.vulkanmod.render;

public class VkBufferPointer {
    public final int i2;
    public int size_t;

    public long allocation;

    public VkBufferPointer(long allocation, int offset, int size) {

        this.allocation = allocation;
        this.size_t =size;
        this.i2=offset;
    }
}
