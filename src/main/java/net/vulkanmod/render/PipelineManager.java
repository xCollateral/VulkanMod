package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.build.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;

import java.util.function.Function;

public abstract class PipelineManager {
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainIndirectShader;
    public static GraphicsPipeline terrainDirectShader;
    public static GraphicsPipeline fastBlitPipeline;

    private static Function<RenderType, GraphicsPipeline> shaderGetter;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    public static void setDefaultShader() {
        setShaderGetter(renderType -> Initializer.CONFIG.indirectDraw ? terrainIndirectShader : terrainDirectShader);
    }

    private static void createBasicPipelines() {
        terrainIndirectShader = createPipeline("terrain_indirect", CustomVertexFormat.COMPRESSED_TERRAIN);
        terrainDirectShader = createPipeline("terrain_direct", CustomVertexFormat.COMPRESSED_TERRAIN);
        fastBlitPipeline = createPipeline("blit", CustomVertexFormat.NONE);
    }

    private static GraphicsPipeline createPipeline(String name, VertexFormat vertexFormat) {
        String path = String.format("basic/%s/%s", name, name);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(vertexFormat, path);
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders();
        return pipelineBuilder.createGraphicsPipeline();
    }

    public static GraphicsPipeline getTerrainShader(RenderType renderType) {
        return shaderGetter.apply(renderType);
    }

    public static void setShaderGetter(Function<RenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static GraphicsPipeline getTerrainDirectShader(RenderType renderType) {
        return terrainDirectShader;
    }

    public static GraphicsPipeline getTerrainIndirectShader(RenderType renderType) {
        return terrainIndirectShader;
    }

    public static GraphicsPipeline getFastBlitPipeline() { return fastBlitPipeline; }

    public static void destroyPipelines() {
        terrainIndirectShader.cleanUp();
        terrainDirectShader.cleanUp();
        fastBlitPipeline.cleanUp();
    }
}
