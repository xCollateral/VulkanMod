package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1f extends Uniform {
    private Supplier<Float> floatSupplier;

    public Vec1f(Info info) {
        super(info);
    }

    void setSupplier() {
        this.floatSupplier = Uniforms.vec1f_uniformMap.get(this.info.name);
    }

    @Override
    public void setSupplier(Supplier<MappedBuffer> supplier) {
        this.floatSupplier = () -> supplier.get().getFloat(0);
    }

    void update(long ptr) {
        float f = this.floatSupplier.get();
        MemoryUtil.memPutFloat(ptr + this.offset, f);
    }
}
