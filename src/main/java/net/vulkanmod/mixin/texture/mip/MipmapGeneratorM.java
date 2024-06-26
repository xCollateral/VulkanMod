package net.vulkanmod.mixin.texture.mip;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.vulkanmod.mixin.texture.image.NativeImageAccessor;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MipmapGenerator.class)
public abstract class MipmapGeneratorM {
    private static final int ALPHA_CUTOFF = 50;

    @Shadow
    private static float getPow22(int i) {
        return 0;
    }

    /**
     * @author
     * @reason Add an average background color to texture that have transparent backgrounds
     * to fix mipmaps artifacts
     */
    @SuppressWarnings("UnreachableCode")
    @Overwrite
    public static NativeImage[] generateMipLevels(NativeImage[] nativeImages, int i) {
        if (i + 1 <= nativeImages.length) {
            return nativeImages;
        } else {
            NativeImage[] nativeImages2 = new NativeImage[i + 1];
            nativeImages2[0] = nativeImages[0];

            long srcPtr = ((NativeImageAccessor)(Object)nativeImages2[0]).getPixels();
            boolean bl = hasTransparentPixel(srcPtr, nativeImages2[0].getWidth(), nativeImages2[0].getHeight());

            if (bl) {
                int avg = calculateAverage(nativeImages2[0]);
                avg = avg & 0x00FFFFFF; //mask out alpha

                NativeImage nativeImage = nativeImages2[0];
                int width = nativeImage.getWidth();
                int height = nativeImage.getHeight();

                for (int m = 0; m < width; ++m) {
                    for (int n = 0; n < height; ++n) {
                        int p0 = MemoryUtil.memGetInt(srcPtr + (m + ((long) n * width)) * 4L);

                        boolean b0 = ((p0 >> 24) & 0xFF) >= ALPHA_CUTOFF;

                        p0 = b0 ? p0 : (avg | p0 & 0xFF000000);

                        int outColor = p0;
                        MemoryUtil.memPutInt(srcPtr + (m + (long) n * width) * 4L, outColor);
                    }
                }

            }

            for(int j = 1; j <= i; ++j) {
                if (j < nativeImages.length) {
                    nativeImages2[j] = nativeImages[j];
                } else {
                    NativeImage nativeImage = nativeImages2[j - 1];
                    NativeImage nativeImage2 = new NativeImage(nativeImage.getWidth() >> 1, nativeImage.getHeight() >> 1, false);
                    int width = nativeImage2.getWidth();
                    int height = nativeImage2.getHeight();

                    srcPtr = ((NativeImageAccessor)(Object)nativeImage).getPixels();
                    long dstPtr = ((NativeImageAccessor)(Object)nativeImage2).getPixels();
                    final int width2 = width * 2;

                    for(int m = 0; m < width; ++m) {
                        for(int n = 0; n < height; ++n) {
                            int p0 = MemoryUtil.memGetInt(srcPtr + ((m * 2 + 0) + ((n * 2 + 0) * width2)) * 4L);
                            int p1 = MemoryUtil.memGetInt(srcPtr + ((m * 2 + 1) + ((n * 2 + 0) * width2)) * 4L);
                            int p2 = MemoryUtil.memGetInt(srcPtr + ((m * 2 + 0) + ((n * 2 + 1) * width2)) * 4L);
                            int p3 = MemoryUtil.memGetInt(srcPtr + ((m * 2 + 1) + ((n * 2 + 1) * width2)) * 4L);

                            int outColor = blend(p0, p1, p2, p3);
                            MemoryUtil.memPutInt(dstPtr + (m + (long) n * width) * 4L, outColor);
                        }
                    }

                    nativeImages2[j] = nativeImage2;
                }
            }

            return nativeImages2;
        }
    }

    private static boolean hasTransparentPixel(long ptr, int width, int height) {
        for(int i = 0; i < width; ++i) {
            for(int j = 0; j < height; ++j) {
                if (getPixelA(MemoryUtil.memGetInt(ptr + (i + j * width) * 4L)) == 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int blend(int p0, int p1, int p2, int p3) {
        int a = gammaBlend(p0, p1, p2, p3, 24);
//        int a = ((p0 >> 24 & 0xFF) + (p1 >> 24 & 0xFF) + (p2 >> 24 & 0xFF) + (p3 >> 24 & 0xFF)) >> 2;
        int b = gammaBlend(p0, p1, p2, p3, 16);
        int g = gammaBlend(p0, p1, p2, p3, 8);
        int r = gammaBlend(p0, p1, p2, p3, 0);
        return a << 24 | b << 16 | g << 8 | r;
    }

    private static int getMax(int i0, int i1, int i2, int i3) {
        return Math.max(Math.max(Math.max(i0, i1), i2), i3);
    }

    private static int gammaBlend(int i, int j, int k, int l, int m) {
        float f = getPow22(i >> m);
        float g = getPow22(j >> m);
        float h = getPow22(k >> m);
        float n = getPow22(l >> m);
        float o = (float)((double)((float)Math.pow((double)(f + g + h + n) * 0.25, 0.45454545454545453)));
        return (int)((double)o * 255.0);
    }

    private static int getPixelA(int rgba) {
        return rgba >> 24;
    }

    @SuppressWarnings("UnreachableCode")
    private static int calculateAverage(NativeImage nativeImage) {
        final int width = nativeImage.getWidth();
        final int height = nativeImage.getHeight();

        final int[] values = new int[width * height];
        int count = 0;
        long srcPtr = ((NativeImageAccessor)(Object)nativeImage).getPixels();

        for(int i = 0; i < width; ++i) {
            for(int j = 0; j < height; ++j) {
//                int value = nativeImage.getPixelRGBA(i, j);
                int value = MemoryUtil.memGetInt(srcPtr + (i + (long) j * width) * 4L);
                if (((value >> 24) & 0xFF) > 0) {
                    values[count] = value;
                    count++;
                }
            }
        }

        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        for (int i = 0; i < count; i++) {
            sumR += values[i] & 0xFF;
            sumG += (values[i] >> 8) & 0xFF;
            sumB += (values[i] >> 16) & 0xFF;
        }

        if(count == 0)
            return 0;

        sumR /= count;
        sumG /= count;
        sumB /= count;

        return (sumR & 0xFF) | ((sumG & 0xFF) << 8) | ((sumB & 0xFF) << 16) | (0xFF << 24);
    }
}
