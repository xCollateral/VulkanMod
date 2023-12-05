package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.vulkanmod.render.texture.SpriteUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public class MSpriteContents {

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void checkUpload(int i, int j, int k, int l, NativeImage[] nativeImages, CallbackInfo ci) {
        if(!SpriteUtil.shouldUpload())
            ci.cancel();

        SpriteUtil.addTransitionedLayout(VTextureSelector.getBoundTexture(0));
    }
}
