package net.vulkanmod.mixin.profiling;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.profiling.BuildTimeBench;
import net.vulkanmod.render.profiling.ProfilerOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerM {

    @Inject(method = "keyPress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void injOverlayToggle(long l, int i, int j, int k, int m, CallbackInfo ci) {
        if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342) && i == 297)
//        if(i == 297)
            ProfilerOverlay.shouldRender = !ProfilerOverlay.shouldRender;

        if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342) && i == 299) {
            BuildTimeBench.startBench();
        }
    }
}
