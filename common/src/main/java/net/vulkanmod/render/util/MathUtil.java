package net.vulkanmod.render.util;

public class MathUtil {

    public static float clamp(float min, float max, float x) {
        return Math.min(Math.max(x, min), max);
    }

    public static int clamp(int min, int max, int x) {
        return Math.min(Math.max(x, min), max);
    }

    public static float saturate(float x) {
        return clamp(0.0f, 1.0f, x);
    }

    public static float lerp(float v0, float v1, float t) {
        return v0 + t * (v1 - v0);
    }
}
