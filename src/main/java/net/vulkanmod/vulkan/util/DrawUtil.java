package net.vulkanmod.vulkan.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandBuffer;

public class DrawUtil {

    public static void blitToScreen() {
//        defualtBlit();
        fastBlit();
    }

    public static void fastBlit() {
        GraphicsPipeline blitPipeline = PipelineManager.getFastBlitPipeline();

        RenderSystem.disableCull();
        VRenderSystem.setPrimitiveTopologyGL(GL11.GL_TRIANGLES);

        Renderer renderer = Renderer.getInstance();
        renderer.bindGraphicsPipeline(blitPipeline);
        renderer.uploadAndBindUBOs(blitPipeline);

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        VK11.vkCmdDraw(commandBuffer, 3, 1, 0, 0);

        RenderSystem.enableCull();
    }

    public static void defualtBlit() {
        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack posestack = RenderSystem.getModelViewStack();
        posestack.pushMatrix();
        posestack.identity();
        RenderSystem.applyModelViewMatrix();
        posestack.popMatrix();

        ShaderInstance shaderInstance = Minecraft.getInstance().gameRenderer.blitShader;
//        RenderSystem.setShader(() -> shaderInstance);

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(-1.0f, -1.0f, 0.0f).setUv(0.0F, 1.0F);
        bufferBuilder.addVertex(1.0f, -1.0f, 0.0f).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(1.0f, 1.0f, 0.0f).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex(-1.0f, 1.0f, 0.0f).setUv(0.0F, 0.0F);
        var meshData = bufferBuilder.buildOrThrow();

        MeshData.DrawState parameters = meshData.drawState();

        Renderer renderer = Renderer.getInstance();

        GraphicsPipeline pipeline = ((ShaderMixed)(shaderInstance)).getPipeline();
        renderer.bindGraphicsPipeline(pipeline);
        renderer.uploadAndBindUBOs(pipeline);
        Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
    }
}
