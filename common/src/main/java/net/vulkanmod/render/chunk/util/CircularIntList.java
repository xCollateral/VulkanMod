package net.vulkanmod.render.chunk.util;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class CircularIntList {
    private int[] list;
    private final int startIndex;

    private int[] previous;
    private int[] next;

    private OwnIterator iterator;

    public CircularIntList(int size, int startIndex) {
        this.startIndex = startIndex;

        this.generateList(size);
    }

    private void generateList(int size) {
        int[] list = new int[size];

        this.previous = new int[size];
        this.next = new int[size];

        int k = 0;
        for(int i = startIndex; i < size; ++i) {
            list[k] = i;

            ++k;
        }
        for(int i = 0; i < startIndex; ++i) {
            list[k] = i;
            ++k;
        }

        this.previous[0] = -1;
        System.arraycopy(list, 0, this.previous, 1, size - 1);

        this.next[size - 1] = -1;
        System.arraycopy(list, 1, this.next, 0, size - 1);

        this.list = list;
    }

    public int getNext(int i) {
        return this.next[i];
    }

    public int getPrevious(int i) {
        return this.previous[i];
    }

    public OwnIterator iterator() {
        return new OwnIterator();
    }

    public RangeIterator rangeIterator(int startIndex, int endIndex) {
        return new RangeIterator(startIndex, endIndex);
    }

    public void restartIterator() {
        this.iterator.restart();
    }

    public class OwnIterator implements Iterator<Integer> {
        private int currentIndex = -1;
        private final int maxIndex = list.length - 1;

        @Override
        public boolean hasNext() {
            return currentIndex < maxIndex;
        }

        @Override
        public Integer next() {
            currentIndex++;
            return list[currentIndex];
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public void restart() {
            this.currentIndex = -1;
        }
    }

    public class RangeIterator implements Iterator<Integer> {
        private int currentIndex;
        private final int startIndex;
        private final int maxIndex;

        public RangeIterator(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.maxIndex = endIndex;
            Validate.isTrue(this.maxIndex < list.length, "Beyond max size");

            this.restart();
        }

        @Override
        public boolean hasNext() {
            return currentIndex < maxIndex;
        }

        @Override
        public Integer next() {
            currentIndex++;
            try {
                return list[currentIndex];
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException();
            }

        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public void restart() {
            this.currentIndex = this.startIndex - 1;
        }
    }
}
