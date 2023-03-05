package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec4f extends Field<float[]> {

    public Vec4f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setFunction() {

        if (this.fieldInfo.name.equals("ColorModulator")) this.set = RenderSystem::getShaderColor;
        else if (this.fieldInfo.name.equals("FogColor")) this.set = RenderSystem::getShaderFogColor;
    }

    void update() {
        float[] floats = this.set.get();

        MemoryUtil.memPutFloat(this.basePtr, floats[0]);
        MemoryUtil.memPutFloat(this.basePtr + 4, floats[1]);
        MemoryUtil.memPutFloat(this.basePtr + 8, floats[2]);
        MemoryUtil.memPutFloat(this.basePtr + 12, floats[3]);

    }

}
