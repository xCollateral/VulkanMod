package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.framebuffer.RenderPass;

public interface ExtendedRenderTarget {

    boolean isBound();

    RenderPass getRenderPass();
}
