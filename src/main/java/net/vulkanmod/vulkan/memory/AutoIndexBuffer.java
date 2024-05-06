package net.vulkanmod.vulkan.memory;

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
        int size;
        ByteBuffer buffer;

        switch (drawType) {
            case QUADS -> {
                size = vertexCount / 4 * 6 * IndexBuffer.IndexType.SHORT.size;
                buffer = genQuadIndices(vertexCount);
            }
            case LINES -> {
                size = vertexCount / 4 * 6 * IndexBuffer.IndexType.SHORT.size;
                buffer = genLineIndices(vertexCount);
            }
            case TRIANGLE_FAN -> {
                size = (vertexCount - 2) * 3 * IndexBuffer.IndexType.SHORT.size;
                buffer = genTriangleFanIndices(vertexCount);
            }
            case TRIANGLE_STRIP, DEBUG_LINES, LINE_STRIP -> {
                size = vertexCount * IndexBuffer.IndexType.SHORT.size;
                buffer = genSequentialIndices(vertexCount);
            }
            default -> throw new RuntimeException("unknown drawType");
        }

        indexBuffer = new IndexBuffer(size, MemoryTypes.GPU_MEM);
        indexBuffer.copyBuffer(buffer);

        MemoryUtil.memFree(buffer);
    }

    public void checkCapacity(int vertexCount) {
        if (vertexCount <= this.vertexCount) {
            return;
        }

        System.out.println("Reallocating AutoIndexBuffer from " + this.vertexCount + " to " + vertexCount);

        // TODO: free old
        // Can't know when VBO will stop using it
        indexBuffer.freeBuffer();
        createIndexBuffer(vertexCount);
    }

    public static ByteBuffer genQuadIndices(int vertexCount) {
        ByteBuffer buffer = MemoryUtil.memCalloc(vertexCount / 4 * 6 * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount; i += 4) {
            indices.put(j, (short) i);
            indices.put(j + 1, (short) (i + 1));
            indices.put(j + 2, (short) (i + 2));
            indices.put(j + 3, (short) (i + 2));
            indices.put(j + 4, (short) (i + 3));
            indices.put(j + 5, (short) i);

            j += 6;
        }

        return buffer;
    }

    public static ByteBuffer genLineIndices(int vertexCount) {
        ByteBuffer buffer = MemoryUtil.memCalloc(vertexCount / 4 * 6 * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount; i += 4) {
            indices.put(j, (short) i);
            indices.put(j + 1, (short) (i + 1));
            indices.put(j + 2, (short) (i + 2));
            indices.put(j + 3, (short) (i + 3));
            indices.put(j + 4, (short) (i + 2));
            indices.put(j + 5, (short) (i + 1));

            j += 6;
        }

        return buffer;
    }

    public static ByteBuffer genTriangleFanIndices(int vertexCount) {
        ByteBuffer buffer = MemoryUtil.memCalloc((vertexCount - 2) * 3 * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();
        
        int j = 0;
        for (int i = 0; i < vertexCount - 2; i += 1) {
            indices.put(j, (short) 0);
            indices.put(j + 1, (short) (i + 1));
            indices.put(j + 2, (short) (i + 2));
        }

        return buffer;
    }

    public static ByteBuffer genSequentialIndices(int vertexCount) {
        ByteBuffer buffer = MemoryUtil.memCalloc(vertexCount * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();

        for (int i = 0; i < vertexCount; i += 1) {
            indices.put(i, (short) i);
        }

        return buffer;
    }

    public IndexBuffer getIndexBuffer() { return indexBuffer; }

    public enum DrawType {
        QUADS(7),
        TRIANGLE_FAN(6),
        TRIANGLE_STRIP(5),
        DEBUG_LINES(4),
        LINE_STRIP(3),
        LINES(2);

        public final int n;

        DrawType(int n) {
            this.n = n;
        }
    }
}
