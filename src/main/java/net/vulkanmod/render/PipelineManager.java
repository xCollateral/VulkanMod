package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.chunk.build.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

import java.util.function.Function;

import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;

public abstract class PipelineManager {
    private static final String shaderPath = SPIRVUtils.class.getResource("/assets/vulkanmod/shaders/").toExternalForm();
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainShaderEarlyZ, terrainShader, fastBlitPipeline;

    private static Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    public static void setDefaultShader() {
        setShaderGetter(renderType -> renderType == TerrainRenderType.TRANSLUCENT ? terrainShaderEarlyZ : terrainShader);
    }

    private static void createBasicPipelines() {
        terrainShaderEarlyZ = createPipeline("terrain","terrain", "terrain_Z", CustomVertexFormat.COMPRESSED_TERRAIN);
        terrainShader = createPipeline("terrain", "terrain", "terrain", CustomVertexFormat.COMPRESSED_TERRAIN);
        fastBlitPipeline = createPipeline("blit", "blit", "blit", CustomVertexFormat.NONE);
    }

    private static GraphicsPipeline createPipeline(String baseName, String vertName, String fragName,VertexFormat vertexFormat) {
        String pathB = String.format("basic/%s/%s", baseName, baseName);
        String pathV = String.format("basic/%s/%s", baseName, vertName);
        String pathF = String.format("basic/%s/%s", baseName, fragName);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(vertexFormat, pathB);
        pipelineBuilder.parseBindingsJSON();

        SPIRVUtils.SPIRV vertShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.vsh", shaderPath, pathV), SPIRVUtils.ShaderKind.VERTEX_SHADER);
        SPIRVUtils.SPIRV fragShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.fsh", shaderPath, pathF), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        pipelineBuilder.setSPIRVs(vertShaderSPIRV, fragShaderSPIRV);

        return pipelineBuilder.createGraphicsPipeline();
    }
    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        return shaderGetter.apply(renderType);
    }

    public static void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static GraphicsPipeline getTerrainDirectShader(RenderType renderType) {
        return terrainShader;
    }

    public static GraphicsPipeline getTerrainIndirectShader(RenderType renderType) {
        return terrainShaderEarlyZ;
    }

    public static GraphicsPipeline getFastBlitPipeline() { return fastBlitPipeline; }

    public static void destroyPipelines() {
        terrainShaderEarlyZ.cleanUp();
        terrainShader.cleanUp();
        fastBlitPipeline.cleanUp();
    }
}
