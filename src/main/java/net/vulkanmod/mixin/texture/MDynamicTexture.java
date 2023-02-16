package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DynamicTexture.class)
public class MDynamicTexture {

    @Shadow private NativeImage pixels;

    @Redirect(method = "<init>*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(III)V"))
    private void redirect(int id, int width, int height) {
        //this.vulkanImage = new VulkanImage(1, 256, 256, 4, false, false);
        ((VAbstractTextureI)(this)).setVulkanImage(new VulkanImage(this.pixels.getWidth(), this.pixels.getHeight()));
        ((VAbstractTextureI)(this)).bindTexture();
    }

}
