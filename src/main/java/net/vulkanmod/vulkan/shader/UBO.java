package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class UBO extends AlignedStruct {
    private final int binding;
    private final int flags;
    private ByteBuffer buffer;
    private List<Field> fields = new ArrayList<>();
    private int currentOffset = 0;
    private int size = 0;

    public UBO(int binding, int type) {
        this.binding = binding;
        this.flags = type;
        this.buffer = MemoryUtil.memAlloc(1024);
    }

    public void addField(Field field) {
        fields.add(field);
        this.size = field.getOffset() + field.getSize();
    }

    public void update() {
        for(Field field : fields) {
            field.update(buffer);
        }
        Field last = fields.get(fields.size() - 1);
        buffer.limit((last.getOffset() + last.getSize()) * Float.BYTES);
        buffer.position(0);
    }

    public int getBinding() {
        return binding;
    }

    public int getFlags() {
        return flags;
    }

    public int getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(int offset) {
        currentOffset = offset;
    }

    public int getSize() {
        return size * 4;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public FloatBuffer getFloatBuffer() {
        return buffer.asFloatBuffer();
    }

    public void resetOffset() { currentOffset = 0;}

}
