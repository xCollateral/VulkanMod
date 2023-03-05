package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec1i extends Field<Integer> {

    public Vec1i(FieldInfo fieldInfo, long ptr) {
        super(fieldInfo, ptr);
    }

    void setFunction() {
        if (this.fieldInfo.name.equals("EndPortalLayers")) this.set = () -> 15;
    }

    void update() {
        int i = this.set.get();
        MemoryUtil.memPutInt(this.basePtr, i);
    }
}
