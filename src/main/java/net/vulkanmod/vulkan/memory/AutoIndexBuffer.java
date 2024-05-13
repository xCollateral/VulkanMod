package net.vulkanmod.vulkan.memory;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AutoIndexBuffer {
    private static final int UINT16_INDEX_MAX = 65536;
    IndexBuffer indexBuffer;

    public AutoIndexBuffer(DrawType drawType) {
        final ByteBuffer buffer = switch (drawType) {
            case QUADS -> genQuadIndices();
            case LINES -> genLineIndices();
            case TRIANGLE_FAN -> genTriangleFanIndices();
            case DEBUG_LINES, TRIANGLE_STRIP, LINE_STRIP -> genSequentialIndices();
            default -> throw new RuntimeException(String.format("Unknown drawType: %s", drawType));
        };

        this.indexBuffer = new IndexBuffer(buffer.capacity(), MemoryTypes.GPU_MEM);
        this.indexBuffer.copyBuffer(buffer);

        MemoryUtil.memFree(buffer);
    }

    public static ByteBuffer genQuadIndices() {
        ByteBuffer buffer = MemoryUtil.memAlloc(UINT16_INDEX_MAX / 4 * 6 * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();

        for (int i = 0, j = 0; i < UINT16_INDEX_MAX; i += 4, j += 6) {
            indices.put(j, (short) i);
            indices.put(j + 1, (short) (i + 1));
            indices.put(j + 2, (short) (i + 2));
            indices.put(j + 3, (short) (i + 2));
            indices.put(j + 4, (short) (i + 3));
            indices.put(j + 5, (short) i);
        }

        return buffer;
    }

    public static ByteBuffer genLineIndices() {
        ByteBuffer buffer = MemoryUtil.memAlloc(UINT16_INDEX_MAX / 4 * 6 * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();

        for (int i = 0, j = 0; i < UINT16_INDEX_MAX; i += 4, j += 6) {
            indices.put(j, (short) i);
            indices.put(j + 1, (short) (i + 1));
            indices.put(j + 2, (short) (i + 2));
            indices.put(j + 3, (short) (i + 3));
            indices.put(j + 4, (short) (i + 2));
            indices.put(j + 5, (short) (i + 1));
        }

        return buffer;
    }

    public static ByteBuffer genTriangleFanIndices() {
        ByteBuffer buffer = MemoryUtil.memAlloc((UINT16_INDEX_MAX - 2) * 3 * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();
        
        for (int i = 0, j = 0; i < UINT16_INDEX_MAX - 2; i += 1, j += 3) {
            indices.put(j, (short) 0);
            indices.put(j + 1, (short) (i + 1));
            indices.put(j + 2, (short) (i + 2));
        }

        return buffer;
    }

    public static ByteBuffer genSequentialIndices() {
        ByteBuffer buffer = MemoryUtil.memAlloc(UINT16_INDEX_MAX * Short.BYTES);
        ShortBuffer indices = buffer.asShortBuffer();

        for (int i = 0; i < UINT16_INDEX_MAX; i += 1) {
            indices.put(i, (short) i);
        }

        return buffer;
    }

    public IndexBuffer getIndexBuffer() { return this.indexBuffer; }

    public void freeBuffer() {
        if (this.indexBuffer == null) {
            return;
        }

        this.indexBuffer.freeBuffer();
        this.indexBuffer = null;
    }

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
