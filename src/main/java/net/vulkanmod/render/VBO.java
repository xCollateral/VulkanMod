package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.render.chunk.util.VBOUtil;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static net.vulkanmod.render.chunk.util.VBOUtil.*;

@Environment(EnvType.CLIENT)
public class VBO {
    public boolean preInitalised=true;
    public boolean hasAbort=false;
    public VertexBuffer vertexBuffer;
    public VkBufferPointer fakeIndexBuffer;


    public int indexCount;
    private int vertexCount;

    private final int index;
    public final RenderTypes type;
    private int x;
    private int y;
    private int z;
    public int indexOff = 0;

    public VBO(int index, RenderTypes name, int x, int y, int z) {
        this.index = index;
        this.type = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void upload(BufferBuilder.RenderedBuffer buffer, boolean sort) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();

        final ByteBuffer vertBuff = buffer.vertexBuffer();
//        final ByteBuffer idxBuff = buffer.indexBuffer();

        if(!sort) translateVBO(vertBuff);


        if (!sort) this.configureVertexFormat(parameters, vertBuff);
        if (type==RenderTypes.TRANSLUCENT) this.configureIndexFormat(buffer.indexBuffer());

        buffer.release();
        preInitalised=false;

    }

    //WorkAround For PushConstants
    //Likely could be moved to an earlier vertex Assembly/Builder state
    private void translateVBO(ByteBuffer buffer) {
        final long addr = MemoryUtil.memAddress0(buffer);
        //Use constant Attribute size to encourage loop unrolling
        for(int i = 0; i< buffer.remaining(); i+=32)
        {
            VUtil.UNSAFE.putFloat(addr+i,   (VUtil.UNSAFE.getFloat(addr+i)  +(float)(x- camX - originX)));
            VUtil.UNSAFE.putFloat(addr+i+4, (VUtil.UNSAFE.getFloat(addr+i+4)+y));
            VUtil.UNSAFE.putFloat(addr+i+8, (VUtil.UNSAFE.getFloat(addr+i+8)+(float)(z- camZ - originZ)));
        }
    }

    private void configureVertexFormat(BufferBuilder.DrawState parameters, ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (!parameters.indexOnly()) {

            if(this.vertexBuffer==null) this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            if(data.remaining()>vertexBuffer.getTotalSize())
            {
                this.vertexBuffer.freeBuffer();
                this.vertexBuffer=new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            }

            //

            vertexBuffer.uploadToBuffer(32 * parameters.vertexCount(), data);

        }
    }
    private void configureIndexFormat(ByteBuffer data) {
        if(data.remaining()==0) return;
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (type==RenderTypes.TRANSLUCENT) {

            if(fakeIndexBuffer ==null || !VBOUtil.virtualBufferIdx.isAlreadyLoaded(index, data.remaining()))
            {
                fakeIndexBuffer =VBOUtil.virtualBufferIdx.addSubIncr(index, data.remaining());
            }
            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
            stagingBuffer.copyBuffer(fakeIndexBuffer.size_t(), data);

            Vulkan.copyStagingtoLocalBuffer(stagingBuffer.getId(), stagingBuffer.getOffset(), VBOUtil.virtualBufferIdx.bufferPointerSuperSet, fakeIndexBuffer.i2(), fakeIndexBuffer.size_t());

            this.indexOff= fakeIndexBuffer.i2()>>1;

        }
    }


    public void close() {
        if(preInitalised) return;
        if(vertexCount <= 0) return;
        vertexBuffer.freeBuffer();
        vertexBuffer = null;
        if(type==RenderTypes.TRANSLUCENT)
        {
            virtualBufferIdx.addFreeableRange(index, fakeIndexBuffer);
            fakeIndexBuffer=null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
        preInitalised=true;
        removeVBO(this);
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

    public void draw() {
        Drawer.drawIndexed2(this.vertexBuffer, this.indexOff, this.indexCount);
    }
}
