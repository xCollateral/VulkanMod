package net.vulkanmod.render.chunk.buffer;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.vulkan.memory.*;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public class AreaBuffer {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Initializer.LOGGER;

    private static final MemoryType MEMORY_TYPE = MemoryTypes.GPU_MEM;

    private final int usage;
    private final int elementSize;

    private final Int2ReferenceOpenHashMap<Segment> usedSegments = new Int2ReferenceOpenHashMap<>();

    Segment first, last;

    private Buffer buffer;

    int size, used = 0;
    int segments = 0;

    public AreaBuffer(Usage usage, int elementCount, int elementSize) {
        this.usage = usage.usage;
        this.elementSize = elementSize;

        this.size = elementCount * elementSize;
        this.buffer = this.allocateBuffer();

        Segment s = new Segment(0, this.size);

        segments++;
        last = first = s;
    }

    private Buffer allocateBuffer() {
        Buffer buffer;
        if (this.usage == Usage.VERTEX.usage) {
            buffer = new VertexBuffer(this.size, MEMORY_TYPE);
        } else {
            buffer = new IndexBuffer(this.size, MEMORY_TYPE);
        }
        return buffer;
    }

    public Segment upload(ByteBuffer byteBuffer, int oldOffset, DrawBuffers.DrawParameters drawParameters) {
        // Free old segment
        if (oldOffset != -1) {
            // Need to delay segment freeing since it might be still used by prev frames in flight
//            this.setSegmentFree(oldOffset);
            MemoryManager.getInstance().addToFreeSegment(this, oldOffset);
        }

        int size = byteBuffer.remaining();

        if (DEBUG && size % elementSize != 0)
            throw new RuntimeException("Unaligned buffer");

        Segment segment = findSegment(size);

        if (segment.size - size > 0) {
            Segment s1 = new Segment(segment.offset + size, segment.size - size);
            segments++;

            if (segment.next != null) {
                s1.bindNext(segment.next);
            } else
                this.last = s1;

            segment.bindNext(s1);

            segment.size = size;
        }

        segment.free = false;
        this.usedSegments.put(segment.offset, segment);

        segment.drawParameters = drawParameters;

        Buffer dst = this.buffer;
        UploadManager.INSTANCE.recordUpload(dst, segment.offset, size, byteBuffer);

        this.used += size;

        return segment;
    }

    public Segment findSegment(int size) {
        Segment segment = null;

        Segment segment1 = this.first;
        while (segment1 != null) {
            if (segment1.isFree() && segment1.size >= size) {
                if (segment == null || segment1.size < segment.size)
                    segment = segment1;
            }

            segment1 = segment1.next;
        }

        if (segment == null || segment.size < size) {
            return this.reallocate(size);
        }

        return segment;
    }

    public Segment reallocate(int uploadSize) {
        int oldSize = this.size;

        int minIncrement = this.size >> 3;
        minIncrement = Util.align(minIncrement, this.elementSize);

        int increment = Math.max(minIncrement, uploadSize << 1);

        if (increment < uploadSize)
            throw new RuntimeException(String.format("Size increment %d < %d (Upload size)", increment, uploadSize));

        int newSize = oldSize + increment;

        this.size = newSize;
        Buffer dst = this.allocateBuffer();

        UploadManager.INSTANCE.copyBuffer(this.buffer, dst);

        // TODO: moving only used segments causes corruption
//        moveUsedSegments(dst);

        this.buffer.freeBuffer();
        this.buffer = dst;

        if (last.isFree()) {
            last.size += increment;
        }
        else {
            int offset = last.offset + last.size;
            Segment segment = new Segment(offset, newSize - offset);
            segments++;

            last.bindNext(segment);

            last = segment;
        }

        if (DEBUG)
            checkSegments();

        return last;
    }

    void moveUsedSegments(Buffer dst) {
        int srcOffset, dstOffset, uploadSize;
        int usedCount = 0;

        dstOffset = 0;
        int currOffset = dstOffset;

        Segment segment = this.first;
        Segment prevUsed = null;

        srcOffset = -1;
        uploadSize = 0;

        while (segment != null) {
            if (!segment.isFree()) {
                usedCount++;

                if (segment.offset != srcOffset + uploadSize) {

                    if (srcOffset == -1) {
                        dstOffset = 0;
                        this.first = segment;
                        segment.prev = null;
                    } else {
                        UploadManager.INSTANCE.copyBuffer(this.buffer, srcOffset, dst, dstOffset, uploadSize);

                        dstOffset += uploadSize;
                    }

                    srcOffset = segment.offset;
                    uploadSize = segment.size;

                } else {
                    uploadSize += segment.size;
                }

                this.usedSegments.remove(segment.offset);
                segment.offset = currOffset;
                currOffset += segment.size;
                updateDrawParams(segment);
                this.usedSegments.put(segment.offset, segment);

                if (prevUsed != null) {
                    prevUsed.bindNext(segment);
                }

                prevUsed = segment;
            }

            segment = segment.next;
        }

        if (uploadSize > 0) {
            UploadManager.INSTANCE.copyBuffer(this.buffer, srcOffset, dst, dstOffset, uploadSize);
        }

        if (prevUsed != null) {
            prevUsed.next = null;
            this.last = prevUsed;

            this.segments = usedCount;
        }
    }

    public void setSegmentFree(int offset) {
        Segment segment = usedSegments.remove(offset * elementSize);

        if (segment == null)
            return;

        this.used -= segment.size;

        segment.free = true;
        segment.drawParameters = null;

        Segment next = segment.next;
        if (next != null && next.isFree()) {
            mergeSegments(segment, next);
        }

        Segment prev = segment.prev;
        if (prev != null && prev.isFree()) {
            mergeSegments(prev, segment);
        }
    }

    private void mergeSegments(Segment segment, Segment next) {
        segment.size += next.size;

        if (next.next != null) {
            next.next.prev = segment;
        } else {
            this.last = segment;
        }

        segment.next = next.next;
        this.segments--;
    }

    private void updateDrawParams(Segment segment) {
        DrawBuffers.DrawParameters params = segment.drawParameters;

        int elementOffset = segment.offset / elementSize;
        if (this.usage == Usage.VERTEX.usage) {
            params.vertexOffset = elementOffset;
        } else {
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
        Segment prev = null;
        int i = 0;
        int usedSegments = 0;

        if (segment.offset != 0)
            LOGGER.error(String.format("expected first offset 0 but got %d", segment.offset));

        while (segment != null) {
            if (i >= this.segments) {
                LOGGER.error("Count is greater than segments");
                break;
            }

            if (segment.prev != prev) {
                LOGGER.error(String.format("expected previous segment not matching (segment %d)", i));
            }

            if (!segment.isFree()) {
                usedSegments++;
            }

            if (segment.offset % elementSize != 0) {
                LOGGER.error(String.format("offset %d misaligned (segment %d)", segment.offset, i));
            }

            Segment next = segment.next;

            if (next != null) {
                int offset = segment.offset + segment.size;
                if (offset != next.offset)
                    LOGGER.error(String.format("expected offset %d but got %d (segment %d)", offset, next.offset, i));

                if (next.prev != segment)
                    LOGGER.error(String.format("segment pointer not correct (segment %d)", i));

            } else {
                if (segment != this.last)
                    LOGGER.error(String.format("segment has no next pointer and it's not last (segment %d)", i));
                else {
                    int segmentEnd = segment.offset + segment.size;
                    if (segment.offset + segment.size != this.size)
                        LOGGER.error(String.format("last segment end (%d) does not match buffer size (%d)", segmentEnd, this.size));

                    // Check segmentation
                    if (segment.offset != this.used)
                        LOGGER.error(String.format("last segment offset (%d) does not match buffer used size (%d)", segmentEnd, this.size));
                }

            }

            prev = segment;
            segment = next;
            i++;
        }

        if (i != this.segments)
            LOGGER.error("Count do not match segments");

        if (usedSegments != this.usedSegments.size())
            LOGGER.error("Counted used segment do not match used segments map size");
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
        DrawBuffers.DrawParameters drawParameters;

        Segment next, prev;

        private Segment(int offset, int size) {
            this.offset = offset;
            this.size = size;
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

    }

    public enum Usage {
        VERTEX(0),
        INDEX(1);

        final int usage;

        Usage(int i) {
            usage = i;
        }
    }

}
