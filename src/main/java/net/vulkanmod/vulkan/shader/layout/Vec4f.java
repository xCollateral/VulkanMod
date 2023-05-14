package net.vulkanmod.vulkan.shader.layout;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

public class Vec4f extends Field {

    public Vec4f(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setSupplier() {

        if (this.fieldInfo.name.equals("ColorModulator")) this.values = VRenderSystem::getShaderColor;
        else if (this.fieldInfo.name.equals("FogColor")) this.values = VRenderSystem::getShaderFogColor;
    }

    void update() {
        MappedBuffer buffer = this.values.get();

        MemoryUtil.memPutFloat(this.basePtr, buffer.getFloat(0));
        MemoryUtil.memPutFloat(this.basePtr + 4, buffer.getFloat(4));
        MemoryUtil.memPutFloat(this.basePtr + 8, buffer.getFloat(8));
        MemoryUtil.memPutFloat(this.basePtr + 12, buffer.getFloat(12));

    }

}
