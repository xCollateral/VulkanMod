package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vec3f extends Field {

    protected Vec3f(String name, AlignedStruct ubo) {
        super(name, 3, 4, ubo.getCurrentOffset());

        ubo.setCurrentOffset(offset + size);
        setFunction();
    }

    void setFunction() {
        switch (this.name) {
            case "Light0_Direction" -> this.set = () -> VRenderSystem.lightDirection0;
            case "Light1_Direction" -> this.set = () -> VRenderSystem.lightDirection1;
            case "ChunkOffset" -> this.set = () -> VRenderSystem.ChunkOffset;
        }
    }

    void update(FloatBuffer fb) {
//        Vector3f vec3 = (Vector3f) this.set.get();
//        float[] floats = new float[3];
//
//        floats[0] = vec3.x();
//        floats[1] = vec3.y();
//        floats[2] = vec3.z();
//
//        fb.position(offset);
//
//        for(float f : floats) {
//            fb.put(f);
//        }
    }

    void update(ByteBuffer buffer) {
        //update(buffer.asFloatBuffer());
        ByteBuffer src = (ByteBuffer) set.get();
//        buffer.position(offset * 4);
        buffer.put(offset * 4, src, 0, src.remaining());
    }
}
