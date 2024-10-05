package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TextureUtil.class)
public class MTextureUtil {

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int generateTextureId() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlTexture.genTextureId();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void prepareImage(NativeImage.InternalGlFormat internalGlFormat, int id, int mipLevels, int width, int height) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.bindTexture(id);
        GlTexture glTexture = GlTexture.getBoundTexture();
        VulkanImage image = glTexture.getVulkanImage();

        if (mipLevels > 0) {
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_LEVEL, mipLevels);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_LOD, 0);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_LOD, mipLevels);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL30.GL_TEXTURE_LOD_BIAS, 0.0F);
        }

        if (image == null || image.mipLevels != mipLevels || image.width != width || image.height != height) {
            if (image != null)
                image.free();

            image = new VulkanImage.Builder(width, height)
                    .setMipLevels(mipLevels + 1)
                    .setLinearFiltering(false)
                    .setClamp(false)
                    .createVulkanImage();

            glTexture.setVulkanImage(image);
            VTextureSelector.bindTexture(image);
        }
    }
}
