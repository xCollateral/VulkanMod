package net.vulkanmod.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.vulkanmod.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Minecraft.class)
public class FabricMinecraftMixin {
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void redirectResourceTick(boolean bl, CallbackInfo ci, long l, int i, int j) {
        int n = Math.min(10, i) - 1;
        SpriteUtil.setDoUpload(j == n);
    }
}
