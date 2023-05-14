package net.vulkanmod.vulkan.shader.layout;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1f extends Field {
    private Supplier<Float> floatSupplier;

    public Vec1f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {
        switch (this.fieldInfo.name) {
            case "FogStart" -> this.floatSupplier = RenderSystem::getShaderFogStart;
            case "FogEnd" -> this.floatSupplier = RenderSystem::getShaderFogEnd;
            case "LineWidth" -> this.floatSupplier = RenderSystem::getShaderLineWidth;
            case "GameTime" -> this.floatSupplier = RenderSystem::getShaderGameTime;
            case "AlphaCutout" -> this.floatSupplier = () -> VRenderSystem.alphaCutout;
//            default -> throw new IllegalArgumentException("Unexpected value: " + this.fieldInfo.name);
        }
    }

    void update() {
        if(this.floatSupplier != null) {
            float f = this.floatSupplier.get();
            MemoryUtil.memPutFloat(this.basePtr, f);
        }
        else {
            //TODO
//            MappedBuffer buffer = this.values.get();
//
//            MemoryUtil.memPutFloat(this.basePtr, buffer.getFloat(0));
        }
    }

    void update(long ptr) {
        if(this.floatSupplier != null) {
            float f = this.floatSupplier.get();
            MemoryUtil.memPutFloat(ptr + this.offset, f);
        }
        else {
            //TODO
        }
    }
}
