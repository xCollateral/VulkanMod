package net.vulkanmod.mixin.render;

import net.minecraft.client.gl.*;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Pipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Shader.class)
public class ShaderMixin implements ShaderMixed {

    private Pipeline pipeline;

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void create(ResourceFactory factory, String name, VertexFormat format, CallbackInfo ci) {
        String path = "core/" + name;
        pipeline = new Pipeline(format, path);
    }

    @Inject(method = "loadProgram", at = @At("HEAD"), cancellable = true)
    private static void loadProgram(ResourceFactory factory, Program.Type type, String name, CallbackInfoReturnable<Program> cir) {
        cir.setReturnValue(null);
        cir.cancel();
    }

    @Inject(method = "loadReferences", at = @At("HEAD"), cancellable = true)
    private void loadReferences(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlUniform;bindAttribLocation(IILjava/lang/CharSequence;)V"))
    private void bindAttr(int program, int index, CharSequence name) {}

    /**
     * @author
     */
    @Overwrite
    public void close() {
        pipeline.cleanUp();
    }
}
