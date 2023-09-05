package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1f extends Field {
    private Supplier<Float> floatSupplier;

    public Vec1f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        this.floatSupplier = Uniforms.vec1f_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.floatSupplier, "Field name not found: " + this.fieldInfo.name);
    }

    void update() {
        if(this.floatSupplier != null) {
            float f = this.floatSupplier.get();
            MemoryUtil.memPutFloat(this.basePtr, f);
        }
        else {
            //TODO
//            MappedBuffer buffer = this.values.get();
//
//            MemoryUtil.memPutFloat(this.basePtr, buffer.getFloat(0));
        }
    }

    void update(long ptr) {
        if(this.floatSupplier != null) {
            float f = this.floatSupplier.get();
            MemoryUtil.memPutFloat(ptr + this.offset, f);
        }
        else {
            //TODO
        }
    }
}
