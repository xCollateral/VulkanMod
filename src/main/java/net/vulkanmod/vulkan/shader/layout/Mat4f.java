package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

public class Mat4f extends Field {
    public Mat4f(FieldInfo info, long ptr) {
        super(info, ptr);
    }

    protected void setSupplier() {
        this.values = Uniforms.mat4f_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.values, "Field name not found: " + this.fieldInfo.name);
    }

    void update() {
        MappedBuffer src = values.get();

//        float[] floats = new float[16];
//        src.asFloatBuffer().get(floats);

        MemoryUtil.memCopy(src.buffer, MemoryUtil.memByteBuffer(this.basePtr, 64));
    }
}
