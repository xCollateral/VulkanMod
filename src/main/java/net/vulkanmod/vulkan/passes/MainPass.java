package net.vulkanmod.vulkan.passes;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface MainPass {

    void begin(VkCommandBuffer commandBuffer, MemoryStack stack);
    void end(VkCommandBuffer commandBuffer);
}
