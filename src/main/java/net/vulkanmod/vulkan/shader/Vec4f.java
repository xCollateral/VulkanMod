package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec4f extends Field {

    protected Vec4f(String name, AlignedStruct ubo) {
        super(name, 4, 4, ubo.getCurrentOffset());

        ubo.setCurrentOffset(offset + size);
        setFunction();
    }

    void setFunction() {
        if (this.name.equals("ColorModulator")) this.set = RenderSystem::getShaderColor;
        else if (this.name.equals("FogColor")) this.set = RenderSystem::getShaderFogColor;
    }

    void update(FloatBuffer fb) {
        float[] floats = (float[]) this.set.get();
        fb.position(offset);

        for(float f : floats) {
            fb.put(f);
        }
    }

    void update(ByteBuffer buffer) {
        //update(buffer.asFloatBuffer());

        float[] floats = (float[]) this.set.get();
        buffer.position(offset * 4);

        for(float f : floats) {
            buffer.putFloat(f);
        }

//        ByteBuffer src = (ByteBuffer) set.get();
//        buffer.position(offset * 4);
//        MemoryUtil.memCopy(src, buffer);
    }
}
