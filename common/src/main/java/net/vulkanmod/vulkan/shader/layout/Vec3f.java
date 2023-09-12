package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.apache.commons.lang3.Validate;

public class Vec3f extends Field {

    public Vec3f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        this.values = Uniforms.vec3f_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.values, "Field name not found: " + this.fieldInfo.name);
    }

    void update() {
        MappedBuffer src = this.values.get();

        VUtil.UNSAFE.putFloat(this.basePtr, VUtil.UNSAFE.getFloat(src.ptr));
        VUtil.UNSAFE.putFloat(this.basePtr + 4, VUtil.UNSAFE.getFloat(src.ptr + 4));
        VUtil.UNSAFE.putFloat(this.basePtr + 8, VUtil.UNSAFE.getFloat(src.ptr + 8));
    }
}
