package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkanmod.gl.TextureMap;
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
    protected VulkanImage vulkanImage;

    /**
     * @author
     */
    @Overwrite
    public void bind() {
        this.bindTexture();
    }

    /**
     * @author
     */
    @Overwrite
    public int getId() {
        if(this.vulkanImage != null) return TextureMap.getId(this.vulkanImage);
        return -1;
    }

    /**
     * @author
     */
    @Overwrite
    public void releaseId() {
        if(this.vulkanImage != null) {
            this.vulkanImage.free();
            this.vulkanImage = null;
        }
//        else
//            System.out.println("trying to free null image");

        if(!TextureMap.removeTexture(this.id));
//            throw new RuntimeException("texture id not found");
    }

    public void setId(int i) {
        this.id = i;
    }

    /**
     * @author
     */
    @Overwrite
    public void setFilter(boolean blur, boolean mipmap) {
        if(blur != this.blur || mipmap != this.mipmap) {
            this.blur = blur;
            this.mipmap = mipmap;

            vulkanImage.updateTextureSampler(this.blur, false, this.mipmap);
        }
    }

    @Override
    public void bindTexture() {
        if (vulkanImage != null) VTextureSelector.bindTexture(vulkanImage);
        else VTextureSelector.bindTexture(VTextureSelector.getWhiteTexture());
    }

    public VulkanImage getVulkanImage() {
        return vulkanImage;
    }

    public void setVulkanImage(VulkanImage image) {
        this.vulkanImage = image;

        TextureMap.addTexture(this.vulkanImage);
    }
}
