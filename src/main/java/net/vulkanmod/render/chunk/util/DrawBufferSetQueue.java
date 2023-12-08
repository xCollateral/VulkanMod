package net.vulkanmod.render.chunk.util;

import net.vulkanmod.render.chunk.DrawBuffers;

import java.util.Arrays;
import java.util.Iterator;

public record DrawBufferSetQueue(int size, int[] set, StaticQueue<DrawBuffers> queue)
{

    public DrawBufferSetQueue(int size) {
        this(size, new int[(int) Math.ceil((float)size / Integer.SIZE)], new StaticQueue<>(size));
    }

    public void add(DrawBuffers chunkArea) {
        if(chunkArea.areaIndex >= this.size)
            throw new IndexOutOfBoundsException();

        int i = chunkArea.areaIndex >> 5;
        if((this.set[i] & (1 << (chunkArea.areaIndex & 31))) == 0) {
            queue.add(chunkArea);
            this.set[i] |= (1 << (chunkArea.areaIndex & 31));
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
