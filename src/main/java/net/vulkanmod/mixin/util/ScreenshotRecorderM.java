package net.vulkanmod.mixin.util;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderM {

    /**
     * @author
     */
    @Overwrite
    public static NativeImage takeScreenshot(Framebuffer framebuffer) {
        int i = framebuffer.textureWidth;
        int j = framebuffer.textureHeight;

        NativeImage nativeimage = new NativeImage(i, j, false);
        //RenderSystem.bindTexture(p_92282_.getColorTextureId());
        nativeimage.loadFromTextureImage(0, true);
        //nativeimage.flipY();
        return nativeimage;
    }
}
