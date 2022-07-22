package net.vulkanmod.mixin.util;

import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.FloatBuffer;

import static org.joml.Math.fma;
import static org.lwjgl.system.MemoryStack.stackGet;

@Mixin(Matrix4f.class)
public class Matrix4fM {
    private static final double v = 0.008726646259971648D;
    private static final FloatBuffer fb = stackGet().mallocFloat(16);

    /**
     * @author
     */
    @Overwrite
    public static Matrix4f viewboxMatrix(double fovy, float aspect, float zNear, float zFar) {
        float f = (float)(1.0D / Math.tan((fovy* v)));
        Matrix4f matrix4f = new Matrix4f();

            org.joml.Matrix4f mat4f = new org.joml.Matrix4f();
//            mat4f.setPerspective((float) fovy, aspect, zNear, zFar, true);


            mat4f.m00(f / aspect);
            mat4f.m11(f);
            mat4f.m22((zFar) / (zNear - zFar));
            mat4f.m32(-1.0F);
            mat4f.m23(zFar * zNear / (zNear - zFar));
            mat4f.m33(0.0F);


            mat4f.get(fb);
//            matrix4f.readColumnMajor(fb);
            matrix4f.readRowMajor(fb);



//        matrix4f.a00 = f / aspect;
//        matrix4f.readColumnMajor();
//        matrix4f.m11 = f;
//        //matrix4f.m22 = (zFar + zNear) / (zNear - zFar);
//        matrix4f.m22 = (zFar) / (zNear - zFar);
//        matrix4f.m32 = -1.0F;
//        //matrix4f.m23 = 2.0F * zFar * zNear / (zNear - zFar);
//        matrix4f.m23 = zFar * zNear / (zNear - zFar);

        return matrix4f;
    }

    /**
     * @author
     */
    @Overwrite
    public static Matrix4f projectionMatrix(float left, float right, float bottom, float top, float nearPlane, float farPlane) {
        Matrix4f matrix4f = new Matrix4f();

        try(MemoryStack stack = MemoryStack.stackPush()) {
            org.joml.Matrix4f mat4f = new org.joml.Matrix4f();

//            mat4f.setOrtho(left, right, bottom, top, nearPlane, farPlane, true);
            FloatBuffer fb = stack.mallocFloat(16);

            float f = right - left;
            float f1 = bottom - top;
            float f2 = farPlane - nearPlane;
            mat4f.m00(2.0F / f);
            mat4f.m11(2.0F / f1);
            mat4f.m22(-1.0F / f2);
            mat4f.m03(-(right + left) / f);
            mat4f.m13(-(bottom + top) / f1);
            mat4f.m23(-(nearPlane) / f2);
            mat4f.m33(1.0F);

            mat4f.get(fb);
//            matrix4f.readColumnMajor(fb);
            matrix4f.readRowMajor(fb);
        }


//        float f = right - left;
//        float f1 = bottom - top;
//        float f2 = zFar - zNear;
//        matrix4f.m00 = 2.0F / f;
//        matrix4f.m11 = 2.0F / f1;
//        matrix4f.m22 = -1.0F / f2; //-2.0f
//        matrix4f.m03 = -(right + left) / f;
//        matrix4f.m13 = -(bottom + top) / f1;
//        matrix4f.m23 = -(zNear) / f2; //zFar + ZNear
//        matrix4f.m33 = 1.0F;
        return matrix4f;
    }
}
