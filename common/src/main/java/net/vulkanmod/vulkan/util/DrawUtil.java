package net.vulkanmod.vulkan.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class DrawUtil {

    public static void drawFramebuffer(Framebuffer framebuffer) {
        Renderer renderer = Renderer.getInstance();

        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
//        matrix4f.setIdentity();
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.setIdentity();
        RenderSystem.applyModelViewMatrix();
        posestack.popPose();

//        drawer.uploadAndBindUBOs(drawer.blitShader);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            framebuffer.bindAsTexture(Renderer.getCommandBuffer(), stack);
        }

        ShaderInstance shaderInstance = Minecraft.getInstance().gameRenderer.blitShader;
        RenderSystem.setShader(() -> shaderInstance);

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
//        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//        bufferBuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
//        bufferBuilder.vertex(1.0D, 0.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
//        bufferBuilder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
//        bufferBuilder.vertex(0.0D, 1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(1.0D, 0.0D, 0.0D).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(0.0D, 1.0D, 0.0D).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
//        bufferBuilder.end();
//        BufferUploader._endInternal(bufferBuilder);
        BufferUploader.drawWithShader(bufferBuilder.end());

        //Draw buffer
//        Pair<BufferBuilder.DrawState, ByteBuffer> pair = bufferBuilder.popNextBuffer();
//        BufferBuilder.DrawState drawstate = pair.getFirst();

//        drawer.draw(pair.getSecond(), 7, drawstate.format(), drawstate.vertexCount());

//        drawer.drawWithoutBinding(pair.getSecond(), 7, drawstate.format(), drawstate.vertexCount());
    }
}
