package net.vulkanmod.mixin.settings.cloud;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsMixin {
    @Final
    @Shadow
    @Mutable
    private float cloudLevel;

    @Inject(method = "getCloudHeight", at = @At("HEAD"))
    private void injectCloudHeight(CallbackInfoReturnable<Float> cir) {
        cloudLevel = Initializer.CONFIG.cloudHeight;
    }
}
