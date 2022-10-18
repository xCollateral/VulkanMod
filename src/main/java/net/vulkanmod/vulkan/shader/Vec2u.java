package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.VRenderSystem;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec2u extends Field {

    protected Vec2u(String name, AlignedStruct ubo) {
        super(name, 2, 4, ubo.getCurrentOffset());

        ubo.setCurrentOffset(offset + size);
        setFunction();
    }

    void setFunction() {
        if (this.name.equals("ScreenSize")) this.set = VRenderSystem::getScreenSize;
    }

    void update(FloatBuffer fb) {
        Vector2f vec2 = (Vector2f) this.set.get();
        float[] floats = new float[2];

        floats[0] = vec2.x();
        floats[1] = vec2.y();

        fb.position(offset);

        for(float f : floats) {
            fb.put(f);
        }
    }

    void update(ByteBuffer buffer) {
        //update(buffer.asFloatBuffer());

        Vector2i vec2 = (Vector2i) this.set.get();
        int[] ints = new int[2];

        ints[0] = vec2.x();
        ints[1] = vec2.y();

        buffer.position(offset * 4);

        for(int f : ints) {
            buffer.putInt(f);
        }
    }
}
