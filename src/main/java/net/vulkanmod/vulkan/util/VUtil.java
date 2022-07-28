package net.vulkanmod.vulkan.util;

import net.vulkanmod.vulkan.Vulkan;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.vmaMapMemory;
import static org.lwjgl.util.vma.Vma.vmaUnmapMemory;

public class VUtil {
    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    public static void memcpy(ByteBuffer buffer, short[] indices) {

        for(short index : indices) {
            buffer.putShort(index);
        }

        buffer.rewind();
    }

    public static void memcpy(ByteBuffer buffer, short[] indices, int offset) {
        buffer.position(offset);

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

    public static void memcpy(ByteBuffer dst, ByteBuffer src, int offset) {
        dst.position(offset);
//        dst.put(src);

        MemoryUtil.memCopy(src, dst);
        src.mark().rewind();
    }

    public static void memcpy(ByteBuffer dst, ByteBuffer src, int size, int offset) {
        dst.position(offset);
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

    public static void memcpy(ByteBuffer buffer, FloatBuffer floatBuffer, int offset) {
        buffer.position(offset);
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

    public static Matrix4f convert(net.minecraft.util.math.Matrix4f src) {
        try(MemoryStack stack = stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            src.writeColumnMajor(fb);
            Matrix4f dst = new Matrix4f(fb);
            return dst;
        }
    }
}
