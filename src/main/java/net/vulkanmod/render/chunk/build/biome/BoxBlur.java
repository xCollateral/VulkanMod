package net.vulkanmod.render.chunk.build.biome;

public class BoxBlur {

    public static void blur(int[] buffer, int[] temp, int width, int filterRadius) {
        horizontalBlur(buffer, temp, 0, width, filterRadius);
        horizontalBlur(temp, buffer, filterRadius, width, filterRadius);
    }

    public static void horizontalBlur(int[] src, int[] dst, int y0, int width, int filterRadius) {
        final int div = filterRadius * 2 + 1;
        final int x0 = filterRadius;
        final int totalWidth = (filterRadius * 2) + width;
        for (int y = y0; y < totalWidth; y++) {
            int color;
            int r = 0, g = 0, b = 0;

            //init accumulator
            for(int x = 0; x < x0 + 1 + filterRadius; ++x) {
                color = src[getIdx(x, y, totalWidth)];
                r += unpackR(color);
                g += unpackG(color);
                b += unpackB(color);
            }

            dst[getIdx(y, x0, totalWidth)] = packColor(r, g, b, div);

            for (int x = x0 + 1; x < x0 + width; x++) {
                color = src[getIdx(x - filterRadius - 1, y, totalWidth)];
                r -= unpackR(color);
                g -= unpackG(color);
                b -= unpackB(color);

                color = src[getIdx(x + filterRadius, y, totalWidth)];
                r += unpackR(color);
                g += unpackG(color);
                b += unpackB(color);

                //transpose
                dst[getIdx(y, x, totalWidth)] = packColor(r, g, b, div);
            }
        }
    }

    public static int getIdx(int x, int z, int width) {
        return x + z * width;
    }

    public static int unpackR(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int unpackG(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int unpackB(int color) {
        return color & 0xFF;
    }

    public static int packColor(int r, int g, int b, int div) {
        return 0xFF000000 | (((r / div) & 0xFF) << 16) | (((g / div) & 0xFF) << 8) | ((b / div) & 0xFF);
    }
}
