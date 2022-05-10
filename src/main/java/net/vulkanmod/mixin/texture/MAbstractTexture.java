package net.vulkanmod.mixin.texture;

import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.texture.AbstractTexture;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AbstractTexture.class)
public abstract class MAbstractTexture implements VAbstractTextureI {
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
    public void setFilter(boolean bilinear, boolean mipmap) {}

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
