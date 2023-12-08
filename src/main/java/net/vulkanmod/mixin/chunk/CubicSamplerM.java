package net.vulkanmod.mixin.chunk;

import net.minecraft.util.CubicSampler;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.vulkan.VRenderSystem;
import org.checkerframework.dataflow.qual.Pure;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CubicSampler.class)
public class CubicSamplerM
{
   @Unique
   private static final float[] GAUSSIAN_SAMPLE_KERNEL=new float[]{0F, 1F, 4F, 6F, 4F, 1F, 0F};
    @Unique
    private static final float[] GAUSSIAN_SAMPLE_KERNEL3=new float[6];

    static {
        final float aFloat = 1;
//        for(int l = 0; l < 6; ++l)
//        {
//            GAUSSIAN_SAMPLE_KERNEL2[l] = fastLerp(aFloat, GAUSSIAN_SAMPLE_KERNEL[l + 1], GAUSSIAN_SAMPLE_KERNEL[l]);
//
//        }
        for(int n = 0; n < 6; ++n)
        {
            GAUSSIAN_SAMPLE_KERNEL3[n] = fastLerp(aFloat, GAUSSIAN_SAMPLE_KERNEL[n + 1], GAUSSIAN_SAMPLE_KERNEL[n]) * fastLerp(1, GAUSSIAN_SAMPLE_KERNEL[n + 1], GAUSSIAN_SAMPLE_KERNEL[n]);

        }
    }

    @Unique
    private static final int anInt = 6;
    @Unique
    @Pure private static float fastLerp(float f, float g, float h) {
        return org.joml.Math.fma(f, h - g, g);
    }
    @Unique
    @Pure private static double fastLerp(double f, double g, double h) {
        return org.joml.Math.fma(f, h - g, g);
    }
    /**
     * @author
     * @reason this uses extremely aggressive Optimisations, so may not reproduce the Sky 100% as accurately to vanilla
     */

    //TODO: Maybe get of UNorm and replace with UInt Colour Operations
    @Overwrite
    public static Vec3 gaussianSampleVec3(Vec3 vec3, CubicSampler.Vec3Fetcher vec3Fetcher) {
        int d3 = (int) vec3.x();
        int d2 = (int) vec3.y();
        int d1 = (int) vec3.z();
        float d = (float) (vec3.x() - d3);
        float e = (float) (vec3.y() - d2);
        float f = (float) (vec3.z() - d1);
        float fx = d*e*f/3;
        float g = 0f;
        Vector3f vec32 = new Vector3f();

        for(int l = 0; l < 6; ++l) {
            final int m = d3 - 2 + l;

            for(int n = 0; n < 6; ++n) {

                final int p = d2 - 2 + n;
                final Vec3 vec31 = vec3Fetcher.fetch(m, p, d1 - 2 + n);
                for(int q = 0; q < 6; ++q) {
                    final float t = GAUSSIAN_SAMPLE_KERNEL3[l] * fastLerp(fx, GAUSSIAN_SAMPLE_KERNEL[q + 1], GAUSSIAN_SAMPLE_KERNEL[q]);
                    g += t;

                    vec32.x += vec31.x * t;
                    vec32.y += vec31.y * t;
                    vec32.z += vec31.z * t;
                }
            }
        }


        return new Vec3(vec32.x, vec32.y, vec32.z).scale(1.0 / g);
    }
    /*@Overwrite
    public static Vec3 gaussianSampleVec3(Vec3 vec3, CubicSampler.Vec3Fetcher vec3Fetcher) {
        final int i = Mth.fastFloor(vec3.x());
        final int j = Mth.fastFloor(vec3.y());
        final int k = Mth.fastFloor(vec3.z());
        final float d = (float) (vec3.x() - i);
        final float e = (float) (vec3.y() - j);
        final float f = (float) (vec3.z() - k);
        double g = 0;
        Vector3d vec32 = new Vector3d(0);

        for (int l = 0; l < anInt; ++l) {
            final float h = fastLerp(d, GAUSSIAN_SAMPLE_KERNEL[l + 1], GAUSSIAN_SAMPLE_KERNEL[l]);
            final int m = i - 2 + l;

            for (int n = 0; n < anInt; ++n) {
                final float o = fastLerp(e, GAUSSIAN_SAMPLE_KERNEL[n + 1], GAUSSIAN_SAMPLE_KERNEL[n]);
                final int p = j - 2 + n;

                final Vec3 vec31 = vec3Fetcher.fetch(m, p, k - 2 + n);
                for (int q = 0; q < anInt; ++q) {
                    final float t = Math.fma(GAUSSIAN_SAMPLE_KERNEL[q + 1], Math.fma(f, f - o, o), Math.fma(f, h - GAUSSIAN_SAMPLE_KERNEL[q], GAUSSIAN_SAMPLE_KERNEL[q]));
                    vec32.x =  Math.fma(vec31.x, t, vec32.x);
                    vec32.y =  Math.fma(vec31.y, t, vec32.y);
                    vec32.z =  Math.fma(vec31.z+ k - 2 + q, t, vec32.z);
                    g += t;
                }
            }
        }


        vec32.mul(1.0 / g, 1.0 / g, 1.0 / g);
        return new Vec3(vec32.x, vec32.y, vec32.z);
    }*/
}
