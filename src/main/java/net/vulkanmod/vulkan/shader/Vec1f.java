package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec1f extends Field<Float> {

    public Vec1f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setFunction() {
        switch (this.fieldInfo.name) {
            case "FogStart" -> this.set = RenderSystem::getShaderFogStart;
            case "FogEnd" -> this.set = RenderSystem::getShaderFogEnd;
            case "LineWidth" -> this.set = RenderSystem::getShaderLineWidth;
            case "GameTime" -> this.set = RenderSystem::getShaderGameTime;
        }
    }

    void update() {
        float f = this.set.get();
        MemoryUtil.memPutFloat(this.basePtr, f);
    }
}
