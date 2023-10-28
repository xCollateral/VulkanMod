package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.IntBuffer;

@Mixin(TextureUtil.class)
public class MTextureUtil {

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int generateTextureId() {
        return GlStateManager._genTexture();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void prepareImage(NativeImage.InternalGlFormat internalGlFormat, int i, int j, int k, int l) {
/*        bind(i);
        if (j >= 0) {
            GlStateManager._texParameter(3553, 33085, j);
            GlStateManager._texParameter(3553, 33082, 0);
            GlStateManager._texParameter(3553, 33083, j);
            GlStateManager._texParameter(3553, 34049, 0.0F);
        }

        for(int m = 0; m <= j; ++m) {
            GlStateManager._texImage2D(3553, m, internalGlFormat.glFormat(), k >> m, l >> m, 0, 6408, 5121, (IntBuffer)null);
        }*/
    }
}
