package net.vulkanmod.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.util.SortUtil;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class QuadSorter {

    private Vector3f[] sortingPoints;
    private float sortX = Float.NaN;
    private float sortY = Float.NaN;
    private float sortZ = Float.NaN;
    private boolean indexOnly;

    private VertexFormat format;
    private int vertexCount;

    public void setQuadSortOrigin(float x, float y, float z) {
        this.sortX = x;
        this.sortY = y;
        this.sortZ = z;
    }

    public SortState getSortState() {
        return new SortState(this.vertexCount, this.sortingPoints);
    }

    public void restoreSortState(QuadSorter.SortState sortState) {
        this.vertexCount = sortState.vertexCount;
        this.sortingPoints = sortState.sortingPoints;

        this.indexOnly = true;
    }

    public void setupQuadSortingPoints(long bufferPtr, int vertexCount, VertexFormat format) {
        this.vertexCount = vertexCount;
        int pointCount = vertexCount / 4;
        Vector3f[] sortingPoints = new Vector3f[pointCount];

        int vertexSize = format.getVertexSize();
        int quadStride = vertexSize * 4;
        int offset = vertexSize * 2;

        if (format == CustomVertexFormat.COMPRESSED_TERRAIN) {
            final float invConv = 1.0f / VertexBuilder.CompressedVertexBuilder.POS_CONV_MUL;
            final float convOffset = -VertexBuilder.CompressedVertexBuilder.POS_OFFSET;

            for (int m = 0; m < pointCount; ++m) {
                long ptr = bufferPtr + (long) m * quadStride;

                short x0 = MemoryUtil.memGetShort(ptr + 0);
                short y0 = MemoryUtil.memGetShort(ptr + 2);
                short z0 = MemoryUtil.memGetShort(ptr + 4);
                short x2 = MemoryUtil.memGetShort(ptr + offset + 0);
                short y2 = MemoryUtil.memGetShort(ptr + offset + 2);
                short z2 = MemoryUtil.memGetShort(ptr + offset + 4);

                float xa = (x0 + x2) * invConv * 0.5f + convOffset;
                float ya = (y0 + y2) * invConv * 0.5f + convOffset;
                float za = (z0 + z2) * invConv * 0.5f + convOffset;
                sortingPoints[m] = new Vector3f(xa, ya, za);
            }
        } else {
            for (int m = 0; m < pointCount; ++m) {
                long ptr = bufferPtr + (long) m * quadStride;

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

        this.sortingPoints = sortingPoints;
    }

    public void putSortedQuadIndices(TerrainBufferBuilder bufferBuilder, VertexFormat.IndexType indexType) {
        float[] distances = new float[this.sortingPoints.length];
        int[] sortingPoints = new int[this.sortingPoints.length];

        for (int i = 0; i < this.sortingPoints.length; sortingPoints[i] = i++) {
            float dx = this.sortingPoints[i].x() - this.sortX;
            float dy = this.sortingPoints[i].y() - this.sortY;
            float dz = this.sortingPoints[i].z() - this.sortZ;
            distances[i] = dx * dx + dy * dy + dz * dz;
        }

        SortUtil.mergeSort(sortingPoints, distances);

        long ptr = bufferBuilder.getPtr();

        final int size = indexType.bytes;
        final int stride = 4; // 4 vertices in a quad
        for (int i = 0; i < sortingPoints.length; ++i) {
            final int quadIndex = sortingPoints[i];
            final int baseVertex = quadIndex * stride;

            MemoryUtil.memPutInt(ptr + (size * 0L), baseVertex + 0);
            MemoryUtil.memPutInt(ptr + (size * 1L), baseVertex + 1);
            MemoryUtil.memPutInt(ptr + (size * 2L), baseVertex + 2);
            MemoryUtil.memPutInt(ptr + (size * 3L), baseVertex + 2);
            MemoryUtil.memPutInt(ptr + (size * 4L), baseVertex + 3);
            MemoryUtil.memPutInt(ptr + (size * 5L), baseVertex + 0);

            ptr += size * 6L;
        }
    }

    public static class SortState {
        final int vertexCount;
        final Vector3f[] sortingPoints;

        SortState(int vertexCount, Vector3f[] sortingPoints) {
            this.vertexCount = vertexCount;
            this.sortingPoints = sortingPoints;

        }
    }
}
