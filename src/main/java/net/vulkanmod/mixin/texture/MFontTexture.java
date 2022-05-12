package net.vulkanmod.mixin.texture;

import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.font.GlyphAtlasTexture;
import net.minecraft.client.texture.NativeImage;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlyphAtlasTexture.class)
public class MFontTexture {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(Lnet/minecraft/client/texture/NativeImage$InternalFormat;III)V"))
    private void redirect(NativeImage.InternalFormat internalFormat, int id, int width, int height) {
        //this.vulkanImage = new VulkanImage(1, 256, 256, 4, false, false);
        ((VAbstractTextureI)(this)).setVulkanImage(new VulkanImage(1, 256, 256, 4, false, false));
        //((VAbstractTextureI)(this)).bind();
    }
}
