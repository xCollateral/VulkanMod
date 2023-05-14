package net.vulkanmod.render.vertex;

public class VertexUtil {

    private static final float NORM_INV = 1.0f / 127.0f;

    public static int packColor(float r, float g, float b, float a) {
        r *= 255.0f;
        g *= 255.0f;
        b *= 255.0f;
        a *= 255.0f;

        return ((int)r & 0xFF) | ((int)g & 0xFF) << 8 |  ((int)b & 0xFF) << 16 | ((int)a & 0xFF) << 24;
    }

    public static int packNormal(float x, float y, float z) {
        x *= 127.0f;
        y *= 127.0f;
        z *= 127.0f;

        return ((int)x & 0xFF) | ((int)y & 0xFF) << 8|  ((int)z & 0xFF) << 16;
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
