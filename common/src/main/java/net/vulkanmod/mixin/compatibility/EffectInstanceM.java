package net.vulkanmod.mixin.compatibility;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.shaders.EffectProgram;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.layout.Field;
import net.vulkanmod.vulkan.shader.layout.UBO;
import net.vulkanmod.vulkan.shader.parser.GlslConverter;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.io.IOUtils;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(EffectInstance.class)
public class EffectInstanceM {

    @Shadow @Final private Map<String, Uniform> uniformMap;
    @Shadow @Final private List<Uniform> uniforms;

    private Pipeline pipeline;

    @Inject(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/EffectInstance;updateLocations()V",
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void inj(ResourceManager resourceManager, String string, CallbackInfo ci,
                     ResourceLocation resourceLocation, Resource resource, Reader reader, JsonObject jsonObject, String string2, String string3) {
        createShaders(resourceManager, string2, string3);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EffectInstance;getOrCreate(Lnet/minecraft/server/packs/resources/ResourceManager;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/EffectProgram;"))
    private EffectProgram redirectShader(ResourceManager resourceManager, Program.Type type, String string) {
        return null;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void close() {

        for (Uniform uniform : this.uniforms) {
            uniform.close();
        }

        //TODO
//        ProgramManager.releaseProgram(this);
    }

    private void createShaders(ResourceManager resourceManager, String vertexShader, String fragShader) {

        try {
            String[] vshPathInfo = this.decompose(vertexShader, ':');
            ResourceLocation vshLocation = new ResourceLocation(vshPathInfo[0], "shaders/program/" + vshPathInfo[1] + ".vsh");
            Resource resource = resourceManager.getResourceOrThrow(vshLocation);
            InputStream inputStream = resource.open();
            String vshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            String[] fshPathInfo = this.decompose(fragShader, ':');
            ResourceLocation fshLocation = new ResourceLocation(fshPathInfo[0], "shaders/program/" + fshPathInfo[1] + ".fsh");
            resource = resourceManager.getResourceOrThrow(fshLocation);
            inputStream = resource.open();
            String fshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            //TODO
            GlslConverter converter = new GlslConverter();
            Pipeline.Builder builder = new Pipeline.Builder(DefaultVertexFormat.POSITION_TEX_COLOR);

            converter.process(DefaultVertexFormat.POSITION_TEX_COLOR, vshSrc, fshSrc);
            UBO ubo = converter.getUBO();
            this.setUniformSuppliers(ubo);

            builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());
            builder.compileShaders(converter.getVshConverted(), converter.getFshConverted());

            this.pipeline = builder.createPipeline();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void setUniformSuppliers(UBO ubo) {

        for(Field field : ubo.getFields()) {
            Uniform uniform = this.uniformMap.get(field.getName());

            Supplier<MappedBuffer> supplier;
            ByteBuffer byteBuffer;

            if (uniform.getType() <= 3) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
            }
            else if (uniform.getType() <= 10) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
            }
            else {
                throw new RuntimeException("out of bounds value for uniform " + uniform);
            }

            MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
            supplier = () -> mappedBuffer;
            //TODO vec1

            field.setSupplier(supplier);
        }

    }

    private String[] decompose(String string, char c) {
        String[] strings = new String[]{"minecraft", string};
        int i = string.indexOf(c);
        if (i >= 0) {
            strings[1] = string.substring(i + 1);
            if (i >= 1) {
                strings[0] = string.substring(0, i);
            }
        }

        return strings;
    }

//    /**
//     * @author
//     * @reason
//     */
//    @Overwrite
//    public void apply() {
//        RenderSystem.assertOnGameThread();
//        this.dirty = false;
//        lastAppliedEffect = this;
//        this.blend.apply();
//        if (this.programId != lastProgramId) {
//            ProgramManager.glUseProgram(this.programId);
//            lastProgramId = this.programId;
//        }
//
//        for(int i = 0; i < this.samplerLocations.size(); ++i) {
//            String string = (String)this.samplerNames.get(i);
//            IntSupplier intSupplier = (IntSupplier)this.samplerMap.get(string);
//            if (intSupplier != null) {
//                RenderSystem.activeTexture('蓀' + i);
//                RenderSystem.enableTexture();
//                int j = intSupplier.getAsInt();
//                if (j != -1) {
//                    RenderSystem.bindTexture(j);
//                    Uniform.uploadInteger((Integer)this.samplerLocations.get(i), i);
//                }
//            }
//        }
//
//        Iterator var5 = this.uniforms.iterator();
//
//        while(var5.hasNext()) {
//            Uniform uniform = (Uniform)var5.next();
//            uniform.upload();
//        }
//
//    }
}
