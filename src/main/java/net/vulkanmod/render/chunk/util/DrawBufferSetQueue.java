package net.vulkanmod.render.chunk.util;

import net.vulkanmod.render.chunk.DrawBuffers;

import java.util.Arrays;
import java.util.Iterator;

public class DrawBufferSetQueue {
    private final int size;
    int[] set;
    ResettableQueue<DrawBuffers> queue;

    public DrawBufferSetQueue(int size) {
        this.size = size;

        int t = (int) Math.ceil((float)size / Integer.SIZE);
        this.set = new int[t];
        this.queue = new ResettableQueue<>(size);
    }

    public void add(DrawBuffers chunkArea) {
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

    public Iterator<DrawBuffers> iterator(boolean reverseOrder) {
        return queue.iterator(reverseOrder);
    }

    public Iterator<DrawBuffers> iterator() {
        return this.iterator(false);
    }

}
