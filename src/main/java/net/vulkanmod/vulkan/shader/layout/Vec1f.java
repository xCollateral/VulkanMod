package net.vulkanmod.vulkan.shader.layout;

import com.mojang.blaze3d.systems.RenderSystem;
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
        }
    }

    void update() {
        float f = this.floatSupplier.get();
        MemoryUtil.memPutFloat(this.basePtr, f);
    }
}
