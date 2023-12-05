package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OverlayTexture.class)
public class MOverlayTexture {

    @Shadow @Final private DynamicTexture texture;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;bind()V"))
    private void overlay(DynamicTexture instance) {

        VTextureSelector.setOverlayTexture(((VAbstractTextureI)this.texture).getVulkanImage());
        VTextureSelector.setActiveTexture(1);
    }

//    @Inject(method = "<init>", at = @At(value = "RETURN", target = "Lnet/minecraft/client/texture/NativeImageBackedTexture;bindTexture()V"))
    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void overlay(CallbackInfo ci) {
        VTextureSelector.setActiveTexture(0);
    }

    @Inject(method = "setupOverlayColor", at = @At(value = "HEAD"), cancellable = true)
    private void setupOverlay(CallbackInfo ci) {
        VTextureSelector.setOverlayTexture(((VAbstractTextureI)this.texture).getVulkanImage());
        ci.cancel();
    }
}
