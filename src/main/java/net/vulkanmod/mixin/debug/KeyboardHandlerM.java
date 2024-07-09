package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerM {

    @Shadow protected abstract boolean handleChunkDebugKeys(int i);

    @Shadow private boolean handledDebugKey;

    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z", ordinal = 0, shift = At.Shift.AFTER))
    private void chunkDebug(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        // GLFW key 296 -> F7
        // U -> Capture frustum
        this.handledDebugKey |= InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 296)
                && this.handleChunkDebugKeys(key);
    }
}
