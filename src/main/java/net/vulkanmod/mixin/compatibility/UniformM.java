package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.shaders.Uniform;
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
}
