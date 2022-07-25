package net.vulkanmod.mixin.util;

import net.minecraft.util.math.Matrix4f;
import org.joml.Math;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.FloatBuffer;

import static org.joml.Math.fma;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryUtil.memGetFloat;

@Mixin(Matrix4f.class)
public class Matrix4fM {
    @Shadow protected float a00,a01,a11,a21,a31,a10,a02,a12,a22,a32,a20,a03,a13,a23,a33,a30;


    private static final double v = 0.008726646259971648D;
    private static final FloatBuffer fb = stackGet().mallocFloat(16);
    private static final long ad = MemoryUtil.memAddress0(fb);

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

    @Overwrite
    public float determinantAndAdjugate() {
        float f = fma(this.a00, this.a11, - this.a01 * this.a10);
        float g = fma(this.a00, this.a12, - this.a02 * this.a10);
        float h = fma(this.a00, this.a13, - this.a03 * this.a10);
        float i = fma(this.a01, this.a12, - this.a02 * this.a11);
        float j = fma(this.a01, this.a13, - this.a03 * this.a11);
        float k = fma(this.a02, this.a13, - this.a03 * this.a12);
        float l = fma(this.a20, this.a31, - this.a21 * this.a30);
        float m = fma(this.a20, this.a32, - this.a22 * this.a30);
        float n = fma(this.a20, this.a33, - this.a23 * this.a30);
        float o = fma(this.a21, this.a32, - this.a22 * this.a31);
        float p = fma(this.a21, this.a33, - this.a23 * this.a31);
        float q = fma(this.a22, this.a33, - this.a23 * this.a32);
        float r = fma(this.a11, q, -fma(this.a12, p, this.a13 * o));
        float s = -fma(this.a10, q, fma(this.a12, n, - this.a13 * m));
        float t = fma(this.a10, p, -fma(this.a11, n, this.a13 * l));
        float u = -fma(this.a10, o, fma(this.a11, m, - this.a12 * l));
        float v = -fma(this.a01, q, fma(this.a02, p, - this.a03 * o));
        float w = fma(this.a00, q, - fma(this.a02, n, this.a03 * m));
        float x = -fma(this.a00, p,fma(this.a01, n, - this.a03 * l));
        float y = fma(this.a00, o, - fma(this.a01, m, this.a02 * l));
        float z = fma(this.a31, k, - fma(this.a32, j, this.a33 * i));
        float aa = -fma(this.a30, k, fma(this.a32,h, - this.a33 * g));
        float ab = fma(this.a30, j, - fma(this.a31,h, this.a33 * f));
        float ac = -fma(this.a30, i, fma(this.a31,g, - this.a32 * f));
        float ad = -fma(this.a21, k, fma(this.a22,j, - this.a23 * i));
        float ae = fma(this.a20, k, - fma(this.a22,h, this.a23 * g));
        float af = -fma(this.a20, j, fma(this.a21,h, - this.a23 * f));
        float ag = fma(this.a20, i, - fma(this.a21,g, this.a22 * f));
        this.a00 = r;
        this.a10 = s;
        this.a20 = t;
        this.a30 = u;
        this.a01 = v;
        this.a11 = w;
        this.a21 = x;
        this.a31 = y;
        this.a02 = z;
        this.a12 = aa;
        this.a22 = ab;
        this.a32 = ac;
        this.a03 = ad;
        this.a13 = ae;
        this.a23 = af;
        this.a33 = ag;
        return fma(f, q, - fma(g, p, fma(h, o, fma(i, n, - fma(j, m, k * l)))));
    }

    @Overwrite
    public void multiplyByTranslation(float x, float y, float z) {
        this.a03 += fma(this.a00, x, fma(this.a01, y, this.a02 * z));
        this.a13 += fma(this.a10, x, fma(this.a11, y, this.a12 * z));
        this.a23 += fma(this.a20, x, fma(this.a21, y, this.a22 * z));
        this.a33 += fma(this.a30, x, fma(this.a31, y, this.a32 * z));
    }

    @Overwrite
    public void multiply(Matrix4f matrix) {
        matrix.writeRowMajor(fb);

        float a001 = memGetFloat(ad);
        float a011 = memGetFloat(ad+4);
        float a021 = memGetFloat(ad+8);
        float a031 = memGetFloat(ad+12);
        float a101 = memGetFloat(ad+16);
        float a111 = memGetFloat(ad+20);
        float a121 = memGetFloat(ad+24);
        float a131 = memGetFloat(ad+28);
        float a201 = memGetFloat(ad+32);
        float a211 = memGetFloat(ad+36);
        float a221 = memGetFloat(ad+40);
        float a231 = memGetFloat(ad+44);
        float a301 = memGetFloat(ad+48);
        float a311 = memGetFloat(ad+52);
        float a321 = memGetFloat(ad+56);
        float a331 = memGetFloat(ad+60);
        float f = fma(a00, a001, fma(a01, a101, fma(a02, a201, a03 * a301)));
        float g = fma(a00, a011, fma(a01, a111, fma(a02, a211, a03 * a311)));
        float h = fma(a00, a021, fma(a01, a121, fma(a02, a221, a03 * a321)));
//        float i = fma(a00, a031, fma(a01, a131, fma(a02, a231, a03 * a331)));
        float j = fma(a10, a001, fma(a11, a101, fma(a12, a201, a13 * a301)));
        float k = fma(a10, a011, fma(a11, a111, fma(a12, a211, a13 * a311)));
        float l = fma(a10, a021, fma(a11, a121, fma(a12, a221, a13 * a321)));
//        float m = fma(a10, a031, fma(a11, a131, fma(a12, a231, a13 * a331)));
        float n = fma(a20, a001, fma(a21, a101, fma(a22, a201, a23 * a301)));
        float o = fma(a20, a011, fma(a21, a111, fma(a22, a211, a23 * a311)));
        float p = fma(a20, a021, fma(a21, a121, fma(a22, a221, a23 * a321)));
        float q = fma(a20, a031, fma(a21, a131, fma(a22, a231, a23)));
        float r = fma(a30, a001, fma(a31, a101, fma(a32, a201, a33 * a301)));
        float s = fma(a30, a011, fma(a31, a111, fma(a32, a211, a33 * a311)));
        float t = fma(a30, a021, fma(a31, a121, fma(a32, a221, a33 * a321)));
        float u = fma(a30, a031, fma(a31, a131, fma(a32, a231, a33 * a331)));


        this.a00 = f;
        this.a01 = g;
        this.a02 = h;
//        this.a03 = i;
        this.a10 = j;
        this.a11 = k;
        this.a12 = l;
//        this.a13 = m;
        this.a20 = n;
        this.a21 = o;
        this.a22 = p;
        this.a23 = q;
        this.a30 = r;
        this.a31 = s;
        this.a32 = t;
        this.a33 = u;
    }
}
