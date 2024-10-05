package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractTexture.class)
public abstract class MAbstractTexture {
    @Shadow protected boolean blur;
    @Shadow protected boolean mipmap;

    @Shadow protected int id;

    /**
     * @author
     */
    @Overwrite
    public void bind() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::bindTexture);
        } else {
            this.bindTexture();
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void setFilter(boolean blur, boolean mipmap) {
        if (blur != this.blur || mipmap != this.mipmap) {
            this.blur = blur;
            this.mipmap = mipmap;

            GlTexture glTexture = GlTexture.getTexture(this.id);
            VulkanImage vulkanImage = glTexture.getVulkanImage();

            if (vulkanImage != null)
                vulkanImage.updateTextureSampler(this.blur, false, this.mipmap);
        }
    }

    private void bindTexture() {
        GlTexture.bindTexture(this.id);
    }
}
