package net.vulkanmod.render.chunk;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.build.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

import java.util.Objects;
import java.util.function.Function;

import static net.vulkanmod.render.vertex.TerrainRenderType.CUTOUT;
import static net.vulkanmod.render.vertex.TerrainRenderType.SOLID;
import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;

public abstract class TerrainShaderManager {
    private static final String resourcePath1 = SPIRVUtils.class.getResource("/assets/vulkanmod/shaders/").toExternalForm();
    private static final String basePath = String.format("basic/%s/%s", "terrain", "terrain");
    public static VertexFormat TERRAIN_VERTEX_FORMAT;
    private static final SPIRVUtils.SPIRV vertShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.vsh", resourcePath1, basePath), SPIRVUtils.ShaderKind.VERTEX_SHADER);


    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainShaderEarlyZ;
    static GraphicsPipeline terrainShader;

    private static Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

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
        terrainShaderEarlyZ = createPipeline("terrain_Z");
        terrainShader = createPipeline("terrain");
    }

    private static GraphicsPipeline createPipeline(String fragPath) {
        String pathF = String.format("basic/%s/%s", "terrain", fragPath);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(CustomVertexFormat.COMPRESSED_TERRAIN, basePath);
        pipelineBuilder.parseBindingsJSON();


        SPIRVUtils.SPIRV fragShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.fsh", resourcePath1, pathF), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        pipelineBuilder.compileShaders2(vertShaderSPIRV, fragShaderSPIRV);
        return pipelineBuilder.createGraphicsPipeline();
    }

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        return renderType == TerrainRenderType.TRANSLUCENT ? terrainShaderEarlyZ : terrainShader;
    }

    public static void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static void destroyPipelines() {
        terrainShaderEarlyZ.cleanUp();
        terrainShader.cleanUp();
    }
}
