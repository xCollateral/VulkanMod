package net.vulkanmod.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Matrix4f;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.VertexBuffer;

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

    private boolean autoIndexed = false;

    public VBO() {}

    public void upload_(BufferBuilder p_85936_) {
        Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> pair = p_85936_.popData();

        //BufferUploader.reset();
        BufferBuilder.DrawArrayParameters bufferbuilder$drawstate = pair.getFirst();
        ByteBuffer bytebuffer = pair.getSecond();
        int i = bufferbuilder$drawstate.getIndexBufferStart();
        this.indexCount = bufferbuilder$drawstate.getVertexCount();
        this.vertexCount = bufferbuilder$drawstate.getCount();
        this.indexType = bufferbuilder$drawstate.getElementFormat();
        this.format = bufferbuilder$drawstate.getVertexFormat();
        this.mode = bufferbuilder$drawstate.getMode();
        this.sequentialIndices = bufferbuilder$drawstate.hasNoIndexBuffer();

        if (!bufferbuilder$drawstate.hasNoVertexBuffer() && vertexCount > 0) {
            bytebuffer.limit(i);

            if(vertexBuffer != null) MemoryManager.addToFreeable(vertexBuffer);
            vertexBuffer = new VertexBuffer(i, VertexBuffer.Type.DEVICE_LOCAL);
            vertexBuffer.copyToVertexBuffer(format.getVertexSize(), this.vertexCount, bytebuffer);

            bytebuffer.position(i);
        }

        if (!this.sequentialIndices) {

            if(bufferbuilder$drawstate.hasNoVertexBuffer()) {

                bytebuffer.limit(indexCount * indexType.size);

                if(indexBuffer != null) MemoryManager.addToFreeable(indexBuffer);
                indexBuffer = new IndexBuffer(bytebuffer.remaining(), IndexBuffer.Type.DEVICE_LOCAL); // Short size
                indexBuffer.copyBuffer(bytebuffer);

                return;

            }

            bytebuffer.limit(bufferbuilder$drawstate.getIndexBufferEnd());

            if(indexBuffer != null) MemoryManager.addToFreeable(indexBuffer);
            indexBuffer = new IndexBuffer(bytebuffer.remaining(), IndexBuffer.Type.DEVICE_LOCAL); // Short size
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
}
