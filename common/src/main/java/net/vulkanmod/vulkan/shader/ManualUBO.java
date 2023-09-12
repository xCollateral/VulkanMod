package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.shader.layout.UBO;
import org.lwjgl.system.MemoryUtil;

public class ManualUBO extends UBO {

    private long srcPtr;
    private int srcSize;

    public ManualUBO(int binding, int type, int size) {
        super(binding, type, size * 4, null);
    }

    public void update() {
        //update manually
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
