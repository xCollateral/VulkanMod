package net.vulkanmod.render.chunk;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.vulkanmod.render.chunk.util.Util;
import org.joml.FrustumIntersection;

public class VFrustum {

    private Vector4f viewVector;
    private double camX;
    private double camY;
    private double camZ;

    private FrustumIntersection frustum;

    private final Vector4f[] frustumData = new Vector4f[6];

    public VFrustum(Matrix4f p_113000_, Matrix4f p_113001_) {
        this.calculateFrustum(p_113000_, p_113001_);

    }

//    public VFrustum(VFrustum p_194440_) {
////        System.arraycopy(p_194440_.frustumData, 0, this.frustumData, 0, p_194440_.frustumData.length);
//        this.camX = p_194440_.camX;
//        this.camY = p_194440_.camY;
//        this.camZ = p_194440_.camZ;
//        this.viewVector = p_194440_.viewVector;
//    }

    public VFrustum offsetToFullyIncludeCameraCube(int p_194442_) {
        double d0 = Math.floor(this.camX / (double)p_194442_) * (double)p_194442_;
        double d1 = Math.floor(this.camY / (double)p_194442_) * (double)p_194442_;
        double d2 = Math.floor(this.camZ / (double)p_194442_) * (double)p_194442_;
        double d3 = Math.ceil(this.camX / (double)p_194442_) * (double)p_194442_;
        double d4 = Math.ceil(this.camY / (double)p_194442_) * (double)p_194442_;
        double d5 = Math.ceil(this.camZ / (double)p_194442_) * (double)p_194442_;

        while(this.intersectAab((float)(d0 - this.camX), (float)(d1 - this.camY), (float)(d2 - this.camZ), (float)(d3 - this.camX), (float)(d4 - this.camY), (float)(d5 - this.camZ))
        >= 0) {
            this.camZ -= (double)(this.viewVector.z() * 4.0F);
            this.camX -= (double)(this.viewVector.x() * 4.0F);
            this.camY -= (double)(this.viewVector.y() * 4.0F);
        }

        return this;
    }

    public void prepare(double p_113003_, double p_113004_, double p_113005_) {
        this.camX = p_113003_;
        this.camY = p_113004_;
        this.camZ = p_113005_;
    }

    private void calculateFrustum(Matrix4f modelViewMatrix, Matrix4f projMatrix) {
        Matrix4f matrix4f = projMatrix.copy();
        matrix4f.multiply(modelViewMatrix);
        matrix4f.transpose();
        this.viewVector = new Vector4f(0.0F, 0.0F, 1.0F, 0.0F);
        this.viewVector.transform(matrix4f);

        org.joml.Matrix4f P = Util.convertMatrix(projMatrix);
        org.joml.Matrix4f MV = Util.convertMatrix(modelViewMatrix);
        this.frustum = new FrustumIntersection(P.mul(MV), true);

        this.getPlane(matrix4f, -1, 0, 0, 0);
        this.getPlane(matrix4f, 1, 0, 0, 1);
        this.getPlane(matrix4f, 0, -1, 0, 2);
        this.getPlane(matrix4f, 0, 1, 0, 3);
        this.getPlane(matrix4f, 0, 0, -1, 4);
        this.getPlane(matrix4f, 0, 0, 1, 5);
    }

    private void getPlane(Matrix4f p_113021_, int p_113022_, int p_113023_, int p_113024_, int p_113025_) {
        Vector4f vector4f = new Vector4f((float)p_113022_, (float)p_113023_, (float)p_113024_, 1.0F);
        vector4f.transform(p_113021_);
        vector4f.normalize();
        this.frustumData[p_113025_] = vector4f;
    }

//    public int isVisible(AABB p_113030_) {
//        return this.cubeInFrustum(p_113030_.minX, p_113030_.minY, p_113030_.minZ, p_113030_.maxX, p_113030_.maxY, p_113030_.maxZ);
//    }

    public int cubeInFrustum(float p_113007_, float p_113008_, float p_113009_, float p_113010_, float p_113011_, float p_113012_) {
        float f = (float)(p_113007_ - this.camX);
        float f1 = (float)(p_113008_ - this.camY);
        float f2 = (float)(p_113009_ - this.camZ);
        float f3 = (float)(p_113010_ - this.camX);
        float f4 = (float)(p_113011_ - this.camY);
        float f5 = (float)(p_113012_ - this.camZ);
        return this.intersectAab(f, f1, f2, f3, f4, f5);
//        return this.customIntersect(f, f1, f2, f3, f4, f5);
    }

    private int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.frustum.intersectAab(minX, minY, minZ, maxX, maxY, maxZ);
//        return this.frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ) ? FrustumIntersection.INSIDE : 1;
    }

    private int customIntersect(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for(int i = 0; i < 6; ++i) {
            Vector4f vector4f = this.frustumData[i];
            if (!(vector4f.dot(new Vector4f(minX, minY, minZ, 1.0F)) > 0.0F)
                    && !(vector4f.dot(new Vector4f(maxX, minY, minZ, 1.0F)) > 0.0F)
                    && !(vector4f.dot(new Vector4f(minX, maxY, minZ, 1.0F)) > 0.0F)
                    && !(vector4f.dot(new Vector4f(maxX, maxY, minZ, 1.0F)) > 0.0F)
                    && !(vector4f.dot(new Vector4f(minX, minY, maxZ, 1.0F)) > 0.0F)
                    && !(vector4f.dot(new Vector4f(maxX, minY, maxZ, 1.0F)) > 0.0F)
                    && !(vector4f.dot(new Vector4f(minX, maxY, maxZ, 1.0F)) > 0.0F)
                    && !(vector4f.dot(new Vector4f(maxX, maxY, maxZ, 1.0F)) > 0.0F)) {
                return 1;
            }
        }

        return -1;
    }
}
