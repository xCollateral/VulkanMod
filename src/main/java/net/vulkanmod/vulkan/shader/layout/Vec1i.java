package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.ScalarUniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1i extends Uniform {
    private Supplier<Integer> intSupplier;

    public Vec1i(Info info) {
        super(info);
    }

    void setSupplier() {
        this.intSupplier = ScalarUniforms.vec1i_uniformMap.get(this.info.name);
    }

    @Override
    public void setSupplier(Supplier<MappedBuffer> supplier) {
        this.intSupplier = () -> supplier.get().getInt(0);
    }

    void update(long ptr) {
        int i = this.intSupplier.get();
        MemoryUtil.memPutInt(ptr + this.offset, i);
    }
}
