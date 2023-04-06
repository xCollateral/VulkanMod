package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
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
    public int indexCount;
    private int vertexCount;

    public final RenderTypes type;
    private int x;
    private int y;
    private int z;

    public VBO(String name, int x, int y, int z) {
        this.type = getLayer(name);
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
//        if (type==RenderTypes.TRANSLUCENT) this.configureIndexBuffer(parameters, idxBuff);

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
