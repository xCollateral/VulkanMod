package net.vulkanmod.render.chunk.build.light.smooth;

import net.vulkanmod.render.chunk.util.SimpleDirection;

/**
 * The neighbor information for each face of a block, used when performing smooth lighting in order to calculate
 * the occlusion of each corner.
 */
@SuppressWarnings("UnnecessaryLocalVariable")
enum AoNeighborInfo {
    DOWN(new SimpleDirection[] { SimpleDirection.SOUTH, SimpleDirection.WEST, SimpleDirection.NORTH, SimpleDirection.EAST },
        new int[] {4, 5, 6, 7,
                0, 1, 2, 3},
        0.5F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = 1.0f - x;
            final float v = z;

            calculateCornerWeights(u, v, out);
        }

        @Override
        public float getU(float x, float y, float z) {
            return (1.0f - x);
        }

        @Override
        public float getV(float x, float y, float z) {
            return z;
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return y;
        }
    },
    UP(new SimpleDirection[] { SimpleDirection.NORTH, SimpleDirection.WEST, SimpleDirection.SOUTH, SimpleDirection.EAST },
        new int[] {2, 3, 0, 1,
                    6, 7, 4, 5},
        1.0F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = 1.0f - x;
            final float v = 1.0f - z;

            calculateCornerWeights(u, v, out);
        }

        @Override
        public float getU(float x, float y, float z) {
            return (1.0f - x);
        }

        @Override
        public float getV(float x, float y, float z) {
            return (1.0f - z);
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return 1.0f - y;
        }
    },
    NORTH(new SimpleDirection[] { SimpleDirection.UP, SimpleDirection.EAST, SimpleDirection.DOWN, SimpleDirection.WEST },
            new int[] {3, 2, 7, 6,
                    1, 0, 5, 4},
            0.8F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = x;
            final float v = y;

            calculateCornerWeights(u, v, out);
        }

        @Override
        public float getU(float x, float y, float z) {
            return x;
        }

        @Override
        public float getV(float x, float y, float z) {
            return y;
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return z;
        }
    },
    SOUTH(new SimpleDirection[] { SimpleDirection.UP, SimpleDirection.WEST, SimpleDirection.DOWN, SimpleDirection.EAST },
            new int[] {0, 1, 4, 5,
                    2, 3, 6, 7},
            0.8F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = 1.0f - x;
            final float v = y;

            calculateCornerWeights(u, v, out);
        }

        @Override
        public float getU(float x, float y, float z) {
            return (1.0f - x);
        }

        @Override
        public float getV(float x, float y, float z) {
            return y;
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return 1.0f - z;
        }
    },
    WEST(new SimpleDirection[] { SimpleDirection.UP, SimpleDirection.NORTH, SimpleDirection.DOWN, SimpleDirection.SOUTH },
            new int[] {1, 3, 5, 7,
                    0, 2, 4, 6},
            0.6F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = 1.0f - z;
            final float v = y;

            calculateCornerWeights(u, v, out);
        }

        @Override
        public float getU(float x, float y, float z) {
            return (1.0f - z);
        }

        @Override
        public float getV(float x, float y, float z) {
            return y;
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return x;
        }
    },
    EAST(new SimpleDirection[] { SimpleDirection.UP, SimpleDirection.SOUTH, SimpleDirection.DOWN, SimpleDirection.NORTH },
            new int[] {2, 0, 6, 4,
                    3, 1, 7, 5},
            0.6F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = z;
            final float v = y;

            calculateCornerWeights(u, v, out);
        }

        @Override
        public float getU(float x, float y, float z) {
            return z;
        }

        @Override
        public float getV(float x, float y, float z) {
            return y;
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return 1.0f - x;
        }
    };

    /**
     * The direction of each corner block from this face, which can be retrieved by offsetting the position of the origin
     * block by the direction vector.
     */
    public final SimpleDirection[] faces;

    /**
     * The constant brightness modifier for this face. This data exists to emulate the results of the OpenGL lighting
     * model which gives a faux directional light appearance to blocks in the game. Not currently used.
     */
    public final float strength;

    /**
     * The indexes of each inner corner occlusion bit for every model vertex.
     */
    public final int[] inCornerBits = new int[4 * 2];

    /**
     * The indexes of each outer corner occlusion bit for every model vertex.
     */
    public final int[] outCornerBits = new int[3 * 4 * 2];

    AoNeighborInfo(SimpleDirection[] directions, int[] indices, float strength) {
        this.faces = directions;
        this.strength = strength;

        copyInCornerBits(this.inCornerBits, indices);
        getOutCornerBits(this.outCornerBits, indices);
    }

    /**
     * Calculates how much each corner contributes to the final "darkening" of the vertex at the specified position. The
     * weight is a function of the distance from the vertex's position to the corner block's position.
     *
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param out The weight values for each corner
     */
    public abstract void calculateCornerWeights(float x, float y, float z, float[] out);

    public abstract float getU(float x, float y, float z);

    public abstract float getV(float x, float y, float z);

    /**
     * Maps the light map and occlusion value arrays {@param lm0} and {@param ao0} from {@link AoFaceData} to the
     * correct corners for this facing.
     *
     * @param lm0 The input light map texture coordinates array
     * @param ao0 The input ambient occlusion color array
     * @param lm1 The re-orientated output light map texture coordinates array
     * @param ao1 The re-orientated output ambient occlusion color array
     */
    public void copyLightValues(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
        lm1[0] = lm0[0];
        lm1[1] = lm0[1];
        lm1[2] = lm0[2];
        lm1[3] = lm0[3];

        ao1[0] = ao0[0];
        ao1[1] = ao0[1];
        ao1[2] = ao0[2];
        ao1[3] = ao0[3];
    }

    /**
     * Calculates the depth (or inset) of the vertex into this facing of the block. Used to determine
     * how much shadow is contributed by the direct neighbors of a block.
     *
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @return The depth of the vertex into this face
     */
    public abstract float getDepth(float x, float y, float z);

    private static final AoNeighborInfo[] VALUES = AoNeighborInfo.values();

    /**
     * @return Returns the {@link AoNeighborInfo} which corresponds with the specified direction
     */
    public static AoNeighborInfo get(SimpleDirection direction) {
        return VALUES[direction.get3DDataValue()];
    }

    /**
     * Calculates corner weights using bilinear interpolation.
     */
    public static void calculateCornerWeights(float u, float v, float[] out) {
        out[0] = u * v;
        out[1] = u * (1.0f - v);
        out[2] = (1.0f - u) * (1.0f - v);
        out[3] = (1.0f - u) * v;
    }

    /**
     * Prepares corner occlusion indexes which will be used by {@link SubBlockAoFace} to calculate
     * face AO values.
     */
    private static void copyInCornerBits(int[] cornersBits, int[] idxs) {
        cornersBits[0] = idxs[0];
        cornersBits[1] = idxs[1];
        cornersBits[2] = idxs[2];
        cornersBits[3] = idxs[3];
        cornersBits[4] = idxs[4];
        cornersBits[5] = idxs[5];
        cornersBits[6] = idxs[6];
        cornersBits[7] = idxs[7];
    }

    /**
     * Prepares corner occlusion indexes which will be used by {@link SubBlockAoFace} to calculate
     * face AO values.
     */
    private static void getOutCornerBits(int[] cornersBits, int[] idxs) {
        cornersBits[0] = idxs[0];
        cornersBits[1] = idxs[3];
        cornersBits[2] = idxs[1];

        cornersBits[3] = idxs[1];
        cornersBits[4] = idxs[2];
        cornersBits[5] = idxs[3];

        cornersBits[6] = idxs[3];
        cornersBits[7] = idxs[0];
        cornersBits[8] = idxs[2];

        cornersBits[9] = idxs[2];
        cornersBits[10] = idxs[1];
        cornersBits[11] = idxs[0];

        cornersBits[12 + 0] = idxs[4 + 0];
        cornersBits[12 + 1] = idxs[4 + 3];
        cornersBits[12 + 2] = idxs[4 + 1];

        cornersBits[12 + 3] = idxs[4 + 1];
        cornersBits[12 + 4] = idxs[4 + 2];
        cornersBits[12 + 5] = idxs[4 + 3];

        cornersBits[12 + 6] = idxs[4 + 3];
        cornersBits[12 + 7] = idxs[4 + 0];
        cornersBits[12 + 8] = idxs[4 + 2];

        cornersBits[12 + 9] = idxs[4 + 2];
        cornersBits[12 + 10] = idxs[4 + 1];
        cornersBits[12 + 11] = idxs[4 + 0];
    }
}
