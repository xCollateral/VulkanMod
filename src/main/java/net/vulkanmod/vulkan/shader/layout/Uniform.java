package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Uniform {
    protected Supplier<MappedBuffer> values;

    Info info;
    protected long offset;
    protected int size;

    Uniform(Info info) {
        this.info = info;
        this.offset = info.offset * 4L;
        this.size = info.size * 4;
        this.setSupplier();
    }

    void setSupplier() {
        this.values = switch (info.type) {
            case "mat4" -> Uniforms.mat4f_uniformMap.get(info.name);
            case "vec4" -> Uniforms.vec4f_uniformMap.get(info.name);
            case "vec3" -> Uniforms.vec3f_uniformMap.get(info.name);
            case "vec2" -> Uniforms.vec2f_uniformMap.get(info.name);

            default -> null;
        };
    }

    public void setSupplier(Supplier<MappedBuffer> supplier) {
        this.values = supplier;
    }

    public String getName() {
        return this.info.name;
    }

    void update(long ptr) {
        MappedBuffer src = values.get();

        MemoryUtil.memCopy(src.ptr, ptr + this.offset, this.size);
    }

    public static Uniform createField(Info info) {
        return switch (info.type) {
            case "mat4", "vec3", "vec4", "vec2" -> new Uniform(info);
            case "mat3" -> new Mat3(info);
            case "float" -> new Vec1f(info);
            case "int" -> new Vec1i(info);
            default -> throw new RuntimeException("not admitted type: " + info.type);
        };
    }

    public int getOffset() {
        return info.offset;
    }

    public int getSize() { return info.size; }

    public String toString() {
        return String.format("%s: %s offset: %d", info.type, info.name, info.offset);
    }

    //TODO
    public static Info createUniformInfo(String type, String name, int count) {
        return switch (type) {
            case "matrix4x4" -> new Info("mat4", name, 4, 16);
            case "float" -> switch (count) {
                case 4 -> new Info("vec4", name, 4, 4);
                case 3 -> new Info("vec3", name, 4, 3);
                case 2 -> new Info("vec2", name, 2, 2);
                case 1 -> new Info("float", name, 1, 1);

                default -> throw new IllegalStateException("Unexpected value: " + count);
            };
            case "int" -> new Info("int", name, 1, 1);
            default -> throw new RuntimeException("not admitted type..");
        };
    }

    public static Info createUniformInfo(String type, String name) {
        return switch (type) {
            case "mat4" -> new Info(type, name, 4, 16);
            case "mat3" -> new Info(type, name, 4, 9);

            case "vec4" -> new Info(type, name, 4, 4);
            case "vec3" -> new Info(type, name, 4, 3);
            case "vec2" -> new Info(type, name, 2, 2);

            case "float", "int" -> new Info(type, name, 1, 1);

            default -> throw new RuntimeException("not admitted type: " + type);
        };
    }

    public static class Info {
        final String type;
        final String name;
        final int align;
        final int size;
        int offset;

        Info(String type, String name, int align, int size) {
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
