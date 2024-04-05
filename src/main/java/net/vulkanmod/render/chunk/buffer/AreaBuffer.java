package net.vulkanmod.render.chunk.buffer;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.vulkan.memory.*;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public class AreaBuffer {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Initializer.LOGGER;

    private static final MemoryType MEMORY_TYPE = MemoryType.GPU_MEM;

    private final int usage;
    private final int elementSize;

    private final Int2ReferenceOpenHashMap<Segment> usedSegments = new Int2ReferenceOpenHashMap<>();

    private final Reference2ReferenceOpenHashMap<Segment, DrawBuffers.DrawParameters> parametersMap = new Reference2ReferenceOpenHashMap<>();

    Segment first, last;

    private Buffer buffer;

    int size, used = 0;
    int segments = 0;

    public AreaBuffer(Usage usage, int size, int elementSize) {

        this.usage = usage.index;
        this.elementSize = elementSize;

        this.buffer = this.allocateBuffer(size);
        this.size = size;

        Segment s = new Segment(0, size);

        segments++;
        last = first = s;
    }

    private Buffer allocateBuffer(int size) {
        int bufferSize = size;

        Buffer buffer;
        if(this.usage == Usage.VERTEX.index) {
            buffer = new VertexBuffer(bufferSize, MEMORY_TYPE);
        } else {
            buffer = new IndexBuffer(bufferSize, MEMORY_TYPE);
        }
        return buffer;
    }

    public Segment upload(ByteBuffer byteBuffer, int oldOffset, DrawBuffers.DrawParameters drawParameters) {
        // Free old segment
        if(oldOffset != -1) {
            // Need to delay segment freeing since it might be still used by prev frames in flight
//            this.setSegmentFree(oldOffset);
            MemoryManager.getInstance().addToFreeSegment(this, oldOffset);
        }

        int size = byteBuffer.remaining();

        if(size % elementSize != 0)
            throw new RuntimeException("unaligned byteBuffer");

        Segment segment = findSegment(size);

        if(segment.size - size > 0) {
            Segment s1 = new Segment(segment.offset + size, segment.size - size);
            segments++;

            if(segment.next != null) {
                s1.bindNext(segment.next);
            }
            else
                this.last = s1;

            segment.bindNext(s1);

            segment.size = size;
        }

        segment.free = false;
        this.usedSegments.put(segment.offset, segment);

        this.parametersMap.put(segment, drawParameters);

        Buffer dst = this.buffer;
        UploadManager.INSTANCE.recordUpload(dst.getId(), segment.offset, size, byteBuffer);

        this.used += size;

        return segment;
    }

    public Segment findSegment(int size) {
        Segment segment = null;

        Segment segment1 = this.first;
        while(segment1 != null) {

            if(segment1.isFree() && segment1.size >= size) {

                if(segment == null || segment1.size < segment.size)
                    segment = segment1;
            }

            segment1 = segment1.next;
        }

        if(segment == null || segment.size < size) {
            return this.reallocate(size);
        }

        return segment;
    }

    public Segment reallocate(int uploadSize) {
        int oldSize = this.size;
        int increment = this.size >> 1;

        //Try to increase size up to 8 times
        for(int i = 0; i < 8 && increment <= uploadSize; ++i) {
            increment *= 2;
        }

        if(increment < uploadSize)
            throw new RuntimeException(String.format("Size increment %d < %d (Upload size)", increment, uploadSize));

        int newSize = oldSize + increment;

        Buffer dst = this.allocateBuffer(newSize);

        UploadManager.INSTANCE.copyBuffer(this.buffer, dst);

        // TODO
//        defrag(dst);

        if(DEBUG)
            checkSegments();

        this.buffer.freeBuffer();
        this.buffer = dst;

        this.size = newSize;

        int offset = Util.align(oldSize, elementSize);

        if(last.isFree())
            last.size += increment;
        else {
            Segment segment = new Segment(offset, increment);
            segments++;

            last.bindNext(segment);

            last = segment;
        }
        return last;
    }

    void defrag(Buffer dst) {
        int srcOffset, dstOffset, size = 0;

        Segment s = this.first;
        while(s != null) {
            if(!s.isFree()) {
                s = s.next;
                continue;
            }

            mergeFreeSubsequent(s);

            Segment src = s.next;

            if(src == null) {
                return;
            }

            dstOffset = s.offset;
            srcOffset = src.offset;

            while(src != null && !src.isFree()) {
                //Swap segments
                if(s.prev != null)
                    s.prev.bindNext(src);
                if(src.next != null)
                    s.bindNext(src.next);

                src.bindNext(s);

                src.offset = s.offset;
                s.offset += src.size;

                //Update draw parameters
                updateDrawParams(src);

                size += src.size;
                src = s.next;
            }

            UploadManager.INSTANCE.copyBuffer(this.buffer, srcOffset, dst, dstOffset, size);

        }

    }

    public void setSegmentFree(int offset) {
        Segment segment = usedSegments.remove(offset * elementSize);

        if(segment == null)
            return;

        this.used -= segment.size;

        segment.free = true;
        parametersMap.remove(segment);

        Segment next = segment.next;
        if(next != null && next.isFree()) {
            mergeSegments(segment, next);
        }

        Segment prev = segment.prev;
        if(prev != null && prev.isFree()) {
            mergeSegments(prev, segment);
        }
    }

    private void mergeSegments(Segment segment, Segment next) {
        segment.size += next.size;

        if(next.next != null) {
            next.next.prev = segment;
        }
        else {
            this.last = segment;
        }

        segment.next = next.next;
        this.segments--;
    }

    private void mergeFreeSubsequent(Segment segment) {
        Segment next = segment.next;
        while(next != null && next.isFree()) {
            mergeSegments(segment, next);

            next = segment.next;
        }
    }

    private void updateDrawParams(Segment segment) {
        var params = this.parametersMap.get(segment);

        int elementOffset = segment.offset / elementSize;
        if(this.usage == Usage.VERTEX.index) {
            params.vertexOffset = elementOffset;
        }
        else {
            params.firstIndex = elementOffset;
        }
    }

    public long getId() {
        return this.buffer.getId();
    }

    public void freeBuffer() {
        this.buffer.freeBuffer();
    }

    public int fragmentation() {
        return (size - used) - (last.isFree() ? last.size : 0);
    }

    public void checkSegments() {
        Segment segment = first;
        int i = 0;

        while(segment != null) {
            Segment next = segment.next;

            if(next != null) {
                int offset = segment.offset + segment.size;
                if (offset != next.offset)
                    LOGGER.error(String.format("expected offset %d but got %d (segment %d)", offset, next.offset, i));

                if(next.prev != segment)
                    LOGGER.error(String.format("segment pointer not correct (segment %d)", i));
            }
            else {
                if(segment != this.last)
                    LOGGER.error(String.format("segment has no next pointer and it's not last (segment %d)", i));
            }

            segment = next;
            i++;
        }
    }

    public int getSize() {
        return size;
    }

    public int getUsed() {
        return used;
    }

    public static class Segment {
        int offset, size;
        boolean free = true;

        Segment next, prev;

        private Segment(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }

        public void reset() {
            this.offset = -1;
            this.size = -1;
        }

        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return size;
        }

        public boolean isFree() {
            return free;
        }

        public void setFree(boolean free) {
            this.free = free;
        }

        public void bindNext(Segment s) {
            this.next = s;
            s.prev = this;
        }

        public void merge() {
            Segment next = this.next;

            this.size += next.size;

            if(next.next != null) {
                next.next.prev = this;
            }

            this.next = next.next;
        }
    }

    public enum Usage {
        VERTEX(0),
        INDEX(1);

        final int index;

        Usage(int i) {
            index = i;
        }
    }

}
