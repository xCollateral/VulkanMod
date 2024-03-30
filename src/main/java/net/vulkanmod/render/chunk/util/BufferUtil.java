package net.vulkanmod.render.chunk.util;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class BufferUtil {

    public static ByteBuffer clone(ByteBuffer src) {
        ByteBuffer ret = MemoryUtil.memAlloc(src.remaining());
        MemoryUtil.memCopy(src, ret);
        return ret;
    }

    public static ByteBuffer bufferSlice(ByteBuffer buffer, int start, int end) {
        return MemoryUtil.memSlice(buffer, start, end - start);
    }
}
