package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.util.MappedBuffer;
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
