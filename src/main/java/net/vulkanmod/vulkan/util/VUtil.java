package net.vulkanmod.vulkan.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;

import static org.lwjgl.system.MemoryStack.stackGet;

public class VUtil {
    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    public static final Unsafe UNSAFE;

    static {
        Field f = null;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static PointerBuffer asPointerBuffer(Collection<String> collection) {

        MemoryStack stack = stackGet();

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    public static void memcpy(ByteBuffer buffer, short[] indices) {

        for(short index : indices) {
            buffer.putShort(index);
        }

        buffer.rewind();
    }

    public static void memcpy(ByteBuffer buffer, short[] indices, long offset) {
        buffer.position((int) offset);

        for(short index : indices) {
            buffer.putShort(index);
        }

        buffer.rewind();
    }

    public static void memcpy(ByteBuffer src, ByteBuffer dst) {
        MemoryUtil.memCopy(src, dst);
        src.limit(src.capacity()).rewind();
    }

    public static void memcpy(ByteBuffer src, long dstPtr) {
        MemoryUtil.memCopy(MemoryUtil.memAddress0(src), dstPtr, src.capacity());
    }

    public static void memcpy(ByteBuffer src, ByteBuffer dst, long offset) {
        dst.position((int)offset);

        MemoryUtil.memCopy(src, dst);
        src.limit(src.capacity()).rewind();
    }

    public static void memcpy(ByteBuffer src, ByteBuffer dst, int size, long offset) {
        dst.position((int)offset);
        src.limit(size);

        MemoryUtil.memCopy(src, dst);
        src.limit(src.capacity()).rewind();
    }

    public static void memcpyImage(ByteBuffer dst, ByteBuffer src, int width, int height, int channels, int unpackSkipRows, int unpackSkipPixels, int unpackRowLenght) {
        int offset = (unpackSkipRows * unpackRowLenght + unpackSkipPixels) * channels;
        for (int i = 0; i < height; ++i) {
            src.limit(offset + width * channels);
            src.position(offset);
            dst.put(src);
            offset += unpackRowLenght * channels;
        }
    }

    public static void memcpy(ByteBuffer buffer, FloatBuffer floatBuffer) {
        while(floatBuffer.hasRemaining()) {
            float f = floatBuffer.get();
            buffer.putFloat(f);
        }
        floatBuffer.position(0);
    }

    public static void memcpy(ByteBuffer buffer, FloatBuffer floatBuffer, long offset) {
        buffer.position((int) offset);
        while(floatBuffer.hasRemaining()) {
            float f = floatBuffer.get();
            buffer.putFloat(f);
        }
        floatBuffer.position(0);
    }

    public static int align(int x, int align) {
        int r = x % align;
        return r == 0 ? x : x + align - r;
    }

}
