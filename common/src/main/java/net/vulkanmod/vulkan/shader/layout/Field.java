package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public abstract class Field {
    protected Supplier<MappedBuffer> values;

    FieldInfo fieldInfo;
    protected long basePtr;
    protected long offset;
    protected int size;

    Field(FieldInfo fieldInfo, long ptr) {
        this.fieldInfo = fieldInfo;
        this.basePtr = ptr + (fieldInfo.offset * 4L);
        this.offset = fieldInfo.offset * 4L;
        this.size = fieldInfo.size * 4;
        this.setSupplier();
    }

    abstract void setSupplier();

    public void setSupplier(Supplier<MappedBuffer> supplier) {
        this.values = supplier;
    }

    public String getName() {
        return this.fieldInfo.name;
    }

    abstract void update();

    void update(long ptr) {
        MappedBuffer src = values.get();

        MemoryUtil.memCopy(src.ptr, ptr + this.offset, this.size);
    }

    public static Field createField(FieldInfo info, long ptr) {
        return switch (info.type) {
            case "mat4" -> new Mat4f(info, ptr);
            case "vec4" -> new Vec4f(info, ptr);
            case "vec3" -> new Vec3f(info, ptr);
            case "vec2" -> new Vec2f(info, ptr);
            case "float" -> new Vec1f(info, ptr);
            case "int" -> new Vec1i(info, ptr);
            default -> throw new RuntimeException("not admitted type: " + info.type);
        };
    }

    public int getOffset() {
        return fieldInfo.offset;
    }

    public int getSize() { return fieldInfo.size; }

    //TODO
    public static FieldInfo createFieldInfo(String type, String name, int count) {
        return switch (type) {
            case "matrix4x4" -> new FieldInfo("mat4", name, 4, 16);
            case "float" -> switch (count) {
                case 4 -> new FieldInfo("vec4", name, 4, 4);
                case 3 -> new FieldInfo("vec3", name, 4, 3);
                case 2 -> new FieldInfo("vec2", name, 4, 2);
                case 1 -> new FieldInfo("float", name, 1, 1);

                default -> throw new IllegalStateException("Unexpected value: " + count);
            };
            case "int" -> new FieldInfo("int", name, 1, 1);
            default -> throw new RuntimeException("not admitted type..");
        };
    }

    public static FieldInfo createFieldInfo(String type, String name) {
        return switch (type) {
            case "mat4" -> new FieldInfo(type, name, 4, 16);
            case "mat3" -> new FieldInfo(type, name, 4, 9);

            case "vec4" -> new FieldInfo(type, name, 4, 4);
            case "vec3" -> new FieldInfo(type, name, 4, 3);
            case "vec2" -> new FieldInfo(type, name, 4, 2);

            case "float", "int" -> new FieldInfo(type, name, 1, 1);

            default -> throw new RuntimeException("not admitted type: " + type);
        };
    }

    public static class FieldInfo {
        final String type;
        final String name;
        final int align;
        final int size;
        int offset;

        FieldInfo(String type, String name, int align, int size) {
            this.type = type;
            this.name = name;
            this.align = align;
            this.size = size;
        }

        int getSizeBytes() { return 4 * this.size; }

        int computeAlignmentOffset(int builderOffset) {
            return this.offset = builderOffset + ((align - (builderOffset % align)) % align);
        }
    }
}
