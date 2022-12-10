package net.vulkanmod.render.chunk;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderRegionCache;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkRenderer {

    private final static int threadCount = 4;
    private Thread[] threads;
    private Queue<Object> tasks = new ConcurrentLinkedQueue<>();



    static class BuilderThread extends Thread {

        ChunkBufferBuilderPack buffers = new ChunkBufferBuilderPack();
        RenderRegionCache cache = new RenderRegionCache();
    }
}
