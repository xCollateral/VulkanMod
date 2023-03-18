package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class Mat4f extends Field {
    public Mat4f(FieldInfo info, long ptr) {
        super(info, ptr);
    }

    protected void setSupplier() {
        switch (this.fieldInfo.name) {
            case "ModelViewMat" -> this.values = VRenderSystem::getModelViewMatrix;
            case "ProjMat" -> this.values = VRenderSystem::getProjectionMatrix;
            case "MVP" -> this.values = VRenderSystem::getMVP;
            case "TextureMat" -> this.values = VRenderSystem::getTextureMatrix;
        }
    }

    void update() {
        MappedBuffer src = values.get();

//        float[] floats = new float[16];
//        src.asFloatBuffer().get(floats);

        MemoryUtil.memCopy(src.buffer, MemoryUtil.memByteBuffer(this.basePtr, 64));
    }
}
