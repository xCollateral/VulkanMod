package net.vulkanmod.vulkan.passes;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import static org.lwjgl.vulkan.VK10.*;

public class LegacyMainPass implements MainPass {
    public static final LegacyMainPass PASS = new LegacyMainPass();

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if(!Renderer.useMode)
        {
            SwapChain swapChain = Vulkan.getSwapChain();
            swapChain.colorAttachmentLayout(stack, commandBuffer, Renderer.getCurrentImage());

            swapChain.beginRenderPass(commandBuffer, stack);
            Renderer.getInstance().setBoundFramebuffer(swapChain);

            VkViewport.Buffer pViewport = swapChain.viewport(stack);
            vkCmdSetViewport(commandBuffer, 0, pViewport);

            VkRect2D.Buffer pScissor = swapChain.scissor(stack);
            vkCmdSetScissor(commandBuffer, 0, pScissor);
        }
    }

    @Override
    public void mainTargetBindWrite() {
        if(Renderer.useMode) {
            RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
            mainTarget.bindWrite(true);
        }
    }

    @Override
    public void mainTargetUnbindWrite() {
        if(Renderer.useMode)
        {
            RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
            mainTarget.unbindWrite();
        }
    }

    @Override
    public void end(VkCommandBuffer commandBuffer) {
        if(Renderer.useMode) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                Renderer.getInstance().endRenderPass(commandBuffer);

                RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
                mainTarget.bindRead();

                SwapChain swapChain = Vulkan.getSwapChain();
                swapChain.colorAttachmentLayout(stack, commandBuffer, Renderer.getCurrentImage());

                swapChain.beginRenderPass(commandBuffer, stack);
                Renderer.getInstance().setBoundFramebuffer(swapChain);

                VkViewport.Buffer pViewport = swapChain.viewport(stack);
                vkCmdSetViewport(commandBuffer, 0, pViewport);

                VkRect2D.Buffer pScissor = swapChain.scissor(stack);
                vkCmdSetScissor(commandBuffer, 0, pScissor);

                VRenderSystem.disableBlend();
                Minecraft.getInstance().getMainRenderTarget().blitToScreen(swapChain.getWidth(), swapChain.getHeight());
            }
        }

        Renderer.getInstance().endRenderPass(commandBuffer);

        int result = vkEndCommandBuffer(commandBuffer);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }
    }
}
