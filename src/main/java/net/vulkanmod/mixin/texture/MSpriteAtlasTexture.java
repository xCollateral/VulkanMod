package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.shader.descriptor.DescriptorManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureAtlas.class)
public class MSpriteAtlasTexture {

    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(IIII)V"))
    private void redirect(int id, int maxLevel, int width, int height) {
        VulkanImage image = new VulkanImage.Builder(width, height).setMipLevels(maxLevel + 1).createVulkanImage();
        ((VAbstractTextureI)(this)).setVulkanImage(image);
        ((VAbstractTextureI)(this)).bindTexture();
        DescriptorManager.updateAllSets(); //Ensure mipmaps updates are scheduled correctly
    }

    /**
     * @author
     */
    @Overwrite
    public void updateFilter(SpriteLoader.Preparations data) {
        //this.setFilter(false, data.maxLevel > 0);
    }
}
