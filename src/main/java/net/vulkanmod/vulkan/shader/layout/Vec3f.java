package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;

public class Vec3f extends Field {

    public Vec3f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        switch (this.fieldInfo.name) {
            case "Light0_Direction" -> this.values = () -> VRenderSystem.lightDirection0;
            case "Light1_Direction" -> this.values = () -> VRenderSystem.lightDirection1;
            case "ChunkOffset" -> this.values = () -> VRenderSystem.ChunkOffset;
        }
    }

    void update() {
        MappedBuffer src = this.values.get();

        VUtil.UNSAFE.putFloat(this.basePtr, VUtil.UNSAFE.getFloat(src.ptr));
        VUtil.UNSAFE.putFloat(this.basePtr + 4, VUtil.UNSAFE.getFloat(src.ptr + 4));
        VUtil.UNSAFE.putFloat(this.basePtr + 8, VUtil.UNSAFE.getFloat(src.ptr + 8));
    }
}
