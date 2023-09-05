package net.vulkanmod.vulkan.util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class ColorUtil {

    static ColorConsumer colorConsumer = new DefaultColorConsumer();

    public static void useGammaCorrection(boolean b) {
        colorConsumer = b ? new GammaColorConsumer() : new DefaultColorConsumer();
    }

    public static int packColorInt(float r, float g, float b, float a) {
        int color = 0;
        color += (int)(a * 255) << 24;
        color += (int)(r * 255) << 16;
        color += (int)(g * 255) << 8;
        color += (int)(b * 255);

        return color;
    }

    public static int BGRAtoRGBA(int v) {
        byte r = (byte) (v >> 16);
        byte g = (byte) (v >> 8);
        byte b = (byte) (v);
        byte a = (byte) (v >> 24);

        return r & 0xFF | (g << 8) & 0xFF00 | (b << 16) & 0xFF0000 | (a << 24) & 0xFF000000;
    }

    public static float gamma(float f) {
        return (float) Math.pow(f, 2.2);
    }

    public static void setRGBA_Buffer(MappedBuffer buffer, float r, float g, float b, float a) {
        colorConsumer.setRGBA_Buffer(buffer, r, g, b, a);
    }

    public static void setRGBA_Buffer(FloatBuffer buffer, float r, float g, float b, float a) {
        colorConsumer.setRGBA_Buffer(buffer, r, g, b, a);
    }

    interface ColorConsumer {

        void setRGBA_Buffer(MappedBuffer buffer, float r, float g, float b, float a);
        void setRGBA_Buffer(FloatBuffer buffer, float r, float g, float b, float a);
        void setRGBA_Buffer(ByteBuffer buffer, float r, float g, float b, float a);

        default void putColor(MappedBuffer buffer, float r, float g, float b, float a) {
            buffer.putFloat(0, r);
            buffer.putFloat(4, g);
            buffer.putFloat(8, b);
            buffer.putFloat(12, a);
        }

        default void putColor(FloatBuffer buffer, float r, float g, float b, float a) {
            buffer.put(0, r);
            buffer.put(1, g);
            buffer.put(2, b);
            buffer.put(3, a);
        }

        default void putColor(ByteBuffer buffer, float r, float g, float b, float a) {
            buffer.putFloat(0, r);
            buffer.putFloat(4, g);
            buffer.putFloat(8, b);
            buffer.putFloat(12, a);
        }
    }

    public static class DefaultColorConsumer implements ColorConsumer {

        @Override
        public void setRGBA_Buffer(MappedBuffer buffer, float r, float g, float b, float a) {
            putColor(buffer, r, g, b, a);
        }

        @Override
        public void setRGBA_Buffer(FloatBuffer buffer, float r, float g, float b, float a) {
            putColor(buffer, r, g, b, a);
        }

        @Override
        public void setRGBA_Buffer(ByteBuffer buffer, float r, float g, float b, float a) {
            putColor(buffer, r, g, b, a);
        }
    }

    public static class GammaColorConsumer implements ColorConsumer {

        @Override
        public void setRGBA_Buffer(MappedBuffer buffer, float r, float g, float b, float a) {
            r = gamma(r);
            g = gamma(g);
            b = gamma(b);
            putColor(buffer, r, g, b, a);
        }

        @Override
        public void setRGBA_Buffer(FloatBuffer buffer, float r, float g, float b, float a) {
            r = gamma(r);
            g = gamma(g);
            b = gamma(b);
            putColor(buffer, r, g, b, a);
        }

        @Override
        public void setRGBA_Buffer(ByteBuffer buffer, float r, float g, float b, float a) {
            r = gamma(r);
            g = gamma(g);
            b = gamma(b);
            putColor(buffer, r, g, b, a);
        }

    }
}
