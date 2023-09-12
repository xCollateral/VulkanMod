package net.vulkanmod.mixin.matrix;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public abstract class Matrix4fM {

    @Shadow public abstract Matrix4f perspective(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne);
    @Shadow public abstract Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne);

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
        return this.ortho(left, right, bottom, top, zNear, zFar, true);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public Matrix4f perspective(float fovy, float aspect, float zNear, float zFar) {
        return this.perspective(fovy, aspect, zNear, zFar, true);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public Matrix4f setPerspective(float fovy, float aspect, float zNear, float zFar) {
        return new Matrix4f().setPerspective(fovy, aspect, zNear, zFar, true);
    }
}
