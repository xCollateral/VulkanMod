package net.vulkanmod.render.vertex;

import net.vulkanmod.vulkan.util.ColorUtil;

public class VertexUtil {

    private static final float NORM_INV = 1.0f / 127.0f;
    private static final float COLOR_INV = 1.0f / 255.0f;

    public static int packColor(float r, float g, float b, float a) {
        return ColorUtil.packColorIntRGBA(r, g, b, a);
    }

    public static int packNormal(float x, float y, float z) {
        x *= 127.0f;
        y *= 127.0f;
        z *= 127.0f;

        return ((int)x & 0xFF) | ((int)y & 0xFF) << 8|  ((int)z & 0xFF) << 16;
    }

    public static float unpackColorR(int i) {
        return ((i >> 24) & 0xFF) * COLOR_INV;
    }

    public static float unpackColorG(int i) {
        return ((i >> 16) & 0xFF) * COLOR_INV;
    }

    public static float unpackColorB(int i) {
        return ((i >> 8) & 0xFF) * COLOR_INV;
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
