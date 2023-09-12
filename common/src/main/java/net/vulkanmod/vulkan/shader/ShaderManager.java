package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.vertex.CustomVertexFormat;

public class ShaderManager {
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static ShaderManager shaderManager;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        shaderManager = new ShaderManager();
    }

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    public static ShaderManager getInstance() { return shaderManager; }

    Pipeline terrainIndirectShader;
    public Pipeline terrainDirectShader;

    public ShaderManager() {
        createBasicPipelines();
    }

    private void createBasicPipelines() {
        this.terrainIndirectShader = createPipeline("terrain");

        this.terrainDirectShader = createPipeline("terrain_direct");
    }

    private Pipeline createPipeline(String name) {
        String path = String.format("basic/%s/%s", name, name);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(CustomVertexFormat.COMPRESSED_TERRAIN, path);
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders();
        return pipelineBuilder.createPipeline();
    }

    public Pipeline getTerrainShader(RenderType renderType) {
        if(Initializer.CONFIG.indirectDraw) {
            return this.terrainIndirectShader;
        }
        else {
            return this.terrainDirectShader;
        }

    }

    public Pipeline getTerrainIndirectShader(RenderType renderType) {
        return terrainIndirectShader;
    }

    public void destroyPipelines() {
        this.terrainIndirectShader.cleanUp();
        this.terrainDirectShader.cleanUp();
    }
}
