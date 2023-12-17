package net.vulkanmod.mixin.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Screenshot.class)
public class ScreenshotRecorderM {

    /**
     * @author
     */
    @Overwrite
    public static NativeImage takeScreenshot(RenderTarget target) {
        int i = target.width;
        int j = target.height;

        NativeImage nativeimage = new NativeImage(i, j, false);
        GlTexture.bindTexture(target.getColorTextureId());

        //TODO screenshot might be requested when cmds have not been submitted yet
//        RenderPass renderPass = ((ExtendedRenderTarget)target).getRenderPass();
//
//        Renderer renderer = Renderer.getInstance();
//        boolean b = renderer.getBoundRenderPass() == renderPass;

        nativeimage.downloadTexture(0, true);

        //nativeimage.flipY();
        return nativeimage;
    }
}
