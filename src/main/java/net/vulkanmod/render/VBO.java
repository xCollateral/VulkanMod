package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public class VBO {
    public boolean preInitalised=true;
    public boolean hasAbort=false;
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private VertexFormat.IndexType indexType;
    public int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
    private boolean sequentialIndices;
    private VertexFormat vertexFormat;

    private boolean autoIndexed = false;
    private int x;
    private int y;
    private int z;

    public VBO(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void upload(BufferBuilder.RenderedBuffer buffer, boolean sort) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
        this.indexType = parameters.indexType();
        this.mode = parameters.mode();


        if(!sort) translateVBO(buffer);


        this.configureVertexFormat(parameters, buffer.vertexBuffer());
        this.configureIndexBuffer(parameters, buffer.indexBuffer());

        buffer.release();
        preInitalised=false;

    }

    //WorkAround For PushConstants
    //Likely could be moved to an earlier vertex Assembly/Builder state
    private void translateVBO(BufferBuilder.RenderedBuffer buffer) {
        final long addr = MemoryUtil.memAddress0(buffer.vertexBuffer());
        //Use constant Attribute size to encourage loop unrolling
        for(int i = 0; i< buffer.vertexBuffer().remaining(); i+=32)
        {
            VUtil.UNSAFE.putFloat(addr+i,   (VUtil.UNSAFE.getFloat(addr+i)  +(float)(x- WorldRenderer.camX - WorldRenderer.originX)));
            VUtil.UNSAFE.putFloat(addr+i+4, (VUtil.UNSAFE.getFloat(addr+i+4)+y));
            VUtil.UNSAFE.putFloat(addr+i+8, (VUtil.UNSAFE.getFloat(addr+i+8)+(float)(z- WorldRenderer.camZ - WorldRenderer.originZ)));
        }
    }

    private VertexFormat configureVertexFormat(BufferBuilder.DrawState parameters, ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (!parameters.indexOnly()) {

            if(vertexBuffer != null) this.vertexBuffer.freeBuffer();
            this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);
        }
        return parameters.format();
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
            indexBuffer.copyBuffer(data);
        }

    }

    public void _drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            VRenderSystem.applyMVPAffine(MV, P);

            Drawer drawer = Drawer.getInstance();
            drawer.draw(vertexBuffer, indexBuffer, indexCount, mode.asGLMode);

            VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

        }
    }

    public void drawChunkLayer() {
        if (this.indexCount != 0) {
            Drawer.getInstance().drawIndexed(vertexBuffer, indexBuffer, indexCount);
        }
    }

    public void close() {
        if(preInitalised) return;
        if(vertexCount <= 0) return;
        vertexBuffer.freeBuffer();
        vertexBuffer = null;
        if(!autoIndexed) {
            indexBuffer.freeBuffer();
            indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
        preInitalised=true;
    }

    public VertexFormat getFormat() {
        return this.vertexFormat;
    }

    public void updateOrigin(int x, int y, int z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
