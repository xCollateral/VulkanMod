package net.vulkanmod.vulkan.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public class DrawUtil {

    public static void blitToScreen() {
        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.setIdentity();
        RenderSystem.applyModelViewMatrix();
        posestack.popPose();

        ShaderInstance shaderInstance = Minecraft.getInstance().gameRenderer.blitShader;
        RenderSystem.setShader(() -> shaderInstance);

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

    }
}
