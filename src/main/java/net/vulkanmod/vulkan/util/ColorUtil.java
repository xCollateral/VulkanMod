package net.vulkanmod.vulkan.util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class ColorUtil {
    private static final float COLOR_INV = 1.0f / 255.0f;

    static ColorConsumer colorConsumer = new DefaultColorConsumer();

    public static void useGammaCorrection(boolean b) {
        colorConsumer = b ? new GammaColorConsumer() : new DefaultColorConsumer();
    }

    public static int floatToInt(float f) {
        return (int)(f * 255.0f) & 0xFF;
    }

    public static float unpackColor(int c, int s) {
        return ((c >> s) & 0xFF) * COLOR_INV;
    }

    public static class ARGB {
        public static int pack(float r, float g, float b, float a) {
            int color = floatToInt(a) << 24 | floatToInt(r) << 16 | floatToInt(g) << 8 | floatToInt(b);

            return color;
        }

        public static float unpackR(int color) {
            return unpackColor(color, 16);
        }

        public static float unpackG(int color) {
            return unpackColor(color, 8);
        }

        public static float unpackB(int color) {
            return unpackColor(color, 0);
        }

        public static float unpackA(int color) {
            return unpackColor(color, 24);
        }

        public static int multiplyAlpha(int color, float m) {
            int newA = floatToInt(unpackA(color) * m);
            return (color & 0x00FFFFFF) | newA << 24;
        }
    }

    public static class RGBA {
        public static int pack(float r, float g, float b, float a) {
//            int color = floatToInt(r) << 24 | floatToInt(g) << 16 | floatToInt(b) << 8 | floatToInt(a);
            int color = floatToInt(a) << 24 | floatToInt(b) << 16 | floatToInt(g) << 8 | floatToInt(r);

            return color;
        }

        public static float unpackR(int color) {
            return unpackColor(color, 24);
        }

        public static float unpackG(int color) {
            return unpackColor(color, 16);
        }

        public static float unpackB(int color) {
            return unpackColor(color, 8);
        }

        public static int fromArgb32(int i) {
            return i & 0xFF00FF00 | (i & 0xFF0000) >> 16 | (i & 0xFF) << 16;
        }
    }

    public static int BGRAtoRGBA(int v) {
        byte r = (byte) (v >> 16);
        byte b = (byte) (v);
        return r & 0xFF | (b << 16) & 0xFF0000 | v & 0xFF00FF00;
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
