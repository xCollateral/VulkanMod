package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class Vec2f extends Field<Vector2f> {

    public Vec2f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setFunction() {
        if (this.fieldInfo.name.equals("ScreenSize")) this.set = VRenderSystem::getScreenSize;
    }

    void update() {
        Vector2f vec2 = this.set.get();

        MemoryUtil.memPutFloat(this.basePtr, vec2.x());
        MemoryUtil.memPutFloat(this.basePtr + 4, vec2.y());
    }
}
