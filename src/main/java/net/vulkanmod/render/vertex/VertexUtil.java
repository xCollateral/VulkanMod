package net.vulkanmod.render.vertex;

public class VertexUtil {

    private static final float NORM_INV = 1.0f / 127.0f;

    public static int packNormal(float x, float y, float z) {
        x *= 127.0f;
        y *= 127.0f;
        z *= 127.0f;

        return ((int)x & 0xFF) | ((int)y & 0xFF) << 8|  ((int)z & 0xFF) << 16;
    }

    public static int packNormal(int x, int y, int z) {
        return (x & 0xFF) | (y & 0xFF) << 8|  (z & 0xFF) << 16;
    }

    public static float unpackN1(int i) {
        return (byte)(i & 0xFF) * NORM_INV;
    }

    public static float unpackN2(int i) {
        return (byte)((i >> 8) & 0xFF) * NORM_INV;
    }

    public static float unpackN3(int i) {
        return (byte)((i >> 16) & 0xFF) * NORM_INV;
    }

}
