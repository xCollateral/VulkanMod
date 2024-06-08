package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractTexture.class)
public abstract class MAbstractTexture implements VAbstractTextureI {
    @Shadow protected boolean blur;
    @Shadow protected boolean mipmap;
    @Shadow protected int id;

    @Shadow public abstract int getId();

    protected VulkanImage vulkanImage;

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
    public void releaseId() {
        if (this.vulkanImage != null) {
            this.vulkanImage.free();
            this.vulkanImage = null;
        }

        TextureUtil.releaseTextureId(this.id);
    }

    public void setId(int i) {
        this.id = i;
    }

    /**
     * @author
     */
    @Overwrite
    public void setFilter(boolean blur, boolean mipmap) {
        if (blur != this.blur || mipmap != this.mipmap) {
            this.blur = blur;
            this.mipmap = mipmap;

            if (this.vulkanImage != null)
                this.vulkanImage.updateTextureSampler(this.blur, false, this.mipmap);
        }
    }

    @Override
    public void bindTexture() {
        GlTexture.bindTexture(this.id);

        // TODO: probably not needed
        if (this.vulkanImage != null)
            VTextureSelector.bindTexture(this.vulkanImage);
    }

    public VulkanImage getVulkanImage() {
        if (this.vulkanImage != null)
            return this.vulkanImage;
        else {
            final GlTexture glTexture = GlTexture.getTexture(this.id);
            if (glTexture != null)
                return glTexture.getVulkanImage();
            else
                return ((VAbstractTextureI) MissingTextureAtlasSprite.getTexture()).getVulkanImage();
        }
    }

    public void setVulkanImage(VulkanImage image) {
        this.vulkanImage = image;

        if (this.id == -1)
            this.getId();
        GlTexture.setVulkanImage(this.id, this.vulkanImage);
    }
}
