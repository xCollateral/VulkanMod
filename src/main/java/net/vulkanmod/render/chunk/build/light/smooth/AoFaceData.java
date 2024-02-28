package net.vulkanmod.render.chunk.build.light.smooth;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.vulkanmod.render.chunk.util.SimpleDirection;
import net.vulkanmod.render.chunk.build.light.data.LightDataAccess;

import static net.vulkanmod.render.chunk.build.light.data.LightDataAccess.*;

class AoFaceData {
    public final int[] lm = new int[4];

    public final float[] ao = new float[4];
    public final float[] bl = new float[4];
    public final float[] sl = new float[4];

    protected int flags;

    public void initLightData(LightDataAccess cache, BlockPos pos, SimpleDirection direction, boolean offset) {
        final int oX = pos.getX();
        final int oY = pos.getY();
        final int oZ = pos.getZ();

        final int x;
        final int y;
        final int z;

        if (offset) {
            // Requested face is outer hence light data is referring to adjacent block
            x = oX + direction.getStepX();
            y = oY + direction.getStepY();
            z = oZ + direction.getStepZ();
        } else {
            x = oX;
            y = oY;
            z = oZ;
        }

        final int e = cache.get(x, y, z);

        final int olm;
        final boolean oem;

        if (offset && unpackFO(e)) {
            final int originWord = cache.get(oX, oY, oZ);
            olm = getLightmap(originWord);
            oem = unpackEM(originWord);
        } else {
            olm = getLightmap(e);
            oem = unpackEM(e);
        }

        final float oao = unpackAO(e);

        SimpleDirection[] faces = AoNeighborInfo.get(direction).faces;

        // Edges
        final int e0 = cache.get(x, y, z, faces[0]);
        final int e0lm = getLightmap(e0);
        final float e0ao = unpackAO(e0);
        final boolean e0op = unpackOP(e0);
        final boolean e0em = unpackEM(e0);

        final int e1 = cache.get(x, y, z, faces[1]);
        final int e1lm = getLightmap(e1);
        final float e1ao = unpackAO(e1);
        final boolean e1op = unpackOP(e1);
        final boolean e1em = unpackEM(e1);

        final int e2 = cache.get(x, y, z, faces[2]);
        final int e2lm = getLightmap(e2);
        final float e2ao = unpackAO(e2);
        final boolean e2op = unpackOP(e2);
        final boolean e2em = unpackEM(e2);

        final int e3 = cache.get(x, y, z, faces[3]);
        final int e3lm = getLightmap(e3);
        final float e3ao = unpackAO(e3);
        final boolean e3op = unpackOP(e3);
        final boolean e3em = unpackEM(e3);

        // If both edges of a corner are occluded, use edge value
        // This is generally right since full blocks should have the same ao value
        final int c0lm;
        final float c0ao;
        final boolean c0em;

        if (e0op && e1op) {
            c0lm = e1lm;
            c0ao = e1ao;
            c0em = e1em;
        } else {
            int d0 = cache.get(x, y, z, faces[0], faces[1]);
            c0lm = getLightmap(d0);
            c0ao = unpackAO(d0);
            c0em = unpackEM(d0);
        }

        final int c1lm;
        final float c1ao;
        final boolean c1em;

        if (e1op && e2op) {
            c1lm = e1lm;
            c1ao = e1ao;
            c1em = e1em;
        } else {
            int d1 = cache.get(x, y, z, faces[1], faces[2]);
            c1lm = getLightmap(d1);
            c1ao = unpackAO(d1);
            c1em = unpackEM(d1);
        }

        final int c2lm;
        final float c2ao;
        final boolean c2em;

        if (e2op && e3op) {
            c2lm = e3lm;
            c2ao = e3ao;
            c2em = e3em;
        } else {
            int d2 = cache.get(x, y, z, faces[2], faces[3]);
            c2lm = getLightmap(d2);
            c2ao = unpackAO(d2);
            c2em = unpackEM(d2);
        }

        final int c3lm;
        final float c3ao;
        final boolean c3em;

        if (e3op && e0op) {
            c3lm = e3lm;
            c3ao = e3ao;
            c3em = e3em;
        } else {
            int d3 = cache.get(x, y, z, faces[3], faces[0]);
            c3lm = getLightmap(d3);
            c3ao = unpackAO(d3);
            c3em = unpackEM(d3);
        }

        float[] ao = this.ao;
        ao[0] = (e0ao + e1ao + c0ao + oao) * 0.25f;
        ao[1] = (e1ao + e2ao + c1ao + oao) * 0.25f;
        ao[2] = (e2ao + e3ao + c2ao + oao) * 0.25f;
        ao[3] = (e3ao + e0ao + c3ao + oao) * 0.25f;

        int[] cb = this.lm;
        cb[0] = calculateCornerBrightness(e0lm, e1lm, c0lm, olm, e0em, e1em, c0em, oem);
        cb[1] = calculateCornerBrightness(e1lm, e2lm, c1lm, olm, e1em, e2em, c1em, oem);
        cb[2] = calculateCornerBrightness(e2lm, e3lm, c2lm, olm, e2em, e3em, c2em, oem);
        cb[3] = calculateCornerBrightness(e3lm, e0lm, c3lm, olm, e3em, e0em, c3em, oem);

        this.flags |= FaceDataFlags.HAS_LIGHT_DATA;
    }

