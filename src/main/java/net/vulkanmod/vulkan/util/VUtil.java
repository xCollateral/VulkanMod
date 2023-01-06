package net.vulkanmod.vulkan.util;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.libc.LibCString.nmemcpy;

public class VUtil {
    public static final long alignedINT32_T = Integer.toUnsignedLong(Integer.MIN_VALUE);
    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    public static final long tmOut = 10000;

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

    public static void memcpy(ByteBuffer dst, ByteBuffer src) {
        //src.limit((int)size);
//        dst.put(src);

        MemoryUtil.memCopy(src, dst);
        src.limit(src.capacity()).rewind();
    }
    public static void memcpy2(long dst, long src, long offset, long size) {
//        dst.position((int)offset);
//        dst.put(src);

        MemoryUtil.memCopy((src), (dst)+offset, size);
//        src.limit(src.capacity()).rewind();
    }
    public static void memcpy(ByteBuffer dst, ByteBuffer src, long offset) {
        dst.position((int)offset);
//        dst.put(src);

        MemoryUtil.memCopy(src, dst);
        src.limit(src.capacity()).rewind();
    }
    public static void memcpy2(ByteBuffer dst, long src, long offset, int size) {
        dst.position((int)offset);

        //Almost Always above 384 bytes if Draw Commands exceed 19 VBOs or more
        nmemcpy(MemoryUtil.memAddress(dst), src, size);


    }

    public static void memcpy(ByteBuffer dst, ByteBuffer src, int size, long offset) {
        dst.position((int)offset);
        src.limit(size);
//        dst.put(src);

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

    public static int align(int num, int align) {
        int r = num % align;
        return r == 0 ? num : num + align - r;
    }

    public static Matrix4f convert(com.mojang.math.Matrix4f src) {
        try(MemoryStack stack = stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            src.store(fb);
            Matrix4f dst = new Matrix4f(fb);
            return dst;
        }
    }

    public static int packColor(float r, float g, float b, float a) {
        int color = 0;
        color += (int)(a * 255) << 24;
        color += (int)(r * 255) << 16;
        color += (int)(g * 255) << 8;
        color += (int)(b * 255);

        return color;
    }
}
