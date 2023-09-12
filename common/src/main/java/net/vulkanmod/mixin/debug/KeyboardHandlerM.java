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

    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z", ordinal = 5, shift = At.Shift.AFTER))
    private void chunkDebug(long l, int i, int j, int k, int m, CallbackInfo ci) {
        this.handledDebugKey |= InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 296) && this.handleChunkDebugKeys(i);
    }
}
