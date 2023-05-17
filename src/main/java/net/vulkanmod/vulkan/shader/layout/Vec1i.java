package net.vulkanmod.vulkan.shader.layout;

import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1i extends Field {
    private Supplier<Integer> intSupplier;

    public Vec1i(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        if (this.fieldInfo.name.equals("EndPortalLayers")) this.intSupplier = () -> 15;
    }

    void update() {
        int i = this.intSupplier.get();
        MemoryUtil.memPutInt(this.basePtr, i);
    }
}
