package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class UBO extends AlignedStruct {
    private final int binding;
    private final int flags;

    protected UBO(int binding, int type, int size, List<Field.FieldInfo> infoList) {
        super(infoList, size);
        this.binding = binding;
        this.flags = type;
    }

    public int getBinding() {
        return binding;
    }

    public int getFlags() {
        return flags;
    }

    public ByteBuffer getBuffer() {
        return buffer.buffer;
    }

}
