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
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
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

    public void upload(BufferBuilder.RenderedBuffer buffer) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
//        this.indexType = parameters.indexType();
        this.mode = parameters.mode();

        this.configureVertexFormat(parameters, buffer.vertexBuffer());
        this.configureIndexBuffer(parameters, buffer.indexBuffer());

        buffer.release();

    }

    private void configureVertexFormat(BufferBuilder.DrawState parameters, ByteBuffer data) {
        if (!parameters.indexOnly()) {

            if(vertexBuffer != null)
                this.vertexBuffer.freeBuffer();

            this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);

        }
    }

    private void configureIndexBuffer(BufferBuilder.DrawState parameters, ByteBuffer data) {
        if (parameters.sequentialIndex()) {

            AutoIndexBuffer autoIndexBuffer;
            switch (this.mode) {
                case TRIANGLE_FAN -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleFanIndexBuffer();
                    this.indexCount = (vertexCount - 2) * 3;
                }
                case QUADS -> {
                    autoIndexBuffer = Renderer.getDrawer().getQuadsIndexBuffer();
                }
                case TRIANGLES -> {
                    autoIndexBuffer = null;
                }
                default -> throw new IllegalStateException("Unexpected draw mode:" + this.mode);
            }

            if(indexBuffer != null && !this.autoIndexed)
                indexBuffer.freeBuffer();

            if(autoIndexBuffer != null) {
                autoIndexBuffer.checkCapacity(vertexCount);
                indexBuffer = autoIndexBuffer.getIndexBuffer();
            }

            this.autoIndexed = true;

        }
        else {
            if(indexBuffer != null)
                this.indexBuffer.freeBuffer();
            this.indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
//            this.indexBuffer = new AsyncIndexBuffer(data.remaining());
            indexBuffer.copyBuffer(data);
        }

    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            drawWithShader(MV, P, ((ShaderMixed)shader).getPipeline());

        }
    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, GraphicsPipeline pipeline) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            VRenderSystem.applyMVP(MV, P);

            Renderer renderer = Renderer.getInstance();
            boolean shouldUpdate = (renderer.bindGraphicsPipeline(pipeline));
            {
                renderer.uploadAndBindUBOs(pipeline, shouldUpdate);
            }

            if(indexBuffer != null)
                Renderer.getDrawer().drawIndexed(vertexBuffer, indexBuffer, indexCount);
            else
                Renderer.getDrawer().draw(vertexBuffer, vertexCount);

            VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

        }
    }

    public void drawChunkLayer() {
        if (this.indexCount != 0) {

            RenderSystem.assertOnRenderThread();
            Renderer.getDrawer().drawIndexed(vertexBuffer, indexBuffer, indexCount);
        }
    }

    public void close() {
        if(vertexCount <= 0) return;
        vertexBuffer.freeBuffer();
        vertexBuffer = null;
        if(!autoIndexed) {
            indexBuffer.freeBuffer();
            indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
    }

//    public VertexFormat getFormat() {
//        return this.vertexFormat;
//    }

}
