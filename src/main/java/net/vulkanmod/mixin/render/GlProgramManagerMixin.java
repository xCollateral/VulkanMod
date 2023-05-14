package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Shader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(ProgramManager.class)
public class GlProgramManagerMixin {

    @Inject(method = "createProgram", at = @At("HEAD"), cancellable = true)
    private static void createProgram(CallbackInfoReturnable<Integer> cir) throws IOException {
        cir.setReturnValue(-1);
    }

    @Inject(method = "linkShader", at = @At("HEAD"), cancellable = true)
    private static void linkProgram(Shader shader, CallbackInfo ci) throws IOException {
        ci.cancel();
    }
}
