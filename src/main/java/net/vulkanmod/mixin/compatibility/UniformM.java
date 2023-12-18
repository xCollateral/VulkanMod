package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Uniform.class)
public class UniformM {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static int glGetUniformLocation(int i, CharSequence charSequence) {
        //TODO
        return 1;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static int glGetAttribLocation(int i, CharSequence charSequence) {
        return 0;
    }

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    public void cancelUpload(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "uploadInteger", at = @At("HEAD"), cancellable = true)
    private static void cancelUploadInteger(int i, int j, CallbackInfo ci) {
        ci.cancel();
    }
}
