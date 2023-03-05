package net.vulkanmod.vulkan.shader;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public abstract class Field<T> {
    protected Supplier<T> set;

    FieldInfo fieldInfo;
    protected long basePtr;

    Field(FieldInfo fieldInfo, long ptr) {
        this.fieldInfo = fieldInfo;
        this.basePtr = ptr + (fieldInfo.offset * 4L);
        this.setFunction();
    }

    abstract void setFunction();

    void update(ByteBuffer buffer) {}

    abstract void update();

    public static Field<?> createField(FieldInfo info, long ptr) {
        return switch (info.type) {
            case "matrix4x4" -> new Mat4f(info, ptr);
            case "float" -> switch (info.size) {
                case 4 -> new Vec4f(info, ptr);
                case 3 -> new Vec3f(info, ptr);
                case 2 -> new Vec2f(info, ptr);
                case 1 -> new Vec1f(info, ptr);

                default -> throw new IllegalStateException("Unexpected value: " + info.size);
            };
            case "int" -> new Vec1i(info, ptr);
            default -> throw new RuntimeException("not admitted type..");
        };
    }

    public int getOffset() {
        return fieldInfo.offset;
    }

    public int getSize() { return fieldInfo.size; }

    public static FieldInfo createFieldInfo(String type, String name, int count) {
        return switch (type) {
            case "matrix4x4" -> new FieldInfo(type, name, 4, 16);
            case "float" -> switch (count) {
                case 4 -> new FieldInfo(type, name, 4, 4);
                case 3 -> new FieldInfo(type, name, 4, 3);
                case 2 -> new FieldInfo(type, name, 4, 2);
                case 1 -> new FieldInfo(type, name, 1, 1);

                default -> throw new IllegalStateException("Unexpected value: " + count);
            };
            case "int" -> new FieldInfo(type, name, 1, 1);
            default -> throw new RuntimeException("not admitted type..");
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
