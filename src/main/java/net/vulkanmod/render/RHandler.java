package net.vulkanmod.render;


import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.Vec3i;

import net.vulkanmod.render.chunk.WorldRenderer;

import java.util.concurrent.CompletableFuture;

public class RHandler
{

//    private static final LevelRenderer worldRenderer = Minecraft.getInstance().levelRenderer;
    //    public static ObjectArrayList<VBO> drawCommands=new ObjectArrayList<>(1024);
    public static ObjectArrayList<VBO> uniqueVBOs=new ObjectArrayList<>(1024);
    public static int loadedVBOs;
    public static int totalVBOs;
    public static double camX;
    public static double camY;
    public static double camZ;
//    public static ChunkGrid viewArea;
   /* public static final VkDrawIndexedIndirectCommand defStruct=VkDrawIndexedIndirectCommand.malloc(MemoryStack.stackGet())
            .indexCount(0)
            .vertexOffset(0)
            .firstIndex(0)
            .firstInstance(0)
            .instanceCount(1);;;*/

    private static Vec3i origin;
    public static int calls;
    public static ChunkRenderDispatcher chunkBuilder = Minecraft.getInstance().levelRenderer.getChunkRenderDispatcher();
    private static long indirectAllocation;


    public static Vec3i obtainWorldOrigin(double e, double f) {
        origin = origin == null ? new Vec3i(e, 0, f) : origin;
        return origin;
    }

    public static void setWorldOrigin(int d, int f) {
       origin = new Vec3i(d,0,  f);

    }

//    public static void addWorldOrigin(int i, int j) {
//        if(origin!=null) origin.add(-i, 0,  -j);
//    }

    public static void setCnkBldr(ChunkRenderDispatcher a) {
        chunkBuilder=a;
    }

    public static CompletableFuture<Void> uploadVBO(VBO vbo, BufferBuilder.RenderedBuffer buffers, boolean sort)
    {
//        if(buffers==null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() ->
                vbo.upload_(buffers, sort), WorldRenderer.taskDispatcher.toUpload::add);
    }

    public static CompletableFuture<Void> uploadVBO(int index, BufferBuilder.RenderedBuffer buffers)
    {
        if(buffers==null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() ->
                getVBOFromIndex(index).upload_(buffers, false), WorldRenderer.taskDispatcher.toUpload::add);
    }

    public static VBO getVBOFromIndex(int index) {
        /*for(VBO vbo : uniqueVBOs)
        {
            if(vbo.index==index) return vbo;
        }
        return uniqueVBOs.get(0);

        */
        return WorldRenderer.chunkGrid.chunks[index].vbo;
    }

    public static void freeVBO(int index) {
        getVBOFromIndex(index).close();
    }
}
