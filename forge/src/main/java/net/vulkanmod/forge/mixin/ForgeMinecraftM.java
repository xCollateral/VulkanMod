package net.vulkanmod.forge.mixin;

import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.injection.At;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.injection.Inject;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.injection.callback.LocalCapture;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public class ForgeMinecraftM {
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void redirectResourceTick(boolean bl, CallbackInfo ci, long l, Runnable runnable, int i, int j) {
        int n = Math.min(10, i) - 1;
        SpriteUtil.setDoUpload(j == n);
    }
}
