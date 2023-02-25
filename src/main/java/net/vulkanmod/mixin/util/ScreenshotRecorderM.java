package net.vulkanmod.mixin.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.vulkanmod.render.chunk.TaskDispatcher;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Screenshot.class)
public class ScreenshotRecorderM {

    /**
     * @author
     */
    @Overwrite
    public static NativeImage takeScreenshot(RenderTarget framebuffer) {

        final NativeImage nativeimage = new NativeImage(framebuffer.width, framebuffer.height, false);
        //RenderSystem.bindTexture(p_92282_.getColorTextureId());
        VulkanImage.downloadTexture(framebuffer.width, framebuffer.height, nativeimage.pixels, Vulkan.getSwapChainImages().get(Drawer.getCurrentFrame()));
        //nativeimage.flipY();
        return nativeimage;
    }
}
