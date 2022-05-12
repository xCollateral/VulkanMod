package net.vulkanmod.vulkan.shader;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec1i extends Field {
    protected Vec1i(String name, AlignedStruct struct) {
        super(name, 1, 1, struct.getCurrentOffset());

        struct.setCurrentOffset(offset + size);
        setFunction();
    }

    void setFunction() {
        if (this.name.equals("EndPortalLayers")) this.set = () -> 15;
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
