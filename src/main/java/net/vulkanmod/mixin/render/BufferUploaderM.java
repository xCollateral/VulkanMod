package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.opengl.GL11;
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
    public static void drawWithShader(BufferBuilder.RenderedBuffer buffer) {
        RenderSystem.assertOnRenderThread();
        buffer.release();

        BufferBuilder.DrawState parameters = buffer.drawState();

        Renderer renderer = Renderer.getInstance();

        if(parameters.vertexCount() <= 0) {
            return;
        }

        ShaderInstance shaderInstance = RenderSystem.getShader();
        // Used to update legacy shader uniforms
        // TODO it would be faster to allocate a buffer from stack and set all values
        shaderInstance.apply();

        GraphicsPipeline pipeline = ((ShaderMixed)(shaderInstance)).getPipeline();
        switch (parameters.mode().asGLMode) {
            case GL11.GL_LINES -> {
                VRenderSystem.topology = VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
                VRenderSystem.polygonMode = VK_POLYGON_MODE_LINE;
            }
            case GL11.GL_LINE_STRIP -> {
                VRenderSystem.topology = VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
                VRenderSystem.polygonMode = VK_POLYGON_MODE_LINE;
            }
            // TODO: Use Triangle Fan topology if supported by the device
            case GL11.GL_TRIANGLE_FAN, GL11.GL_TRIANGLES -> {
                VRenderSystem.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
                VRenderSystem.polygonMode = VK_POLYGON_MODE_FILL;
            }
            case GL11.GL_TRIANGLE_STRIP -> {
                VRenderSystem.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
                VRenderSystem.polygonMode = VK_POLYGON_MODE_FILL;
            }
            default -> throw new RuntimeException(String.format("Unknown VertexFormat mode: %s", parameters.mode()));
        }
        renderer.bindGraphicsPipeline(pipeline);
        renderer.uploadAndBindUBOs(pipeline);
        Renderer.getDrawer().draw(buffer.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
    }

}
