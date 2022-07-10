package net.vulkanmod.mixin.texture;

import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.texture.AbstractTexture;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractTexture.class)
public abstract class MAbstractTexture implements VAbstractTextureI {
    @Shadow protected boolean bilinear;
    @Shadow protected boolean mipmap;
    protected VulkanImage vulkanImage;

    /**
     * @author
     */
    @Overwrite
    public void bindTexture() {
        this.bind();
    }

    /**
     * @author
     */
    @Overwrite
    public int getGlId() {
        return -1;
    }

    /**
     * @author
     */
    @Overwrite
    public void setFilter(boolean blur, boolean mipmap) {
        if(blur != this.bilinear || mipmap != this.mipmap) {
            this.bilinear = blur;
            this.mipmap = mipmap;

            vulkanImage.updateTextureSampler(this.bilinear, false, this.mipmap);
        }
    }

    @Override
    public void bind() {
        if (vulkanImage != null) VTextureSelector.bindTexture(vulkanImage);
        else VTextureSelector.bindTexture(VTextureSelector.getWhiteTexture());
    }

    public VulkanImage getVulkanImage() {
        return vulkanImage;
    }

    public void setVulkanImage(VulkanImage image) {
        this.vulkanImage = image;
    }
}
