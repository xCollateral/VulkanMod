package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec3f extends Field<ByteBuffer> {

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
        ByteBuffer src = this.set.get();

        FloatBuffer fb = src.asFloatBuffer();

        VUtil.UNSAFE.putFloat(this.basePtr, fb.get(0));
        VUtil.UNSAFE.putFloat(this.basePtr + 4, fb.get(1));
        VUtil.UNSAFE.putFloat(this.basePtr + 8, fb.get(2));
    }
}
