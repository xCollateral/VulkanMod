package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public class SkyBoxVBO {
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private VertexFormat.IndexType indexType;
    private int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
    private boolean sequentialIndices;
    private VertexFormat vertexFormat;

    private boolean autoIndexed = false;


    public void upload_(BufferBuilder.RenderedBuffer buffer) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
        this.indexType = parameters.indexType();
        this.mode = parameters.mode();

        this.configureVertexFormat(parameters, buffer.vertexBuffer());
        this.configureIndexBuffer(parameters);

        buffer.release();

    }

    private void configureVertexFormat(BufferBuilder.DrawState parameters, ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (!parameters.indexOnly()) {

            if(vertexBuffer != null) this.vertexBuffer.freeBuffer();
            this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);
        }
    }

    private void configureIndexBuffer(BufferBuilder.DrawState parameters) {
        if (parameters.sequentialIndex()) {

            AutoIndexBuffer autoIndexBuffer;
            Drawer instance = Drawer.getInstance();
            if(this.mode != VertexFormat.Mode.TRIANGLE_FAN) {
                autoIndexBuffer = instance.getQuadsIndexBuffer();
            } else {
                autoIndexBuffer = instance.getTriangleFanIndexBuffer();
                this.indexCount = (vertexCount - 2) * 3;
            }

            if(indexBuffer != null && !this.autoIndexed) indexBuffer.freeBuffer();

            autoIndexBuffer.checkCapacity(vertexCount);
            indexBuffer = autoIndexBuffer.getIndexBuffer();
            this.autoIndexed = true;

        }

    }

    public void _drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            VRenderSystem.applyMVP(MV, P);

            Drawer.getInstance().draw(vertexBuffer, indexBuffer, indexCount, mode.asGLMode);

            VRenderSystem.copyMVP();

        }
    }

    public VertexFormat getFormat() {
        return this.vertexFormat;
    }

}
