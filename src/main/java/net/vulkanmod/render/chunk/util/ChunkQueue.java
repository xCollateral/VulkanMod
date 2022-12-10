package net.vulkanmod.render.chunk.util;

import net.vulkanmod.render.chunk.WorldRenderer;

public class ChunkQueue {
    WorldRenderer.QueueChunkInfo[] queue;
    int position = 0;
    int limit = 0;
    int capacity;

    public ChunkQueue() {
        this.capacity = 1024;

        this.queue = new WorldRenderer.QueueChunkInfo[this.capacity];
    }

    public boolean hasNext() {
        return this.position < this.limit;
    }

    public WorldRenderer.QueueChunkInfo poll() {
        WorldRenderer.QueueChunkInfo chunk = this.queue[position];
        this.position++;

        return chunk;
    }

    public void add(WorldRenderer.QueueChunkInfo chunk) {
        if(chunk == null)
            return;

        if(limit == capacity) resize();
        this.queue[limit] = chunk;

        this.limit++;
    }

    private void resize() {
        this.capacity *= 2;

        WorldRenderer.QueueChunkInfo[] oldQueue = this.queue;
        this.queue = new WorldRenderer.QueueChunkInfo[this.capacity];

        System.arraycopy(oldQueue, 0, this.queue, 0, oldQueue.length);
    }

    public void clear() {
        this.position = 0;
        this.limit = 0;
    }

    //debug
    public void resetPosition() {
        this.position = 0;
    }
}
