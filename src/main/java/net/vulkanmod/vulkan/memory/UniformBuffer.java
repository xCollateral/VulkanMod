package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.UniformState;
import org.lwjgl.system.MemoryUtil;

import static net.vulkanmod.vulkan.util.VUtil.align;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;

public class UniformBuffer extends Buffer {

    private final static int minOffset = (int) DeviceManager.deviceProperties.limits().minUniformBufferOffsetAlignment();

    public static int getAlignedSize(int uploadSize) {
        return align(uploadSize, minOffset);
    }

    public UniformBuffer(int size, MemoryType memoryType) {
        super(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, memoryType);
        this.createBuffer(size);
    }

    public boolean checkCapacity(int size) {
        final boolean b = size > this.bufferSize - this.usedBytes;
        if (b) {
            resizeBuffer((this.bufferSize + size) * 2);
        }
        return b;
    }

    public void updateOffset(int alignedSize) {
        usedBytes += alignedSize;
        usedBytes %= bufferSize;
    }

    private void resizeBuffer(int newSize) {
        MemoryManager.getInstance().addToFreeable(this);
        createBuffer(newSize);
    }

    public long getPointer() {
        return this.data.get(0) + usedBytes;
    }
    public long getBasePointer() {
        return this.data.get(0);
    }

    public void upload(UniformState uniformState) {

        MemoryUtil.memCopy(uniformState.getMappedBufferPtr().ptr, this.getPointer(), uniformState.size*4);
    }

    public int getBlockOffset() {

        return usedBytes;
    }
}
