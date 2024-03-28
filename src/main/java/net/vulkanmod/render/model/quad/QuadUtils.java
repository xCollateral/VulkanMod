package net.vulkanmod.render.model.quad;

public abstract class QuadUtils {
    public static final byte DEFAULT_START_IDX = 0;
    public static final byte FLIPPED_START_IDX = 3;

    /***
     * Gets start idx based on AO and LM values to fix anisotropy
     */
    public static int getIterationStartIdx(float[] aos, int[] lms) {
        final float ao00_11 = aos[0] + aos[2];
        final float ao10_01 = aos[1] + aos[3];
        if(ao00_11 > ao10_01) {
            return DEFAULT_START_IDX;
        } else if(ao00_11 < ao10_01) {
            return FLIPPED_START_IDX;
        }

        final float lm00_11 = lms[0] + lms[2];
        final float lm10_01 = lms[1] + lms[3];
//        if(lm00_11 >= lm10_01) {
//            return DEFAULT_START_IDX;
//        } else {
//            return FLIPPED_START_IDX;
//        }

        if(lm00_11 >= lm10_01) {
            return FLIPPED_START_IDX;
        } else {
            return DEFAULT_START_IDX;
        }
    }

    /***
     * Gets start idx based on AO values to fix anisotropy
     */
    public static int getIterationStartIdx(float[] aos) {
        final float ao00_11 = aos[0] + aos[2];
        final float ao10_01 = aos[1] + aos[3];
        if(ao00_11 >= ao10_01) {
            return DEFAULT_START_IDX;
        } else {
            return FLIPPED_START_IDX;
        }

    }
}
