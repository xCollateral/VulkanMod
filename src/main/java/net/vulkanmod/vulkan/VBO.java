package net.vulkanmod.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Matrix4f;
import net.vulkanmod.vulkan.memory.*;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public class VBO {
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private VertexFormat.IntType indexType;
    private int indexCount;
    private int vertexCount;
    private VertexFormat.DrawMode mode;
    private boolean sequentialIndices;
    private VertexFormat format;
    private int offset;

    private boolean autoIndexed = false;

    public VBO() {}

    //TODO
    public void upload_(BufferBuilder p_85936_) {
        Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> pair = p_85936_.popData();

        //BufferUploader.reset();
        BufferBuilder.DrawArrayParameters parameters = pair.getFirst();
        ByteBuffer bytebuffer = pair.getSecond();
        int i = parameters.getIndexBufferStart();
        this.indexCount = parameters.getVertexCount();
        this.vertexCount = parameters.getCount();
        this.indexType = parameters.getElementFormat();
        this.format = parameters.getVertexFormat();
        this.mode = parameters.getMode();
        this.sequentialIndices = parameters.hasNoIndexBuffer();

        if (!parameters.hasNoVertexBuffer() && vertexCount > 0) {
            bytebuffer.limit(i);

            if(vertexBuffer != null) MemoryManager.addToFreeable(vertexBuffer);
            vertexBuffer = new VertexBuffer(i, MemoryTypes.GPU_MEM);
            vertexBuffer.copyToVertexBuffer(format.getVertexSize(), this.vertexCount, bytebuffer);

            bytebuffer.position(i);
        }

        if (!this.sequentialIndices) {

            if(parameters.hasNoVertexBuffer()) {

                bytebuffer.limit(indexCount * indexType.size);

                if(indexBuffer != null) MemoryManager.addToFreeable(indexBuffer);
                indexBuffer = new IndexBuffer(bytebuffer.remaining(), MemoryTypes.GPU_MEM); // Short size
                indexBuffer.copyBuffer(bytebuffer);

                return;

            }

            bytebuffer.limit(parameters.getIndexBufferEnd());

            if(indexBuffer != null) MemoryManager.addToFreeable(indexBuffer);
            indexBuffer = new IndexBuffer(bytebuffer.remaining(), MemoryTypes.GPU_MEM); // Short size
            indexBuffer.copyBuffer(bytebuffer);

            bytebuffer.position(0);
        } else {

            if (vertexCount <= 0) {
                return;
            }

            AutoIndexBuffer autoIndexBuffer;
            if(this.mode != VertexFormat.DrawMode.TRIANGLE_FAN) {
                autoIndexBuffer = Drawer.getInstance().getQuadsIndexBuffer();
            } else {
                autoIndexBuffer = Drawer.getInstance().getTriangleFanIndexBuffer();
                this.indexCount = (vertexCount - 2) * 3;
            }
            autoIndexBuffer.checkCapacity(vertexCount);
            indexBuffer = autoIndexBuffer.getIndexBuffer();
            this.autoIndexed = true;

            bytebuffer.limit(i);
            bytebuffer.position(0);
        }

    }

//    public void upload_(BufferBuilder bufferBuilder) {
//        Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> pair = bufferBuilder.popData();
//
//        //BufferUploader.reset();
//        BufferBuilder.DrawArrayParameters parameters = pair.getFirst();
//        ByteBuffer bytebuffer = pair.getSecond();
//
//        this.offset = parameters.getIndexBufferStart();
//        this.indexCount = parameters.getVertexCount();
//        this.vertexCount = parameters.getCount();
//        this.indexType = parameters.getElementFormat();
//        this.mode = parameters.getMode();
//        this.sequentialIndices = parameters.hasNoIndexBuffer();
//
//        this.configureVertexBuffer(parameters, bytebuffer);
//        this.configureIndexBuffer(parameters, bytebuffer);
//
//        bytebuffer.position(0);
//
//    }

    private VertexFormat configureVertexBuffer(BufferBuilder.DrawArrayParameters parameters, ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (!parameters.hasNoVertexBuffer() && vertexCount > 0) {
            data.limit(offset);

            if(vertexBuffer == null) vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            vertexBuffer.uploadWholeBuffer(data);

            data.position(offset);
        }
        return parameters.getVertexFormat();
    }

    private void configureIndexBuffer(BufferBuilder.DrawArrayParameters parameters, ByteBuffer data) {
        if (parameters.hasNoIndexBuffer()) {
            AutoIndexBuffer autoIndexBuffer;
            if(this.mode != VertexFormat.DrawMode.TRIANGLE_FAN) {
                autoIndexBuffer = Drawer.getInstance().getQuadsIndexBuffer();
            } else {
                autoIndexBuffer = Drawer.getInstance().getTriangleFanIndexBuffer();
                this.indexCount = (vertexCount - 2) * 3;
            }

            if(indexBuffer != null && !this.autoIndexed) indexBuffer.freeBuffer();

            autoIndexBuffer.checkCapacity(vertexCount);
            indexBuffer = autoIndexBuffer.getIndexBuffer();
            this.autoIndexed = true;

            return;
        }

        data.limit(parameters.getIndexBufferEnd());

        if(indexBuffer == null) indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
        indexBuffer.uploadWholeBuffer(data);
    }

    public void _drawWithShader(Matrix4f MV, Matrix4f P, Shader shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            VRenderSystem.applyMVP(MV, P);

            Drawer drawer = Drawer.getInstance();
            drawer.draw(vertexBuffer, indexBuffer, indexCount, mode.mode);

            VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

        }
    }

    public void drawChunkLayer() {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();
            Drawer drawer = Drawer.getInstance();
            drawer.drawIndexed(vertexBuffer, indexBuffer, indexCount);
        }
    }

    public void close() {
        if(vertexCount <= 0) return;
        vertexBuffer.freeBuffer();
        if(!autoIndexed) indexBuffer.freeBuffer();
    }

    public VertexFormat getFormat() {
        return this.format;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
