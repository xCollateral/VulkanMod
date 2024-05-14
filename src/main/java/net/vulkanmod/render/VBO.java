package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer.DrawType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public class VBO {
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;

    private int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;

    private boolean autoIndexed = false;

    public VBO() {}

    public void upload(final BufferBuilder.RenderedBuffer buffer) {
        final BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
//        this.indexType = parameters.indexType();
        this.mode = parameters.mode();

        this.configureVertexFormat(parameters, buffer.vertexBuffer());
        this.configureIndexBuffer(parameters, buffer.indexBuffer());

        buffer.release();
    }

    private void configureVertexFormat(final BufferBuilder.DrawState parameters, final ByteBuffer data) {
        if (parameters.indexOnly()) {
            return;
        }

        if (vertexBuffer != null) {
            this.vertexBuffer.freeBuffer();
        }

        this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
        this.vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);
    }

    private void configureIndexBuffer(final BufferBuilder.DrawState parameters, final ByteBuffer data) {
        if (parameters.sequentialIndex()) {
            final AutoIndexBuffer autoIndexBuffer = switch (this.mode) {
                case QUADS -> Renderer.getDrawer().getQuadsIndexBuffer();
                case LINES -> Renderer.getDrawer().getLinesIndexBuffer();
                case TRIANGLES -> null;
                case TRIANGLE_FAN -> Renderer.getDrawer().getTriangleFanIndexBuffer();
                case LINE_STRIP, TRIANGLE_STRIP  -> Renderer.getDrawer().getTriangleStripIndexBuffer();
                case DEBUG_LINES -> Renderer.getDrawer().getSequentialIndexBuffer();
                case DEBUG_LINE_STRIP -> Renderer.getDrawer().getDebugLineStripIndexBuffer();
                default -> throw new IllegalStateException(String.format("Unexpected mode: %s", this.mode));
            };

            if (!this.autoIndexed && this.indexBuffer != null) {
                this.indexBuffer.freeBuffer();
                this.indexBuffer = null;
                this.indexCount = 0;
            }

            if (autoIndexBuffer != null) {
                this.indexBuffer = autoIndexBuffer.getIndexBuffer();
                this.indexCount = DrawType.fromVertexFormat(this.mode).indexCount(this.vertexCount);
            }
            this.autoIndexed = true;
        } else {
            if (this.indexBuffer != null) {
                this.indexBuffer.freeBuffer();
            }
            this.indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            this.indexBuffer.copyBuffer(data);
        }
    }

    public void drawWithShader(final Matrix4f MV, final Matrix4f P, final ShaderInstance shader) {
        if (this.indexCount == 0) {
            return;
        }

        RenderSystem.assertOnRenderThread();
        RenderSystem.setShader(() -> shader);

        this.drawWithShader(MV, P, ((ShaderMixed)shader).getPipeline());
    }

    public void drawWithShader(final Matrix4f MV, final Matrix4f P, final GraphicsPipeline pipeline) {
        if (this.indexCount == 0) {
            return;
        }

        RenderSystem.assertOnRenderThread();

        VRenderSystem.applyMVP(MV, P);
        VRenderSystem.setPrimitiveTopologyGL(this.mode.asGLMode);

        final Renderer renderer = Renderer.getInstance();
        renderer.bindGraphicsPipeline(pipeline);
        renderer.uploadAndBindUBOs(pipeline);

        if (this.indexBuffer == null) {
            Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
        } else {
            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
        }

        VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
    }

    public void drawChunkLayer() {
        if (this.indexCount == 0) {
            return;
        }

        RenderSystem.assertOnRenderThread();
        VRenderSystem.setPrimitiveTopologyGL(this.mode.asGLMode);
        if (this.indexBuffer == null) {
            Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
        } else {
            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
        }
    }

    public void close() {
        if (this.vertexCount <= 0) {
            return;
        }

        this.vertexBuffer.freeBuffer();
        this.vertexBuffer = null;

        if (!this.autoIndexed && this.indexBuffer != null) {
            this.indexBuffer.freeBuffer();
            this.indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
    }

//    public VertexFormat getFormat() {
//        return this.vertexFormat;
//    }
}
