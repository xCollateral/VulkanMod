package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.*;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;

@Environment(EnvType.CLIENT)
public class VBO {
    public static final int size_t = 32768;
    public AABB bb;
    public BlockPos.MutableBlockPos origin;

    public VkBufferPointer addSubIncr;
//    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
//    private VertexFormat vertexFormat;

    private boolean autoIndexed = false;

    public boolean preInitialised = true;

    public VBO(AABB bb, int index, BlockPos.MutableBlockPos origin) {
        this.bb = bb;
        this.origin = origin;
    }

    public void upload_(BufferBuilder.RenderedBuffer buffer) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
        this.mode = parameters.mode();
        preInitialised=false;
        this.configureVertexFormat(parameters, buffer.vertexBuffer());
        this.configureIndexBuffer(parameters, buffer.indexBuffer());

        if(!buffer.released) buffer.release();

    }

    private void configureVertexFormat(BufferBuilder.DrawState parameters, ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (!parameters.indexOnly()) {

            if(addSubIncr==null || addSubIncr.sizes<data.remaining())
            {
                if(addSubIncr != null) VirtualBuffer.addFreeableRange(addSubIncr);
                addSubIncr=VirtualBuffer.addSubIncr(data.remaining());
            }


            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
            stagingBuffer.copyBuffer(data.remaining(), data);

            copyStagingtoLocalBuffer(stagingBuffer.getId(), stagingBuffer.offset, VirtualBuffer.bufferPointerSuperSet, addSubIncr.i2, addSubIncr.sizes);
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
            Drawer.drawIndexed(addSubIncr, indexBuffer, indexCount);
        }
    }

    public void close() {
        if(preInitialised) return;
        if(vertexCount <= 0) return;
        VirtualBuffer.addFreeableRange(addSubIncr);
        addSubIncr=null;
//        vertexBuffer = null;
        if(!autoIndexed) {
            indexBuffer.freeBuffer();
            indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
        RHandler.uniqueVBOs.remove(this);
        preInitialised=true;
    }

    /*public VertexFormat getFormat() {
        return this.vertexFormat;
    }*/

}
