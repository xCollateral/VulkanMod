package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class Mat4f extends Field<ByteBuffer> {
    public Mat4f(FieldInfo info, long ptr) {
        super(info, ptr);
    }

    protected void setFunction() {
        switch (this.fieldInfo.name) {
            case "ModelViewMat" -> this.set = VRenderSystem::getModelViewMatrix;
            case "ProjMat" -> this.set = VRenderSystem::getProjectionMatrix;
            case "MVP" -> this.set = VRenderSystem::getMVP;
            case "TextureMat" -> this.set = VRenderSystem::getTextureMatrix;
        }
    }

    void update() {
        ByteBuffer src = set.get();

//        float[] floats = new float[16];
//        src.asFloatBuffer().get(floats);

        MemoryUtil.memCopy(src, MemoryUtil.memByteBuffer(this.basePtr, 64));
    }
}
