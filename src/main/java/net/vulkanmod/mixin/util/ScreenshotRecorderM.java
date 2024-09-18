package net.vulkanmod.mixin.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Screenshot.class)
public class ScreenshotRecorderM {

    /**
     * @author
     */
    @Overwrite
    public static NativeImage takeScreenshot(RenderTarget target) {
        int width = target.width;
        int height = target.height;

        NativeImage nativeimage = new NativeImage(width, height, false);
        GlTexture.bindTexture(target.getColorTextureId());

        // Need to submit and wait cmds if screenshot was requested
        // before the end of the frame
        Renderer.getInstance().flushCmds();

        nativeimage.downloadTexture(0, true);
        return nativeimage;
    }
}
