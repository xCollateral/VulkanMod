package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec3f extends Field<MappedBuffer> {

    public Vec3f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setFunction() {
        switch (this.fieldInfo.name) {
            case "Light0_Direction" -> this.set = () -> VRenderSystem.lightDirection0;
            case "Light1_Direction" -> this.set = () -> VRenderSystem.lightDirection1;
            case "ChunkOffset" -> this.set = () -> VRenderSystem.ChunkOffset;
        }
    }

    void update() {
        MappedBuffer src = this.set.get();

        VUtil.UNSAFE.putFloat(this.basePtr, VUtil.UNSAFE.getFloat(src.ptr));
        VUtil.UNSAFE.putFloat(this.basePtr + 4, VUtil.UNSAFE.getFloat(src.ptr + 4));
        VUtil.UNSAFE.putFloat(this.basePtr + 8, VUtil.UNSAFE.getFloat(src.ptr + 8));
    }
}
