package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

public class Mat3 extends Uniform {

    Mat3(Info info) {
        super(info);
    }

    void update(long ptr) {
        MappedBuffer src = values.get();

        MemoryUtil.memCopy(src.ptr + 0, ptr + this.offset + 0, 12);
        MemoryUtil.memCopy(src.ptr + 12, ptr + this.offset + 16, 12);
        MemoryUtil.memCopy(src.ptr + 24, ptr + this.offset + 32, 12);
    }
}
