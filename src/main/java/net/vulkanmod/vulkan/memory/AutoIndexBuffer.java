package net.vulkanmod.vulkan.memory;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AutoIndexBuffer {

    private static final int UINT16_INDEX_MAX = 65536; //Indices overflow past 1<<16 due to use of VK_INDEX_TYPE_UINT16
    final IndexBuffer indexBuffer;

    public AutoIndexBuffer(DrawType drawType) {

        final ByteBuffer buffer = switch (drawType) {
            case QUADS -> genQuadIdxs();
            case TRIANGLE_FAN -> genTriangleFanIdxs();
            case TRIANGLE_STRIP -> genTriangleStripIdxs();
        };

        indexBuffer = new IndexBuffer(buffer.capacity(), MemoryType.GPU_MEM);
        indexBuffer.copyBuffer(buffer);

        MemoryUtil.memFree(buffer);
    }

    public static ByteBuffer genQuadIdxs() {
        //short[] idxs = {0, 1, 2, 0, 2, 3};

        int indexCount = UINT16_INDEX_MAX * 3 / 2;
        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();
        //short[] idxs = new short[indexCount];

        int j = 0;
        for(int i = 0; i < UINT16_INDEX_MAX; i += 4) {

            idxs.put(j, (short) i);
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));
            idxs.put(j + 3, (short) (i));
            idxs.put(j + 4, (short) (i + 2));
            idxs.put(j + 5, (short) (i + 3));

            j += 6;
        }

        return buffer;
        //this.type.copyIndexBuffer(this, bufferSize, idxs);
    }

    public static ByteBuffer genTriangleFanIdxs() {
        int indexCount = (UINT16_INDEX_MAX - 2) * 3;
        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        //short[] idxs = byteBuffer.asShortBuffer().array();

        int j = 0;
        for (int i = 0; i < UINT16_INDEX_MAX - 2; ++i) {
//            idxs[j] = 0;
//            idxs[j + 1] = (short) (i + 1);
//            idxs[j + 2] = (short) (i + 2);

            idxs.put(j, (short) 0);
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));

            j += 3;
        }

        buffer.rewind();
        return buffer;
    }

    public static ByteBuffer genTriangleStripIdxs() {
        int indexCount = (UINT16_INDEX_MAX - 2) * 3;

        //TODO: free buffer
        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        //short[] idxs = byteBuffer.asShortBuffer().array();

        int j = 0;
        for (int i = 0; i < UINT16_INDEX_MAX - 2; ++i) {
//            idxs[j] = 0;
//            idxs[j + 1] = (short) (i + 1);
//            idxs[j + 2] = (short) (i + 2);

            idxs.put(j, (short) i);
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));

            j += 3;
        }

        buffer.rewind();
        return buffer;
    }

    public IndexBuffer getIndexBuffer() { return indexBuffer; }

    public enum DrawType {
        QUADS(7),
        TRIANGLE_FAN(6),
        TRIANGLE_STRIP(5);

        public final int n;

        DrawType (int n) {
            this.n = n;
        }
    }
}
