package net.vulkanmod.mixin.render;

import net.minecraft.client.gl.GlProgramManager;
import net.minecraft.client.gl.GlShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(GlProgramManager.class)
public class GlProgramManagerMixin {

    @Inject(method = "createProgram", at = @At("HEAD"), cancellable = true)
    private static void createProgram(CallbackInfoReturnable<Integer> cir) throws IOException {
        cir.setReturnValue(-1);
    }

    @Inject(method = "linkProgram", at = @At("HEAD"), cancellable = true)
    private static void linkProgram(GlShader shader, CallbackInfo ci) throws IOException {
        ci.cancel();
    }
}
