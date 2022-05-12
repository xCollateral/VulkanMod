package net.vulkanmod.vulkan.shader;

import net.minecraft.util.math.Matrix4f;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Mat4f extends Field {
    //private Supplier<Matrix4f> set;

    protected Mat4f(String name, AlignedStruct ubo) {
        super(name, 16, 4, ubo.getCurrentOffset());

        ubo.setCurrentOffset(offset + size);
        setFunction();
    }

    protected void setFunction() {
        switch (this.name) {
            case "ModelViewMat" -> this.set = VRenderSystem::getModelViewMatrix;
            case "ProjMat" -> this.set = VRenderSystem::getProjectionMatrix;
            case "MVP" -> this.set = VRenderSystem::getMVP;
            case "TextureMat" -> this.set = VRenderSystem::getTextureMatrix;
        }
    }

    public void update(FloatBuffer fb) {
        Matrix4f mat4 = (Matrix4f) set.get();
        FloatBuffer temp = MemoryUtil.memAllocFloat(16);
        mat4.writeColumnMajor(temp);
        fb.position(offset);
        fb.put(temp);
        MemoryUtil.memFree(temp);

    }

    public void update(ByteBuffer buffer) {
        ByteBuffer src = (ByteBuffer) set.get();

        float[] floats = new float[16];
        src.asFloatBuffer().get(floats);

        buffer.position(offset << 2);
        MemoryUtil.memCopy(src, buffer);
        //buffer.position(offset + 64);

    }
}
