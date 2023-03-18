package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.shader.Pipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderInstance.class)
public class ShaderMixin implements ShaderMixed {

    private Pipeline pipeline;

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void create(ResourceProvider factory, String name, VertexFormat format, CallbackInfo ci) {
        String path = "core/" + name;
        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders();
        this.pipeline = pipelineBuilder.createPipeline();
    }

    @Inject(method = "getOrCreate", at = @At("HEAD"), cancellable = true)
    private static void loadProgram(ResourceProvider factory, Program.Type type, String name, CallbackInfoReturnable<Program> cir) {
        cir.setReturnValue(null);
        cir.cancel();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glBindAttribLocation(IILjava/lang/CharSequence;)V"))
    private void bindAttr(int program, int index, CharSequence name) {}

    /**
     * @author
     */
    @Overwrite
    public void close() {
        pipeline.cleanUp();
    }
}
