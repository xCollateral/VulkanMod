package net.vulkanmod.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class DrawUtil {

    public static void blitQuad() {
        blitQuad(0.0f, 1.0f, 1.0f, 0.0f);
    }

    public static void drawTexQuad(BufferBuilder builder, float x0, float y0, float x1, float y1, float z,
                                   float u0, float v0, float u1, float v1) {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(x0, y0, z).setUv(0.0F, 1.0F);
        bufferBuilder.addVertex(x1, y0, z).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(x1, y1, z).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex(x0, y1, z).setUv(0.0F, 0.0F);

        MeshData meshData = bufferBuilder.buildOrThrow();

        Renderer.getDrawer().draw(meshData.vertexBuffer(), VertexFormat.Mode.QUADS, meshData.drawState().format(), meshData.drawState().vertexCount());

    }

    public static void blitQuad(float x0, float y0, float x1, float y1) {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(x0, y0, 0.0f).setUv(0.0F, 1.0F);
        bufferBuilder.addVertex(x1, y0, 0.0f).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(x1, y1, 0.0f).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex(x0, y1, 0.0f).setUv(0.0F, 0.0F);

        MeshData meshData = bufferBuilder.buildOrThrow();

        Renderer.getDrawer().draw(meshData.vertexBuffer(), VertexFormat.Mode.QUADS, meshData.drawState().format(), meshData.drawState().vertexCount());

    }

    public static void drawFramebuffer(GraphicsPipeline pipeline, VulkanImage attachment) {

        Renderer.getInstance().bindGraphicsPipeline(pipeline);

        VTextureSelector.bindTexture(attachment);

        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F, true);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack posestack = RenderSystem.getModelViewStack();
        posestack.pushMatrix();
        posestack.identity();
        RenderSystem.applyModelViewMatrix();
        posestack.popMatrix();

        Renderer.getInstance().uploadAndBindUBOs(pipeline);

        blitQuad(0.0f, 0.0f, 1.0f, 1.0f);
    }
}
