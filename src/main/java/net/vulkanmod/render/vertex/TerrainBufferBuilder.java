package net.vulkanmod.render.vertex;

import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.util.BufferUtil;
import net.vulkanmod.render.util.SortUtil;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class TerrainBufferBuilder {
    private static final Logger LOGGER = Initializer.LOGGER;

    private ByteBuffer buffer;
    protected long bufferPtr;
    protected int nextElementByte;
    private int vertices;

    private int renderedBufferCount;
    private int renderedBufferPointer;

    private final VertexFormat format;

    private boolean building;

    private Vector3f[] sortingPoints;
    private float sortX = Float.NaN;
    private float sortY = Float.NaN;
    private float sortZ = Float.NaN;
    private boolean indexOnly;

    protected VertexBuilder vertexBuilder;

    public TerrainBufferBuilder(int size) {
        this.buffer = MemoryTracker.create(size * 6);
        this.bufferPtr = MemoryUtil.memAddress0(this.buffer);

        this.format = PipelineManager.TERRAIN_VERTEX_FORMAT;
        this.vertexBuilder = PipelineManager.TERRAIN_VERTEX_FORMAT == CustomVertexFormat.COMPRESSED_TERRAIN ? new CompressedVertexBuilder() : new DefaultVertexBuilder();
    }

    public void ensureCapacity() {
        this.ensureCapacity(this.format.getVertexSize() * 4);
    }

    private void ensureCapacity(int size) {
        if (this.nextElementByte + size > this.buffer.capacity()) {
            int capacity = this.buffer.capacity();
            int newSize = (capacity + size) * 2;
            LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", capacity, newSize);
            this.resize(newSize);
        }
    }

    private void resize(int newSize) {
        ByteBuffer byteBuffer = MemoryTracker.resize(this.buffer, newSize);
        byteBuffer.rewind();
        this.buffer = byteBuffer;
        this.bufferPtr = MemoryUtil.memAddress0(this.buffer);
    }

    public void setQuadSortOrigin(float f, float g, float h) {
        if (this.sortX != f || this.sortY != g || this.sortZ != h) {
            this.sortX = f;
            this.sortY = g;
            this.sortZ = h;
            if (this.sortingPoints == null) {
                this.sortingPoints = this.makeQuadSortingPoints();
            }
        }
    }

    public SortState getSortState() {
        return new SortState(VertexFormat.Mode.QUADS, this.vertices, this.sortingPoints, this.sortX, this.sortY, this.sortZ);
    }

    public void restoreSortState(SortState sortState) {
        this.buffer.rewind();
        this.vertices = sortState.vertices;
        this.nextElementByte = this.renderedBufferPointer;
        this.sortingPoints = sortState.sortingPoints;
        this.sortX = sortState.sortX;
        this.sortY = sortState.sortY;
        this.sortZ = sortState.sortZ;
        this.indexOnly = true;
    }

    public void begin() {
        if (this.building) {
            throw new IllegalStateException("Already building!");
        } else {
            this.building = true;
            this.buffer.rewind();
        }
    }

    private Vector3f[] makeQuadSortingPoints() {
        int pointsNum = this.vertices / 4;
        Vector3f[] sortingPoints = new Vector3f[pointsNum];

        int stride = this.format.getVertexSize() * 4;
        int vertexSize = this.format.getVertexSize();
        int offset = vertexSize * 2;

        if (this.format == CustomVertexFormat.COMPRESSED_TERRAIN) {
            final float invConv = 1.0f / CompressedVertexBuilder.POS_CONV_MUL;

            for (int m = 0; m < pointsNum; ++m) {
                long ptr = this.bufferPtr + this.renderedBufferPointer + (long) m * stride;

                short x0 = MemoryUtil.memGetShort(ptr + 0);
                short y0 = MemoryUtil.memGetShort(ptr + 2);
                short z0 = MemoryUtil.memGetShort(ptr + 4);
                short x2 = MemoryUtil.memGetShort(ptr + offset + 0);
                short y2 = MemoryUtil.memGetShort(ptr + offset + 2);
                short z2 = MemoryUtil.memGetShort(ptr + offset + 4);

                float xa = (x0 + x2) * invConv * 0.5f;
                float ya = (y0 + y2) * invConv * 0.5f;
                float za = (z0 + z2) * invConv * 0.5f;
                sortingPoints[m] = new Vector3f(xa, ya, za);
            }
        } else {
            for (int m = 0; m < pointsNum; ++m) {
                long ptr = this.bufferPtr + this.renderedBufferPointer + (long) m * stride;

                float x0 = MemoryUtil.memGetFloat(ptr + 0);
                float y0 = MemoryUtil.memGetFloat(ptr + 4);
                float z0 = MemoryUtil.memGetFloat(ptr + 8);
                float x2 = MemoryUtil.memGetFloat(ptr + offset + 0);
                float y2 = MemoryUtil.memGetFloat(ptr + offset + 4);
                float z2 = MemoryUtil.memGetFloat(ptr + offset + 8);

                float q = (x0 + x2) * 0.5f;
                float r = (y0 + y2) * 0.5f;
                float s = (z0 + z2) * 0.5f;
                sortingPoints[m] = new Vector3f(q, r, s);
            }
        }

        return sortingPoints;
    }

    private void putSortedQuadIndices(VertexFormat.IndexType indexType) {
        float[] distances = new float[this.sortingPoints.length];
        int[] sortingPoints = new int[this.sortingPoints.length];

        for (int i = 0; i < this.sortingPoints.length; sortingPoints[i] = i++) {
            float dx = this.sortingPoints[i].x() - this.sortX;
            float dy = this.sortingPoints[i].y() - this.sortY;
            float dz = this.sortingPoints[i].z() - this.sortZ;
            distances[i] = dx * dx + dy * dy + dz * dz;
        }

        SortUtil.mergeSort(sortingPoints, distances);

        long ptr = this.bufferPtr + this.nextElementByte;

        final int size = getIndexSize(indexType);
        final int stride = 4; // 4 vertices in a quad
        for (int i = 0; i < sortingPoints.length; ++i) {
            int quadIndex = sortingPoints[i];

            MemoryUtil.memPutInt(ptr + (size * 0L), quadIndex * stride + 0);
            MemoryUtil.memPutInt(ptr + (size * 1L), quadIndex * stride + 1);
            MemoryUtil.memPutInt(ptr + (size * 2L), quadIndex * stride + 2);
            MemoryUtil.memPutInt(ptr + (size * 3L), quadIndex * stride + 2);
            MemoryUtil.memPutInt(ptr + (size * 4L), quadIndex * stride + 3);
            MemoryUtil.memPutInt(ptr + (size * 5L), quadIndex * stride + 0);

            ptr += size * 6L;
        }
    }

    private static int getIndexSize(VertexFormat.IndexType indexType) {
        return switch (indexType) {
            case SHORT -> 2;
            case INT -> 4;
        };
    }

    public boolean isCurrentBatchEmpty() {
        return this.vertices == 0;
    }

    @Nullable
    public RenderedBuffer end() {
        this.ensureDrawing();
        if (this.isCurrentBatchEmpty()) {
            this.reset();
            return null;
        } else {
            RenderedBuffer renderedBuffer = this.storeRenderedBuffer();
            this.reset();
            return renderedBuffer;
        }
    }

    private void ensureDrawing() {
        if (!this.building) {
            throw new IllegalStateException("Not building!");
        }
    }

    private RenderedBuffer storeRenderedBuffer() {
        int indexCount = this.vertices / 4 * 6;
        int vertexBufferSize = !this.indexOnly ? this.vertices * this.format.getVertexSize() : 0;
        VertexFormat.IndexType indexType = VertexFormat.IndexType.least(indexCount);
        boolean sequentialIndexing;
        int size;

        if (this.sortingPoints != null) {
            int indexBufferSize = indexCount * indexType.bytes;
            this.ensureCapacity(indexBufferSize);
            this.putSortedQuadIndices(indexType);
            sequentialIndexing = false;
            this.nextElementByte += indexBufferSize;
            size = vertexBufferSize + indexBufferSize;
        } else {
            sequentialIndexing = true;
            size = vertexBufferSize;
        }

        int ptr = this.renderedBufferPointer;
        this.renderedBufferPointer += size;
        ++this.renderedBufferCount;

        DrawState drawState = new DrawState(this.format.getVertexSize(), this.vertices, indexCount, indexType, this.indexOnly, sequentialIndexing);
        return new RenderedBuffer(ptr, drawState);
    }

    public void reset() {
        this.building = false;
        this.vertices = 0;

        this.sortingPoints = null;
        this.sortX = Float.NaN;
        this.sortY = Float.NaN;
        this.sortZ = Float.NaN;
        this.indexOnly = false;
    }

    void releaseRenderedBuffer() {
        if (this.renderedBufferCount > 0 && --this.renderedBufferCount == 0) {
            this.clear();
        }

    }

    public void clear() {
        if (this.renderedBufferCount > 0) {
            LOGGER.warn("Clearing BufferBuilder with unused batches");
        }

        this.discard();
    }

    public void discard() {
        this.renderedBufferCount = 0;
        this.renderedBufferPointer = 0;
        this.nextElementByte = 0;
    }

    public boolean building() {
        return this.building;
    }

    public void endVertex() {
        this.nextElementByte += this.vertexBuilder.getStride();
        ++this.vertices;
    }

    public void vertex(float x, float y, float z, int color, float u, float v, int light, int packedNormal) {
        final long ptr = this.bufferPtr + this.nextElementByte;
        this.vertexBuilder.vertex(ptr, x, y, z, color, u, v, light, packedNormal);
        this.endVertex();
    }

    public void setBlockAttributes(BlockState blockState) {
    }

    public long getPtr() {
        return this.bufferPtr + this.nextElementByte;
    }

    public static class SortState {
        final VertexFormat.Mode mode;
        final int vertices;
        @Nullable
        final Vector3f[] sortingPoints;
        final float sortX;
        final float sortY;
        final float sortZ;

        SortState(VertexFormat.Mode mode, int i, @Nullable Vector3f[] vector3fs, float f, float g, float h) {
            this.mode = mode;
            this.vertices = i;
            this.sortingPoints = vector3fs;
            this.sortX = f;
            this.sortY = g;
            this.sortZ = h;
        }
    }

    public class RenderedBuffer {
        private final int pointer;
        private final DrawState drawState;
        private boolean released;

        RenderedBuffer(int pointer, DrawState drawState) {
            this.pointer = pointer;
            this.drawState = drawState;
        }

        public ByteBuffer vertexBuffer() {
            int start = this.pointer + this.drawState.vertexBufferStart();
            int end = this.pointer + this.drawState.vertexBufferEnd();
            return BufferUtil.bufferSlice(TerrainBufferBuilder.this.buffer, start, end);
        }

        public ByteBuffer indexBuffer() {
            int start = this.pointer + this.drawState.indexBufferStart();
            int end = this.pointer + this.drawState.indexBufferEnd();
            return BufferUtil.bufferSlice(TerrainBufferBuilder.this.buffer, start, end);
        }

        public DrawState drawState() {
            return this.drawState;
        }

        public boolean isEmpty() {
            return this.drawState.vertexCount == 0;
        }

        public void release() {
            if (this.released) {
                throw new IllegalStateException("Buffer has already been released!");
            } else {
                TerrainBufferBuilder.this.releaseRenderedBuffer();
                this.released = true;
            }
        }
    }

    public record DrawState(int vertexSize, int vertexCount, int indexCount, VertexFormat.IndexType indexType,
                            boolean indexOnly, boolean sequentialIndex) {

        public int vertexBufferSize() {
            return this.vertexCount * this.vertexSize;
        }

        public int vertexBufferStart() {
            return 0;
        }

        public int vertexBufferEnd() {
            return this.vertexBufferSize();
        }

        public int indexBufferStart() {
            return this.indexOnly ? 0 : this.vertexBufferEnd();
        }

        public int indexBufferEnd() {
            return this.indexBufferStart() + this.indexBufferSize();
        }

        private int indexBufferSize() {
            return this.sequentialIndex ? 0 : this.indexCount * this.indexType.bytes;
        }

        public int bufferSize() {
            return this.indexBufferEnd();
        }

        public int vertexCount() {
            return this.vertexCount;
        }

        public int indexCount() {
            return this.indexCount;
        }

        public VertexFormat.IndexType indexType() {
            return this.indexType;
        }

        public boolean indexOnly() {
            return this.indexOnly;
        }

        public boolean sequentialIndex() {
            return this.sequentialIndex;
        }
    }

    public interface VertexBuilder {
        void vertex(long ptr, float x, float y, float z, int color, float u, float v, int light, int packedNormal);

        int getStride();
    }

    static class DefaultVertexBuilder implements VertexBuilder {
        private static final int VERTEX_SIZE = 32;

        public void vertex(long ptr, float x, float y, float z, int color, float u, float v, int light, int packedNormal) {
            MemoryUtil.memPutFloat(ptr + 0, x);
            MemoryUtil.memPutFloat(ptr + 4, y);
            MemoryUtil.memPutFloat(ptr + 8, z);

            MemoryUtil.memPutInt(ptr + 12, color);

            MemoryUtil.memPutFloat(ptr + 16, u);
            MemoryUtil.memPutFloat(ptr + 20, v);

            MemoryUtil.memPutShort(ptr + 24, (short) (light & '\uffff'));
            MemoryUtil.memPutShort(ptr + 26, (short) (light >> 16 & '\uffff'));

            MemoryUtil.memPutInt(ptr + 28, packedNormal);
        }

        @Override
        public int getStride() {
            return VERTEX_SIZE;
        }
    }

    static class CompressedVertexBuilder implements VertexBuilder {
        private static final int VERTEX_SIZE = 20;

        public static final float POS_CONV_MUL = 2048.0f;
        public static final float POS_OFFSET = -4.0f;
        public static final float POS_OFFSET_CONV = POS_OFFSET * POS_CONV_MUL;

        public static final float UV_CONV_MUL = 32768.0f;

        public void vertex(long ptr, float x, float y, float z, int color, float u, float v, int light, int packedNormal) {
            final short sX = (short) (x * POS_CONV_MUL + POS_OFFSET_CONV);
            final short sY = (short) (y * POS_CONV_MUL + POS_OFFSET_CONV);
            final short sZ = (short) (z * POS_CONV_MUL + POS_OFFSET_CONV);

            MemoryUtil.memPutShort(ptr + 0, sX);
            MemoryUtil.memPutShort(ptr + 2, sY);
            MemoryUtil.memPutShort(ptr + 4, sZ);

            final short l = (short) (((light >>> 8) & 0xFF00) | (light & 0xFF));
            MemoryUtil.memPutShort(ptr + 6, l);

            MemoryUtil.memPutInt(ptr + 8, color);

            MemoryUtil.memPutShort(ptr + 12, (short) (u * UV_CONV_MUL));
            MemoryUtil.memPutShort(ptr + 14, (short) (v * UV_CONV_MUL));
        }

        @Override
        public int getStride() {
            return VERTEX_SIZE;
        }
    }
}
