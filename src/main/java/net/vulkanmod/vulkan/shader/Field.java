package net.vulkanmod.vulkan.shader;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.function.Supplier;

public abstract class Field {
    protected int offset;
    protected final int align;
    protected int size;
    protected int type;
    protected String name;
    protected FloatBuffer fieldBuffer;
    protected Supplier<Object> set;

    protected Field(String name, int size, int align, int offset) {
        this.name = name;
        this.size = size;
        this.align = align;

        this.offset = offset + ((align - (offset % align)) % align);
    }

    protected Field(String name, int size, int align, AlignedStruct struct) {
        this.name = name;
        this.size = size;
        this.align = align;

        int offset = struct.currentOffset;
        this.offset = offset + ((align - (offset % align)) % align);
        struct.setCurrentOffset(this.offset);
    }

    abstract void setFunction();

    abstract void update(FloatBuffer fb);

    abstract void update(ByteBuffer buffer);

    public static Field createField(String type, String name, int count, AlignedStruct ubo) {
        if(type.equals("matrix4x4")) {
            return new Mat4f(name, ubo);
        }
        else if(type.equals("float")) {
            if (count == 4) return new Vec4f(name, ubo);
            else if (count == 3) return new Vec3f(name,ubo);
            else return new Vec1f(name, ubo);
        }
        else if(type.equals("int")) {
          if (count == 2) return new Vec2u(name,ubo);
          else return new Vec1i(name, ubo);
        }
        else {
            throw new RuntimeException("not admitted type..");
        }
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() { return size; }
}
