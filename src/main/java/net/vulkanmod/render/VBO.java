package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static net.vulkanmod.render.chunk.util.VBOUtil.*;

@Environment(EnvType.CLIENT)
public class VBO {
    final int index;
    public boolean preInitalised=true;
    public boolean hasAbort=false;
    private VertexBuffer vertexBuffer;
    public int indexCount;
    private int vertexCount;

    public final RenderTypes type;
    private int x;
    private int y;
    private int z;

    public VBO(int index, String name, int x, int y, int z) {
        this.index = index;
        this.type = getLayer(name);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void upload(BufferBuilder.RenderedBuffer buffer, boolean sort) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = Math.min(parameters.indexCount(), parameters.vertexCount()/4*6);
        this.vertexCount = parameters.vertexCount();

        final ByteBuffer vertBuff = buffer.vertexBuffer();

        if(!sort) translateVBO(MemoryUtil.memAddress0(vertBuff));


        this.configureVertexFormat(vertBuff);
//        this.configureIndexBuffer(idxBuff);

        buffer.release();
        preInitalised=false;

    }

    //WorkAround For PushConstants
    //Likely could be moved to an earlier vertex Assembly/Builder state
    private void translateVBO(long addr) {
        //Use constant Attribute size to encourage loop unrolling
        for(int i = 0; i< vertexCount*32; i+=32)
        {
            VUtil.UNSAFE.putFloat(addr+i,   (VUtil.UNSAFE.getFloat(addr+i)  +(float)(x- camX - originX)));
            VUtil.UNSAFE.putFloat(addr+i+4, (VUtil.UNSAFE.getFloat(addr+i+4)+y));
            VUtil.UNSAFE.putFloat(addr+i+8, (VUtil.UNSAFE.getFloat(addr+i+8)+(float)(z- camZ - originZ)));
        }
    }

    private void configureVertexFormat(ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);

        if(this.vertexBuffer==null) this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
        if(data.remaining()> vertexBuffer.getTotalSize())
        {
            this.vertexBuffer.freeBuffer();
            this.vertexBuffer=new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
        }

        //

        vertexBuffer.uploadToBuffer(data.remaining(), data);
    }


    public void drawChunkLayer() {
        if (this.indexCount != 0) {
            Drawer.getInstance().drawIndexed2(vertexBuffer, indexCount);
        }
    }

    public void close() {
        if(preInitalised) return;
        if(vertexCount <= 0) return;
        vertexBuffer.freeBuffer();
        vertexBuffer = null;

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
}
