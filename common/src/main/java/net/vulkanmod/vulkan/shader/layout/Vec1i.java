package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1i extends Field {
    private Supplier<Integer> intSupplier;

    public Vec1i(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        this.intSupplier = Uniforms.vec1i_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.intSupplier, "Field name not found: " + this.fieldInfo.name);
    }

    void update() {
        if(this.intSupplier != null) {
            int i = this.intSupplier.get();
            MemoryUtil.memPutInt(this.basePtr, i);
        }
        else {
            MappedBuffer buffer = this.values.get();

            MemoryUtil.memPutInt(this.basePtr, buffer.getInt(0));
        }
    }

    void update(long ptr) {
        if(this.intSupplier != null) {
            int i = this.intSupplier.get();
            MemoryUtil.memPutInt(ptr + this.offset, i);
        }
        else {
            //TODO
        }
    }
}
