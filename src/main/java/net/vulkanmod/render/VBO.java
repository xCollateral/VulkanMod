package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.shader.Pipeline;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

@Environment(EnvType.CLIENT)
public class VBO {
    private VertexBuffer vertexBuffer;
//    private AsyncVertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
//    private VertexFormat.IndexType indexType;
    private int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
//    private boolean sequentialIndices;
//    private VertexFormat vertexFormat;
//    private int vertexOffset;

    private boolean autoIndexed = false;
//    private boolean uploaded = false;

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
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (!parameters.indexOnly()) {

            if(vertexBuffer != null) this.vertexBuffer.freeBuffer();
            this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
//            this.vertexBuffer = new AsyncVertexBuffer(data.remaining());
            vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);

//            this.uploaded = false;
        }
    }

    private void configureIndexBuffer(BufferBuilder.DrawState parameters, ByteBuffer data) {
        if (parameters.sequentialIndex()) {

            AutoIndexBuffer autoIndexBuffer;
            if(this.mode != VertexFormat.Mode.TRIANGLE_FAN) {
                autoIndexBuffer = Drawer.getInstance().getQuadsIndexBuffer();
            } else {
                autoIndexBuffer = Drawer.getInstance().getTriangleFanIndexBuffer();
                this.indexCount = (vertexCount - 2) * 3;
            }

            if(indexBuffer != null && !this.autoIndexed) indexBuffer.freeBuffer();

            autoIndexBuffer.checkCapacity(vertexCount);
            indexBuffer = autoIndexBuffer.getIndexBuffer();
            this.autoIndexed = true;

        }
        else {
            if(indexBuffer != null) this.indexBuffer.freeBuffer();
            this.indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
//            this.indexBuffer = new AsyncIndexBuffer(data.remaining());
            indexBuffer.copyBuffer(data);
        }

    }

    public void _drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            VRenderSystem.applyMVP(MV, P);

            Drawer drawer = Drawer.getInstance();
            Pipeline pipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
            drawer.bindPipeline(pipeline);
            drawer.uploadAndBindUBOs(pipeline);
            drawer.drawIndexed(vertexBuffer, indexBuffer, indexCount);

            VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

        }
    }

    public void drawChunkLayer() {
//        if (this.indexCount != 0) {
//        if(!this.uploaded) {
//            this.checkUploadStatus();
//            if(!this.uploaded) return;
//        }
        if (this.indexCount != 0) {

            RenderSystem.assertOnRenderThread();
            Drawer drawer = Drawer.getInstance();
            drawer.drawIndexed(vertexBuffer, indexBuffer, indexCount);
        }
    }

//    public void checkUploadStatus() {
//        if(!(this.indexBuffer instanceof AsyncIndexBuffer asyncIndexBuffer))
//            this.uploaded = this.vertexBuffer.checkStatus();
//        else {
//            this.uploaded = this.vertexBuffer.checkStatus() && asyncIndexBuffer.checkStatus();
//        }
//    }

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
