package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1f extends Field {
    private Supplier<Float> floatSupplier;

    public Vec1f(FieldInfo fieldInfo) {
        super(fieldInfo);
    }

    void setSupplier() {
        this.floatSupplier = Uniforms.vec1f_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.floatSupplier, "Field name not found: " + this.fieldInfo.name);
    }

    void update(long ptr) {
        float f = this.floatSupplier.get();
        MemoryUtil.memPutFloat(ptr + this.offset, f);
    }
}
