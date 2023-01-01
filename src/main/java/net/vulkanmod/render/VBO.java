package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.*;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;

@Environment(EnvType.CLIENT)
public class VBO implements Comparable<VBO> {
    public static final int size_t = 32768;
    private final int index;
    public int x;
    public int y;
    public int z;
//    public AABB bb;

    private VkBufferPointer addSubIncr;
    public boolean translucent=false;
    //    private VertexBuffer vertexBuffer;
    private VkBufferPointer indexBuffer;
    public int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
//    private VertexFormat vertexFormat;

    private final boolean autoIndexed = false;

    public boolean preInitialised = true;
    public VkDrawIndexedIndirectCommand indirectCommand;

    public VBO(int index, int x, int y, int z) {
        this.index=index;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void upload_(BufferBuilder.RenderedBuffer buffer, boolean sort) {

        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
       //TODO: Could Move to BufferBuilderM but need to access VBO/CHunk Origin to Translate/Offset the Vertices Correctly
        if(!sort) translateVBO(buffer);


        this.mode = parameters.mode();
        preInitialised=false;
//        this.configureVertexFormat(buffer.vertexBuffer());
//        this.configureIndexBuffer(buffer.indexBuffer());

        //Don't upload the VertexBuffer again if its just a sort (as its already been uploaded prior during initial upload)
        indirectCommand = indirectCommand==null ? VkDrawIndexedIndirectCommand.create(MemoryUtil.nmemAlignedAlloc(8, 20)) : indirectCommand;//ALIGN and SIZEOF are NULL due to a bug in LWJGL
        indirectCommand
                .indexCount(parameters.indexCount())
                .vertexOffset(!parameters.indexOnly() | !sort? configureVertexFormat(buffer.vertexBuffer()) : addSubIncr.i2>>5)
                .firstIndex(configureIndexBuffer(parameters.sequentialIndex(), buffer.indexBuffer()))
                .firstInstance(0)
                .instanceCount(this.indexCount != 0 ? 1 : 0); //Cull if Empty

        if(!buffer.released) buffer.release();

    }
    //WorkAround For PushConstants
    private void translateVBO(BufferBuilder.RenderedBuffer buffer) {
        final long addr = MemoryUtil.memAddress0(buffer.vertexBuffer());
        for(int i = 0; i< buffer.vertexBuffer().remaining(); i+=32)
        {
            MemoryUtil.memPutFloat(addr+i,   (MemoryUtil.memGetFloat(addr+i)  +(float)(x-RHandler.camX-WorldRenderer.originX)));
            MemoryUtil.memPutFloat(addr+i+4, (MemoryUtil.memGetFloat(addr+i+4)+y));
            MemoryUtil.memPutFloat(addr+i+8, (MemoryUtil.memGetFloat(addr+i+8)+(float)(z-RHandler.camZ-WorldRenderer.originZ)));
        }
    }

    private int configureVertexFormat(ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        {

            if(addSubIncr == null || addSubIncr.size_t < data.remaining() || !VirtualBuffer.isAlreadyLoaded(index))
            {
                addSubIncr=VirtualBuffer.addSubIncr(index, data.remaining());
            }


            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
            stagingBuffer.copyBuffer(data.remaining(), data);

            copyStagingtoLocalBuffer(stagingBuffer.getId(), stagingBuffer.offset, VirtualBuffer.bufferPointerSuperSet, addSubIncr.i2, addSubIncr.size_t);
            return addSubIncr.i2>>5;
        }
    }

    private int configureIndexBuffer(boolean seqIdx, ByteBuffer data) {
        {
            if(seqIdx)
            {
                AutoIndexBuffer autoIndexBuffer;
                if(this.mode != VertexFormat.Mode.TRIANGLE_FAN) {
                    autoIndexBuffer = Drawer.getQuadsIndexBuffer();
                } else {
                    autoIndexBuffer = Drawer.getTriangleFanIndexBuffer();
                    this.indexCount = (vertexCount - 2) * 3;
                }
                data=autoIndexBuffer.getBuffer();
            }
            if(indexBuffer==null || indexBuffer.size_t <data.remaining() || !VirtualBufferIdx.isAlreadyLoaded(index))
            {
                indexBuffer=VirtualBufferIdx.addSubIncr(index, data.remaining());
            }
            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
            stagingBuffer.copyBuffer(indexBuffer.size_t, data);

            copyStagingtoLocalBuffer(stagingBuffer.getId(), stagingBuffer.offset, VirtualBufferIdx.bufferPointerSuperSet, indexBuffer.i2, indexBuffer.size_t);

            return indexBuffer.i2>>1;
        }

    }

    public void drawChunkLayer() {
        if (this.indexCount != 0) {

            Drawer.drawIndexedBindless(indirectCommand);
        }
    }

    public void close() {
        if(preInitialised) return;
        if(vertexCount <= 0) return;
        VirtualBuffer.addFreeableRange(index, addSubIncr);
        addSubIncr=null;
//        vertexBuffer = null;
        {
            VirtualBufferIdx.addFreeableRange(index, indexBuffer);
            indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
        RHandler.uniqueVBOs.remove(this);
        indirectCommand.free();
        indirectCommand=null;
        preInitialised=true;
    }

    public void updateOrigin(int x, int y, int z) {
//        this.origin=origin;
        this.x=x;
        this.y=y;
        this.z=z;
//        this.bb=bb;
    }
    int diff(int num, int num2)  {
        int a = num-num2;
        int i1 = a >>> 31;

        return (a^ (i1 ==1 ? -1 : 0))-i1;
    }
    @Override
    public int compareTo(@NotNull VBO o) {
        BlockPos v = WorldRenderer.minecraft.getCameraEntity().blockPosition();
        int x=diff(v.getX(),o.x);
        int z=diff(v.getZ(),o.z);
        int x_=diff(v.getX(),this.x);
        int z_=diff(v.getZ(),this.z);
        return (z_+x_)-(z + x);
    }

    /*public VertexFormat getFormat() {
        return this.vertexFormat;
    }*/

}
