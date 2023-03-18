package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

public class Vec2f extends Field {

    public Vec2f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        if (this.fieldInfo.name.equals("ScreenSize")) this.values = VRenderSystem::getScreenSize;
    }

    void update() {
        MappedBuffer buffer = this.values.get();

        MemoryUtil.memPutFloat(this.basePtr, buffer.getFloat(0));
        MemoryUtil.memPutFloat(this.basePtr + 4, buffer.getFloat(4));
    }
}
