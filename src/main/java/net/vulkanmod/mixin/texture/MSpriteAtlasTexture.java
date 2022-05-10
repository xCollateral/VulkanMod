package net.vulkanmod.mixin.texture;

import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpriteAtlasTexture.class)
public class MSpriteAtlasTexture {

    private VulkanImage vulkanImage;

    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(IIII)V"))
    private void redirect(int id, int maxLevel, int width, int height) {
        //this.vulkanImage = new VulkanImage(1, 256, 256, 4, false, false);
        ((VAbstractTextureI)(this)).setVulkanImage(new VulkanImage(maxLevel + 1, width, height));
        ((VAbstractTextureI)(this)).bind();
    }

    /**
     * @author
     */
    @Overwrite
    public void applyTextureFilter(SpriteAtlasTexture.Data data) {
        //this.setFilter(false, data.maxLevel > 0);
    }
}
