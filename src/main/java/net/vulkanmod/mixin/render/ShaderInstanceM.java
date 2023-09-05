package net.vulkanmod.mixin.render;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.layout.Field;
import net.vulkanmod.vulkan.shader.layout.UBO;
import net.vulkanmod.vulkan.shader.parser.GlslConverter;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(ShaderInstance.class)
public class ShaderInstanceM implements ShaderMixed {

    @Shadow @Final private Map<String, Uniform> uniformMap;
    @Shadow @Final private String name;

    @Shadow @Final @Nullable public Uniform MODEL_VIEW_MATRIX;
    @Shadow @Final @Nullable public Uniform PROJECTION_MATRIX;
    @Shadow @Final @Nullable public Uniform COLOR_MODULATOR;
    @Shadow @Final @Nullable public Uniform LINE_WIDTH;
    private Pipeline pipeline;
    boolean isLegacy = false;


    public Pipeline getPipeline() {
        return pipeline;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void create(ResourceProvider resourceProvider, String name, VertexFormat format, CallbackInfo ci) {
        if(Pipeline.class.getResourceAsStream("/assets/vulkanmod/shaders/minecraft/core/" + name + ".json") == null) {
            createLegacyShader(resourceProvider, new ResourceLocation("shaders/core/" + name + ".json"), format);
            return;
        }

        String path = "minecraft/core/" + name;
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

    /**
     * @author
     */
    @Overwrite
    public void apply() {
        RenderSystem.setShader(() -> (ShaderInstance)(Object)this);

        if(this.isLegacy) {
            if (this.MODEL_VIEW_MATRIX != null) {
                this.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
            }

            if (this.PROJECTION_MATRIX != null) {
                this.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
            }

            if (this.COLOR_MODULATOR != null) {
                this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
            }

//            if (shaderInstance.SCREEN_SIZE != null) {
//                Window window = Minecraft.getInstance().getWindow();
//                shaderInstance.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
//            }

//            if (this.LINE_WIDTH != null) {
//                this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
//            }
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void clear() {}

    private void setUniformSuppliers(UBO ubo) {

        for(Field field : ubo.getFields()) {
            Uniform uniform = this.uniformMap.get(field.getName());

            if(uniform == null) {
                throw new NullPointerException(String.format("Field: %s not present in map: %s", field.getName(), this.uniformMap));
            }

            Supplier<MappedBuffer> supplier;
            ByteBuffer byteBuffer;

            if(uniform != null) {
                if (uniform.getType() <= 3) {
                    byteBuffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
                }
                else if (uniform.getType() <= 10) {
                    byteBuffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
                }
                else {
                    throw new RuntimeException("out of bounds value for uniform " + uniform);
                }
            } else {
                Initializer.LOGGER.warn(String.format("Shader: %s field: %s not present in uniform map", this.name, field.getName()));

                //TODO
                byteBuffer = null;
            }


            MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
            supplier = () -> mappedBuffer;
            //TODO vec1

            field.setSupplier(supplier);
        }

    }

    private void createLegacyShader(ResourceProvider resourceProvider, ResourceLocation location, VertexFormat format) {
        try {
            Reader reader = resourceProvider.openAsReader(location);

            JsonObject jsonObject = GsonHelper.parse(reader);

            String string2 = GsonHelper.getAsString(jsonObject, "vertex");
            String string3 = GsonHelper.getAsString(jsonObject, "fragment");

            String vertPath = "shaders/core/" + string2 + ".vsh";
            Resource resource = resourceProvider.getResourceOrThrow(new ResourceLocation(vertPath));
            InputStream inputStream = resource.open();
            String vshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            String fragPath = "shaders/core/" + string3 + ".fsh";
            resource = resourceProvider.getResourceOrThrow(new ResourceLocation(fragPath));
            inputStream = resource.open();
            String fshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            GlslConverter converter = new GlslConverter();
            Pipeline.Builder builder = new Pipeline.Builder(format);

            converter.process(format, vshSrc, fshSrc);
            UBO ubo = converter.getUBO();
            this.setUniformSuppliers(ubo);

            builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());
            builder.compileShaders(converter.getVshConverted(), converter.getFshConverted());

            this.pipeline = builder.createPipeline();
            this.isLegacy = true;

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}

