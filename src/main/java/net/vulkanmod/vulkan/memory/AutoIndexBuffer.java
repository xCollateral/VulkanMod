package net.vulkanmod.vulkan.memory;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.vertex.VertexFormat;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AutoIndexBuffer {
    private static final int UINT16_INDEX_MAX = 65536;
    private final IndexBuffer indexBuffer;

    public interface IndexConsumer {
        public void accept(int i, int index);
    }

    public interface IndexGenerator {
        public void accept(IndexConsumer consumer, int vertex, int index);
    }

    public AutoIndexBuffer(final int vertexSubtrahend, final int vertexStride, final int indexStride, final IndexGenerator generator) {
        final int indexCount = ((UINT16_INDEX_MAX - vertexSubtrahend) + (vertexStride - 1)) / vertexStride * indexStride;
        final ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        final ShortBuffer indices = buffer.asShortBuffer();
        final IndexConsumer indexConsumer = (i, index) -> {
            indices.put(i, (short) index);
        };
        for (int i = 0; i < indexCount; i += indexStride) {
            generator.accept(indexConsumer, i / indexStride * vertexStride, i);
        }

        this.indexBuffer = new IndexBuffer(buffer.capacity(), MemoryTypes.GPU_MEM);
        this.indexBuffer.copyBuffer(buffer);

        MemoryUtil.memFree(buffer);
    }

    public final IndexBuffer getIndexBuffer() { return this.indexBuffer; }

    public void freeBuffer() {
        if (this.indexBuffer == null) {
            return;
        }

        this.indexBuffer.freeBuffer();
    }

    public enum DrawType {
        QUADS(7),
        TRIANGLE_FAN(6),
        TRIANGLE_STRIP(5),
        DEBUG_LINES(4),
        LINE_STRIP(3),
        LINES(2),
        DEBUG_LINE_STRIP(1);

        public final int n;

        DrawType(final int n) {
            this.n = n;
        }

        public static final DrawType fromVertexFormat(final VertexFormat.Mode mode) {
            return switch (mode) {
                case QUADS -> DrawType.QUADS;
                case TRIANGLE_FAN -> DrawType.TRIANGLE_FAN;
                case TRIANGLE_STRIP -> DrawType.TRIANGLE_STRIP;
                case DEBUG_LINES -> DrawType.DEBUG_LINES;
                case LINE_STRIP -> DrawType.LINE_STRIP;
                case LINES -> DrawType.LINES;
                case DEBUG_LINE_STRIP -> DrawType.DEBUG_LINE_STRIP;
                default -> throw new IllegalArgumentException(String.format("Invalid VertexFormat.Mode %s for AutoIndexBuffer", mode));
            };
        }

        public final int indexCount(final int vertexCount) {
            return switch (this) {
                case QUADS, LINES -> (vertexCount + 3) / 4 * 6;
                case TRIANGLE_FAN, TRIANGLE_STRIP, LINE_STRIP -> (vertexCount - 2) * 3;
                case DEBUG_LINE_STRIP -> (vertexCount - 1) * 2;
                default -> vertexCount;
            };
        }
    }
}
