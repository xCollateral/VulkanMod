package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

// TODO: This class is only used to emulate a CPU buffer for texture copying purposes
//  any other use is not supported
public class GlBuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlBuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlBuffer boundBuffer;

    private static GlBuffer pixelUnpackBufferBound;

    public static int glGenBuffers() {
        int id = ID_COUNTER;
        map.put(id, new GlBuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void glBindBuffer(int target, int buffer) {
        boundId = buffer;
        boundBuffer = map.get(buffer);

        if (buffer <= 0)
            return;

        if (boundBuffer == null)
            throw new NullPointerException("bound texture is null");

        checkTarget(target);

        boundBuffer.target = target;
    }

    public static void glBufferData(int target, ByteBuffer byteBuffer, int usage) {
        checkTarget(target);

        pixelUnpackBufferBound = boundBuffer;
    }

    public static void glBufferData(int target, long size, int usage) {
        checkTarget(target);

        pixelUnpackBufferBound = boundBuffer;

        boundBuffer.allocate((int) size);
    }

    @Nullable
    public static ByteBuffer glMapBuffer(int target, int access) {
        checkTarget(target);

        return boundBuffer.data;
    }

    public static boolean glUnmapBuffer(int i) {
        return true;
    }

    public static void glDeleteBuffers(int id) {
        var buffer = map.remove(id);

        if (buffer != null)
            buffer.freeData();
    }

    public static GlBuffer getPixelUnpackBufferBound() {
        return pixelUnpackBufferBound;
    }

    private static void checkTarget(int target) {
        if (target != GL32.GL_PIXEL_UNPACK_BUFFER)
            throw new IllegalArgumentException("target %d not supported".formatted(target));
    }

    int id;
    int target;

    ByteBuffer data;

    public GlBuffer(int id) {
        this.id = id;
    }

    private void allocate(int size) {
        if (this.data != null)
            this.freeData();

        this.data = MemoryUtil.memAlloc(size);
    }

    private ByteBuffer getData() {
        return this.data;
    }

    private void freeData() {
        MemoryUtil.memFree(data);
    }

}
