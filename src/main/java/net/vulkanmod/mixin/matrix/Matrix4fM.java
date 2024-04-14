package net.vulkanmod.mixin.matrix;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public abstract class Matrix4fM {

    @Shadow public abstract Matrix4f perspective(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne);
    @Shadow public abstract Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne);

    @Shadow public abstract float m00();

    @Shadow public abstract float m01();

    @Shadow public abstract float m02();

    @Shadow public abstract float m03();

    @Shadow public abstract float m10();

    @Shadow public abstract float m11();

    @Shadow public abstract float m12();

    @Shadow public abstract float m13();

    @Shadow public abstract float m20();

    @Shadow public abstract float m21();

    @Shadow public abstract float m22();

    @Shadow public abstract float m23();

    @Shadow public abstract float m30();

    @Shadow public abstract float m31();

    @Shadow public abstract float m32();

    @Shadow public abstract float m33();

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
