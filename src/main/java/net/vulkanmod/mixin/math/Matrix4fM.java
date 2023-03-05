package net.vulkanmod.mixin.math;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Matrix4f.class)
public class Matrix4fM {

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public Matrix4f setOrtho(float left, float right, float bottom, float top, float zNear, float zFar) {
        return new Matrix4f().setOrtho(left, right, bottom, top, zNear, zFar, true);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar) {
        return new Matrix4f().ortho(left, right, bottom, top, zNear, zFar, true);
    }
}
