package net.vulkanmod.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;

public class DrawUtil {

    public static void blitQuad() {
        blitQuad(0.0, 1.0, 1.0, 0.0);
    }

    public static void drawTexQuad(BufferBuilder builder, double x0, double y0, double x1, double y1, double z,
                                   float u0, float v0, float u1, float v1) {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(x0, y0, z).uv(0.0F, 1.0F).endVertex();
        bufferbuilder.vertex(x1, y0, z).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex(x1, y1, z).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(x0, y1, z).uv(0.0F, 0.0F).endVertex();

        BufferBuilder.RenderedBuffer renderedBuffer = bufferbuilder.end();

        Renderer.getDrawer().draw(renderedBuffer.vertexBuffer(), VertexFormat.Mode.QUADS, renderedBuffer.drawState().format(), renderedBuffer.drawState().vertexCount());

    }

    public static void blitQuad(double x0, double y0, double x1, double y1) {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(x0, y0, 0.0D).uv(0.0F, 1.0F).endVertex();
        bufferbuilder.vertex(x1, y0, 0.0D).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex(x1, y1, 0.0D).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(x0, y1, 0.0D).uv(0.0F, 0.0F).endVertex();

        BufferBuilder.RenderedBuffer renderedBuffer = bufferbuilder.end();

        Renderer.getDrawer().draw(renderedBuffer.vertexBuffer(), VertexFormat.Mode.QUADS, renderedBuffer.drawState().format(), renderedBuffer.drawState().vertexCount());

    }

    public static void drawFramebuffer(GraphicsPipeline pipeline, VulkanImage attachment) {

       boolean shouldUpdate = Renderer.getInstance().bindGraphicsPipeline(pipeline);

        VTextureSelector.bindTexture(attachment);

        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F, true);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.setIdentity();
        RenderSystem.applyModelViewMatrix();
        posestack.popPose();

        Renderer.getInstance().uploadAndBindUBOs(pipeline, shouldUpdate);

        blitQuad(0.0D, 0.0D, 1.0D, 1.0D);
    }
}
