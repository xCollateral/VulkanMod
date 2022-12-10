package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureAtlas.class)
public class MSpriteAtlasTexture {

    private VulkanImage vulkanImage;

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(IIII)V"))
    private void redirect(int id, int maxLevel, int width, int height) {
        //this.vulkanImage = new VulkanImage(1, 256, 256, 4, false, false);
        ((VAbstractTextureI)(this)).setVulkanImage(new VulkanImage(maxLevel + 1, width, height));
        ((VAbstractTextureI)(this)).bindTexture();
    }

    /**
     * @author
     */
    @Overwrite
    public void updateFilter(TextureAtlas.Preparations data) {
        //this.setFilter(false, data.maxLevel > 0);
    }
}
