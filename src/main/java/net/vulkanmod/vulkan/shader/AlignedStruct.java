package net.vulkanmod.vulkan.shader;

public abstract class AlignedStruct {

    protected int currentOffset = 0;

    public int getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(int offset) {
        currentOffset = offset;
    }
}
