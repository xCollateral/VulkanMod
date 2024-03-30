package net.vulkanmod.render.chunk.util;

import net.vulkanmod.render.chunk.ChunkArea;

import java.util.Arrays;
import java.util.Iterator;

public record AreaSetQueue(int size, int[] set, StaticQueue<ChunkArea> queue) {

    public AreaSetQueue(int size) {
        this(size, new int[(int) Math.ceil((float) size / Integer.SIZE)], new StaticQueue<>(size));
    }

    public void add(ChunkArea chunkArea) {
        final int i = chunkArea.index >> 5;
        final int mask = 1 << (chunkArea.index & 31);
        if ((this.set[i] & mask) == 0) {
            queue.add(chunkArea);
            this.set[i] |= mask;
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
