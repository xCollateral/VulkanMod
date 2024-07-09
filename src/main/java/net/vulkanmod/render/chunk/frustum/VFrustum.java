package net.vulkanmod.render.chunk.frustum;

import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class VFrustum {
    private Vector4f viewVector = new Vector4f();
    private double camX;
    private double camY;
    private double camZ;

    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();

    public VFrustum offsetToFullyIncludeCameraCube(int offset) {
        double d0 = Math.floor(this.camX / (double) offset) * (double) offset;
        double d1 = Math.floor(this.camY / (double) offset) * (double) offset;
        double d2 = Math.floor(this.camZ / (double) offset) * (double) offset;
        double d3 = Math.ceil(this.camX / (double) offset) * (double) offset;
        double d4 = Math.ceil(this.camY / (double) offset) * (double) offset;
        double d5 = Math.ceil(this.camZ / (double) offset) * (double) offset;

        while (this.intersectAab((float) (d0 - this.camX), (float) (d1 - this.camY), (float) (d2 - this.camZ), (float) (d3 - this.camX), (float) (d4 - this.camY), (float) (d5 - this.camZ)) >= 0) {
            this.camZ -= (this.viewVector.z() * 4.0F);
            this.camX -= (this.viewVector.x() * 4.0F);
            this.camY -= (this.viewVector.y() * 4.0F);
        }

        return this;
    }

    public void setCamOffset(double camX, double camY, double camZ) {
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
    }

    public void calculateFrustum(Matrix4f modelViewMatrix, Matrix4f projMatrix) {
        projMatrix.mul(modelViewMatrix, this.matrix);

        this.frustum.set(this.matrix, false);
        this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
    }

    public int cubeInFrustum(float x1, float y1, float z1, float x2, float y2, float z2) {
        float f = (float) (x1 - this.camX);
        float f1 = (float) (y1 - this.camY);
        float f2 = (float) (z1 - this.camZ);
        float f3 = (float) (x2 - this.camX);
        float f4 = (float) (y2 - this.camY);
        float f5 = (float) (z2 - this.camZ);
        return this.intersectAab(f, f1, f2, f3, f4, f5);
    }

    public boolean testFrustum(float x1, float y1, float z1, float x2, float y2, float z2) {
        float f = (float) (x1 - this.camX);
        float f1 = (float) (y1 - this.camY);
        float f2 = (float) (z1 - this.camZ);
        float f3 = (float) (x2 - this.camX);
        float f4 = (float) (y2 - this.camY);
        float f5 = (float) (z2 - this.camZ);
        return this.frustum.testAab(f, f1, f2, f3, f4, f5);
    }

    private int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.frustum.intersectAab(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean isVisible(AABB aABB) {
        return this.cubeInFrustum(aABB.minX, aABB.minY, aABB.minZ, aABB.maxX, aABB.maxY, aABB.maxZ);
    }

    private boolean cubeInFrustum(double d, double e, double f, double g, double h, double i) {
        float j = (float) (d - this.camX);
        float k = (float) (e - this.camY);
        float l = (float) (f - this.camZ);
        float m = (float) (g - this.camX);
        float n = (float) (h - this.camY);
        float o = (float) (i - this.camZ);
        return this.frustum.testAab(j, k, l, m, n, o);
    }
}