    public void unpackLightData() {
        int[] lm = this.lm;

        float[] bl = this.bl;
        float[] sl = this.sl;

        bl[0] = unpackBlockLight(lm[0]);
        bl[1] = unpackBlockLight(lm[1]);
        bl[2] = unpackBlockLight(lm[2]);
        bl[3] = unpackBlockLight(lm[3]);

        sl[0] = unpackSkyLight(lm[0]);
        sl[1] = unpackSkyLight(lm[1]);
        sl[2] = unpackSkyLight(lm[2]);
        sl[3] = unpackSkyLight(lm[3]);

        this.flags |= FaceDataFlags.HAS_UNPACKED_LIGHT_DATA;
    }

    public boolean getCornerOcclusion(int bits, int[] values, int i) {
        return (bits & (1 << values[i])) != 0;
    }

    public float getBlendedSkyLight(float[] w) {
        return weightedSum(this.sl, w);
    }

    public float getBlendedBlockLight(float[] w) {
        return weightedSum(this.bl, w);
    }

    public float getBlendedShade(float[] w) {
        return weightedSum(this.ao, w);
    }

    static float weightedSum(float[] v, float[] w) {
        float t0 = v[0] * w[0];
        float t1 = v[1] * w[1];
        float t2 = v[2] * w[2];
        float t3 = v[3] * w[3];

        return t0 + t1 + t2 + t3;
    }

    static float unpackSkyLight(int i) {
        return (i >> 16) & 0xFF;
    }

    static float unpackBlockLight(int i) {
        return i & 0xFF;
    }

    static int calculateCornerBrightness(int a, int b, int c, int d, boolean aem, boolean bem, boolean cem, boolean dem) {
        // FIX: Normalize corner vectors correctly to the minimum non-zero value between each one to prevent
        // strange issues
        if ((a == 0) || (b == 0) || (c == 0) || (d == 0)) {
            // Find the minimum value between all corners
            final int min = minNonZero(minNonZero(a, b), minNonZero(c, d));

            // Normalize the corner values
            a = Math.max(a, min);
            b = Math.max(b, min);
            c = Math.max(c, min);
            d = Math.max(d, min);
        }

        // FIX: Apply the fullbright lightmap from emissive blocks at the very end so it cannot influence
        // the minimum lightmap and produce incorrect results (for example, sculk sensors in a dark room)
        if (aem) {
            a = LightTexture.FULL_BRIGHT;
        }
        if (bem) {
            b = LightTexture.FULL_BRIGHT;
        }
        if (cem) {
            c = LightTexture.FULL_BRIGHT;
        }
        if (dem) {
            d = LightTexture.FULL_BRIGHT;
        }

        return ((a + b + c + d) >> 2) & 0xFF00FF;
    }

    static int minNonZero(int a, int b) {
        if (a == 0) {
            return b;
        } else if (b == 0) {
            return a;
        }

        return Math.min(a, b);
    }

    public boolean hasLightData() {
        return (this.flags & FaceDataFlags.HAS_LIGHT_DATA) != 0;
    }

    public boolean hasUnpackedLightData() {
        return (this.flags & FaceDataFlags.HAS_UNPACKED_LIGHT_DATA) != 0;
    }

    public void reset() {
        this.flags = 0;
    }
}
