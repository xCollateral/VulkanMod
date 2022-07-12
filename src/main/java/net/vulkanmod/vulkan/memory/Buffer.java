package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static net.vulkanmod.vulkan.memory.MemoryManager.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Buffer {
    protected long id;
    protected long allocation;

    protected int bufferSize;
    protected int usedBytes;
    protected int offset;

//    protected Buffer.Type type;
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
            this.data = Map(this.allocation);
        }
    }

    public void freeBuffer() {
        MemoryManager.addToFreeable(this);
    }

    public void reset() { usedBytes = 0; }

    public long getAllocation() { return allocation; }

    public long getUsedBytes() { return usedBytes; }

    public long getId() {return id; }

    protected void setBufferSize(int size) { this.bufferSize = size; }

    protected void setId(long id) { this.id = id; }

    protected void setAllocation(long allocation) {this.allocation = allocation; }

    public BufferInfo getBufferInfo() { return new BufferInfo(this.id, this.allocation, this.bufferSize, this.type.getType()); }

    public record BufferInfo(long id, long allocation, long bufferSize, MemoryType.Type type) {}
}
