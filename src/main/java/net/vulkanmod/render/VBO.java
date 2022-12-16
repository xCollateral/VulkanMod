package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public class VBO {
    public AABB bb;
    private final int index;
    public BlockPos.MutableBlockPos origin;
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private VertexFormat.IndexType indexType;
    private int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
    private boolean sequentialIndices;
    private VertexFormat vertexFormat;

    private boolean autoIndexed = false;

    public boolean preInitialised = true;

    public VBO(AABB bb, int index, BlockPos.MutableBlockPos origin) {
        this.bb = bb;
        this.index = index;
        this.origin = origin;
    }

    public void upload_(BufferBuilder.RenderedBuffer buffer) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
        this.indexType = parameters.indexType();
        this.mode = parameters.mode();
        preInitialised=false;
        this.configureVertexFormat(parameters, buffer.vertexBuffer());
        this.configureIndexBuffer(parameters, buffer.indexBuffer());

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

    public void drawChunkLayer() {
        if (this.indexCount != 0) {

            RenderSystem.assertOnRenderThread();
            Drawer.drawIndexed(vertexBuffer, indexBuffer, indexCount);
        }
    }

    public void close() {
        if(preInitialised) return;
        if(vertexCount <= 0) return;
        vertexBuffer.freeBuffer();
        vertexBuffer = null;
        if(!autoIndexed) {
            indexBuffer.freeBuffer();
            indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
        RHandler.uniqueVBOs.remove(this);
        preInitialised=true;
    }

    public VertexFormat getFormat() {
        return this.vertexFormat;
    }

}
