package net.vulkanmod.render.chunk.util;

import net.vulkanmod.render.chunk.ChunkArea;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class AreaSetQueue {
    private final int size;
    int[] set;
    ResettableQueue<ChunkArea> queue;

    public AreaSetQueue(int size) {
        this.size = size;

        int t = (int) Math.ceil((float)size / Integer.SIZE);
        this.set = new int[t];
        this.queue = new ResettableQueue<>(size);
    }

    public void add(ChunkArea chunkArea) {
        if(chunkArea.index >= this.size)
            throw new IndexOutOfBoundsException();

        int i = chunkArea.index >> 5;
        if((this.set[i] & (1 << (chunkArea.index & 31))) == 0) {
            queue.add(chunkArea);
            this.set[i] |= (1 << (chunkArea.index & 31));
        }
    }

    public void clear() {
        Arrays.fill(this.set, 0);

        this.queue.clear();
    }

    public Iterator<ChunkArea> iterator(boolean reverseOrder) {
        return queue.iterator(reverseOrder);
    }

    public Iterator<ChunkArea> iterator() {
        return this.iterator(false);
    }

}
