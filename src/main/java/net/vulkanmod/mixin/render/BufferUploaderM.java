package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BufferUploader.class)
public class BufferUploaderM {

    /**
     * @author
     */
    @Overwrite
    public static void reset() {}

    /**
     * @author
     */
    @Overwrite
    public static void drawWithShader(MeshData meshData) {
        RenderSystem.assertOnRenderThread();

        MeshData.DrawState parameters = meshData.drawState();

        Renderer renderer = Renderer.getInstance();

        if (parameters.vertexCount() > 0) {
            ShaderInstance shaderInstance = RenderSystem.getShader();

            // Prevent drawing if formats don't match to avoid disturbing visual bugs
            if (shaderInstance.getVertexFormat() != parameters.format()) {
                meshData.close();
                return;
            }

            // Used to update legacy shader uniforms
            // TODO it would be faster to allocate a buffer from stack and set all values
            shaderInstance.apply();

            GraphicsPipeline pipeline = ((ShaderMixed)(shaderInstance)).getPipeline();
            VRenderSystem.setPrimitiveTopologyGL(parameters.mode().asGLMode);
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

    /**
     * @author
     */
    @Overwrite
    public static void draw(MeshData meshData) {
        MeshData.DrawState parameters = meshData.drawState();

        if (parameters.vertexCount() > 0) {
            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

}
