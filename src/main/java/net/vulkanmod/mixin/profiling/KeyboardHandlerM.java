package net.vulkanmod.mixin.profiling;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.profiling.BuildTimeProfiler;
import net.vulkanmod.render.profiling.ProfilerOverlay;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerM {

    @Inject(method = "keyPress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void injOverlayToggle(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
            switch (key) {
                case GLFW.GLFW_KEY_F8 -> ProfilerOverlay.toggle();
                case GLFW.GLFW_KEY_F10 -> BuildTimeProfiler.startBench();
            }
        }
        else if(ProfilerOverlay.shouldRender) {
            ProfilerOverlay.onKeyPress(key);
        }
    }
}
