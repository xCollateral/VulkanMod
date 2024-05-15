package net.vulkanmod.vulkan.memory;

import net.vulkanmod.Initializer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AutoIndexBuffer {
    int vertexCount;
    DrawType drawType;
    IndexBuffer indexBuffer;

    public AutoIndexBuffer(int vertexCount, DrawType type) {
        this.drawType = type;

        createIndexBuffer(vertexCount);
    }

    private void createIndexBuffer(int vertexCount) {
        this.vertexCount = vertexCount;
        ByteBuffer buffer;

        switch (this.drawType) {
            case QUADS -> buffer = genQuadIndices(vertexCount);
            case TRIANGLE_FAN -> buffer = genTriangleFanIndices(vertexCount);
            case TRIANGLE_STRIP -> buffer = genTriangleStripIndices(vertexCount);
            case LINES -> buffer = genLinesIndices(vertexCount);
            case DEBUG_LINE_STRIP -> buffer = genDebugLineStripIndices(vertexCount);
            default -> throw new IllegalArgumentException("Unsupported drawType: %s".formatted(this.drawType));
        }

        int size = buffer.capacity();
        this.indexBuffer = new IndexBuffer(size, MemoryTypes.GPU_MEM);
        this.indexBuffer.copyBuffer(buffer);

        MemoryUtil.memFree(buffer);
    }

    public void checkCapacity(int vertexCount) {
        if(vertexCount > this.vertexCount) {
            int newVertexCount = this.vertexCount * 2;
            Initializer.LOGGER.info("Reallocating AutoIndexBuffer from {} to {}", this.vertexCount, newVertexCount);

            this.indexBuffer.freeBuffer();
            createIndexBuffer(newVertexCount);
        }
    }

    public static ByteBuffer genQuadIndices(int vertexCount) {
        int indexCount = vertexCount * 3 / 2;
        indexCount = roundUpToDivisible(indexCount, 6);

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for(int i = 0; i < vertexCount; i += 4) {
            idxs.put(j, (short) (i));
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));
            idxs.put(j + 3, (short) (i));
            idxs.put(j + 4, (short) (i + 2));
            idxs.put(j + 5, (short) (i + 3));

            j += 6;
        }

        return buffer;
    }

    public static ByteBuffer genLinesIndices(int vertexCount) {
        int indexCount = vertexCount * 3 / 2;
        indexCount = roundUpToDivisible(indexCount, 6);

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for(int i = 0; i < vertexCount; i += 4) {
            idxs.put(j, (short) (i));
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));
            idxs.put(j + 3, (short) (i + 3));
            idxs.put(j + 4, (short) (i + 2));
            idxs.put(j + 5, (short) (i + 1));

            j += 6;
        }

        return buffer;
    }

    public static ByteBuffer genTriangleFanIndices(int vertexCount) {
        int indexCount = (vertexCount - 2) * 3;
        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount - 2; ++i) {
            idxs.put(j, (short) 0);
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));

            j += 3;
        }

        return buffer;
    }

    public static ByteBuffer genTriangleStripIndices(int vertexCount) {
        int indexCount = (vertexCount - 2) * 3;

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount - 2; ++i) {
            idxs.put(j, (short) i);
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));

            j += 3;
        }

        return buffer;
    }

    public static ByteBuffer genDebugLineStripIndices(int vertexCount) {
        int indexCount = (vertexCount - 1) * 2;

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount - 1; ++i) {
            idxs.put(j, (short) i);
            idxs.put(j + 1, (short) (i + 1));

            j += 2;
        }

        return buffer;
    }

    public static int roundUpToDivisible(int n, int d) {
        return ((n + d - 1) / d) * d;
    }

    public IndexBuffer getIndexBuffer() { return this.indexBuffer; }

    public void freeBuffer() {
        this.indexBuffer.freeBuffer();
    }

    public enum DrawType {
        QUADS(7),
        TRIANGLE_FAN(6),
        TRIANGLE_STRIP(5),
        DEBUG_LINE_STRIP(4),
        DEBUG_LINES(3),
        LINES(1); // Emulates lines with quads

        public final int n;

        DrawType(int n) {
            this.n = n;
        }

        public static int getIndexCount(DrawType drawType, int vertexCount) {
            return switch (drawType) {
                case QUADS, LINES -> vertexCount * 3 / 2;
                case TRIANGLE_FAN, TRIANGLE_STRIP -> (vertexCount - 2) * 3;
                default -> 0;
            };
        }

        public static int getQuadIndexCount(int vertexCount) {
            return vertexCount * 3 / 2;
        }

        public static int getTriangleStripIndexCount(int vertexCount) {
            return (vertexCount - 2) * 3;
        }
    }
}
