package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PushConstant extends AlignedStruct {
    private ByteBuffer buffer;
    private Field fields;
    private int size;

    public void allocateBuffer() {
        buffer = MemoryUtil.memAlignedAlloc(Pointer.POINTER_SIZE, size);
    }

    public void update() {
//        for(Field field : fields) {
            fields.update(buffer);
//        }

        buffer.rewind();
    }

    public void addField(Field field) {
        fields=(field);
        this.size = (field.getOffset() + field.getSize()) * 4;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int getSize() { return size; }
}
