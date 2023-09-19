package net.vulkanmod.vulkan.shader.descriptor;

import org.lwjgl.system.MemoryUtil;

public class ManualUBO extends UBO {

    private long srcPtr;
    private int srcSize;

    public ManualUBO(int binding, int type, int size) {
        super(binding, type, size * 4, null);
    }

    @Override
    public void update(long ptr) {
        //update manually
        MemoryUtil.memCopy(this.srcPtr, ptr, this.srcSize);
    }

    public void setSrc(long ptr, int size) {
        this.srcPtr = ptr;
        this.srcSize = size;
    }
}
