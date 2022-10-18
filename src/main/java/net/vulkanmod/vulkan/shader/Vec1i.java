package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec1i extends Field {
    protected Vec1i(String name, AlignedStruct struct) {
        super(name, 1, 1, struct.getCurrentOffset());

        struct.setCurrentOffset(offset + size);
        setFunction();
    }

    void setFunction() {
        switch (this.name) {
            case "EndPortalLayers" -> this.set = () -> 15;
            case "LineWidth" -> this.set = () -> (int)RenderSystem.getShaderLineWidth();

        }
    }

    void update(FloatBuffer fb) {
//        float f = (float) this.set.get();
//        fb.position(offset);
//        fb.put(f);
    }

    void update(ByteBuffer buffer) {
        //update(buffer.asFloatBuffer());

        int f = (int) this.set.get();
        buffer.position(offset * 4);
        buffer.putInt(f);
    }
}
