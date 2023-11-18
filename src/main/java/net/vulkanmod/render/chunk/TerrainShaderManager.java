package net.vulkanmod.render.chunk;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.build.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;

import java.util.function.Function;

public abstract class TerrainShaderManager {
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static final GraphicsPipeline terrainIndirectShader=null;
    public static GraphicsPipeline terrainShader;

    private static Function<RenderType, GraphicsPipeline> shaderGetter;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    public static void setDefaultShader() {
        setShaderGetter(renderType -> terrainShader);
    }

    private static void createBasicPipelines() {
//        terrainIndirectShader = createPipeline("terrain_indirect");
        terrainShader = createPipeline("terrain");
    }

    private static GraphicsPipeline createPipeline(String name) {
        String path = String.format("basic/%s/%s", name, name);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(CustomVertexFormat.COMPRESSED_TERRAIN, path);
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

    public static void destroyPipelines() {
//        terrainIndirectShader.cleanUp();
        terrainShader.cleanUp();
    }
}
