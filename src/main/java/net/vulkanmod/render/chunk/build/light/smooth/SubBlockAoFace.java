package net.vulkanmod.render.chunk.build.light.smooth;

import net.minecraft.core.BlockPos;
import net.vulkanmod.render.chunk.util.SimpleDirection;
import net.vulkanmod.render.chunk.build.light.data.LightDataAccess;

import static net.vulkanmod.render.chunk.build.light.data.LightDataAccess.*;

public class SubBlockAoFace extends AoFaceData {

    public void initLightData(LightDataAccess cache, BlockPos pos, SimpleDirection direction, boolean offset) {
        final int oX = pos.getX();
        final int oY = pos.getY();
        final int oZ = pos.getZ();

        final int x;
        final int y;
        final int z;

        // if offset is true, requested face is outset hence light data is referring to adjacent block
        if (offset) {
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

        AoNeighborInfo aoInfo = AoNeighborInfo.get(direction);
        SimpleDirection[] faces = aoInfo.faces;

        int[] cbits = aoInfo.outCornerBits;

        // Edges
        final int e0 = cache.get(x, y, z, faces[0]);
        final int e0lm = getLightmap(e0);
        final boolean e0em = unpackEM(e0);

        final int e0co = unpackCO(e0);

        final int e1 = cache.get(x, y, z, faces[1]);
        final int e1lm = getLightmap(e1);
        final boolean e1em = unpackEM(e1);

        final int e1co = unpackCO(e1);

        final int e2 = cache.get(x, y, z, faces[2]);
        final int e2lm = getLightmap(e2);
        final boolean e2em = unpackEM(e2);

        final int e2co = unpackCO(e2);

        final int e3 = cache.get(x, y, z, faces[3]);
        final int e3lm = getLightmap(e3);
        final boolean e3em = unpackEM(e3);

        final int e3co = unpackCO(e3);

        final boolean c0oc = offset && getCornerOcclusion(unpackCO(e), cbits, 4);
        final boolean f0c0 = offset ? getCornerOcclusion(e0co, cbits, 0) : unpackFO(e0);
        final boolean f1c0 = offset ? getCornerOcclusion(e1co, cbits, 1) : unpackFO(e1);

        int d0 = cache.get(x, y, z, faces[0], faces[1]);
        boolean d0co = getCornerOcclusion(unpackCO(d0), cbits, 2);

        final int c0lm;
        float c0ao = c0oc ? 0.2f : 1.0f;
        final boolean c0em;

        if((f0c0 && f1c0) || (c0oc &&  d0co)) {
            c0ao = 1.6f;
        }
        else {
            c0ao += f0c0 ? 0.2f : 1.0f;
            c0ao += f1c0 ? 0.2f : 1.0f;

            if(offset)
                c0ao += d0co ? 0.2f : 1.0f;
            else
                c0ao += unpackFO(d0) ? 0.2f : 1.0f;
        }

        if (f0c0 && f1c0) {
            c0lm = e1lm;
            c0em = e1em;
        } else {
            c0lm = getLightmap(d0);
            c0em = unpackEM(d0);
        }

        final boolean c1oc = offset && getCornerOcclusion(unpackCO(e), cbits, 0);
        final boolean f1c1 = offset ? getCornerOcclusion(e1co, cbits, 3) : unpackFO(e1);
        final boolean f2c1 = offset ? getCornerOcclusion(e2co, cbits, 4) : unpackFO(e2);

        int d1 = cache.get(x, y, z, faces[1], faces[2]);
        boolean d1co = getCornerOcclusion(unpackCO(d1), cbits, 5);

        final int c1lm;
        float c1ao = c1oc ? 0.2f : 1.0f;
        final boolean c1em;

        if((f1c1 && f2c1) || (c1oc &&  d1co)) {
            c1ao = 1.6f;
        }
        else {
            c1ao += f1c1 ? 0.2f : 1.0f;
            c1ao += f2c1 ? 0.2f : 1.0f;

            if(offset)
                c1ao += d1co ? 0.2f : 1.0f;
            else
                c1ao += unpackFO(d1) ? 0.2f : 1.0f;
        }

        if (f1c1 && f2c1) {
            c1lm = e1lm;
            c1em = e1em;
        } else {
            c1lm = getLightmap(d1);
            c1em = unpackEM(d1);
        }

        final boolean c2oc = offset && getCornerOcclusion(unpackCO(e), cbits, 2);
        final boolean f2c2 = offset ? getCornerOcclusion(e2co, cbits, 6) : unpackFO(e2);
        final boolean f3c2 = offset ? getCornerOcclusion(e3co, cbits, 7) : unpackFO(e3);

        int d2 = cache.get(x, y, z, faces[2], faces[3]);
        boolean d2co = getCornerOcclusion(unpackCO(d2), cbits, 8);

        final int c2lm;
        float c2ao = c2oc ? 0.2f : 1.0f;
        final boolean c2em;

        if((f2c2 && f3c2) || (c2oc &&  d2co)) {
            c2ao = 1.6f;
        }
        else {
            c2ao += f2c2 ? 0.2f : 1.0f;
            c2ao += f3c2 ? 0.2f : 1.0f;

            if(offset)
                c2ao += d2co ? 0.2f : 1.0f;
            else
                c2ao += unpackFO(d2) ? 0.2f : 1.0f;
        }

        if (f2c2 && f3c2) {
            c2lm = e3lm;
            c2em = e3em;
        } else {
            c2lm = getLightmap(d2);
            c2em = unpackEM(d2);
        }

        final boolean c3oc = offset && getCornerOcclusion(unpackCO(e), cbits, 1);
        final boolean f3c3 = offset ? getCornerOcclusion(e3co, cbits, 9) : unpackFO(e3);
        final boolean f0c3 = offset ? getCornerOcclusion(e0co, cbits, 10) : unpackFO(e0);

        int d3 = cache.get(x, y, z, faces[3], faces[0]);
        boolean d3co = getCornerOcclusion(unpackCO(d3), cbits, 11);

        final int c3lm;
        float c3ao = c3oc ? 0.2f : 1.0f;
        final boolean c3em;

        if((f3c3 && f0c3) || (c3oc &&  d3co)) {
            c3ao = 1.6f;
        }
        else {
            c3ao += f3c3 ? 0.2f : 1.0f;
            c3ao += f0c3 ? 0.2f : 1.0f;

            if(offset)
                c3ao += d3co ? 0.2f : 1.0f;
            else
                c3ao += unpackFO(d3) ? 0.2f : 1.0f;
        }

        if (f3c3 && f0c3) {
            c3lm = e3lm;
            c3em = e3em;
        } else {
            c3lm = getLightmap(d3);
            c3em = unpackEM(d3);
        }

        float[] ao = this.ao;
        ao[0] = c0ao * 0.25f;
        ao[1] = c1ao * 0.25f;
        ao[2] = c2ao * 0.25f;
        ao[3] = c3ao * 0.25f;

        int[] cb = this.lm;
        cb[0] = calculateCornerBrightness(e0lm, e1lm, c0lm, olm, e0em, e1em, c0em, oem);
        cb[1] = calculateCornerBrightness(e1lm, e2lm, c1lm, olm, e1em, e2em, c1em, oem);
        cb[2] = calculateCornerBrightness(e2lm, e3lm, c2lm, olm, e2em, e3em, c2em, oem);
        cb[3] = calculateCornerBrightness(e3lm, e0lm, c3lm, olm, e3em, e0em, c3em, oem);

        this.flags |= FaceDataFlags.HAS_LIGHT_DATA;
    }

    public void calculateSelfOcclusion(LightDataAccess cache, BlockPos pos, SimpleDirection direction) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final int e = cache.get(x, y, z);

        AoNeighborInfo aoInfo = AoNeighborInfo.get(direction);

        int[] cbits = aoInfo.inCornerBits;

        final int co = unpackCO(e);
        final boolean c0oc = getCornerOcclusion(co, cbits, 6);
        final boolean c1oc = getCornerOcclusion(co, cbits, 4);
        final boolean c2oc = getCornerOcclusion(co, cbits, 5);
        final boolean c3oc = getCornerOcclusion(co, cbits, 7);

        float c0ao = c0oc ? 0.25f : 1.0f;
        float c1ao = c1oc ? 0.25f : 1.0f;
        float c2ao = c2oc ? 0.25f : 1.0f;
        float c3ao = c3oc ? 0.25f : 1.0f;

        float[] ao = this.ao;
        ao[0] = c0ao;
        ao[1] = c1ao;
        ao[2] = c2ao;
        ao[3] = c3ao;
    }

    public void calculatePartialAlignedFace(LightDataAccess cache, BlockPos pos, SimpleDirection direction) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final int e = cache.get(x, y, z);

        final int x2 = x + direction.getStepX();
        final int y2 = y + direction.getStepY();
        final int z2 = z + direction.getStepZ();

        final int eb = cache.get(x2, y2, z2);

        AoNeighborInfo aoInfo = AoNeighborInfo.get(direction);
        SimpleDirection[] faces = aoInfo.faces;

        int[] cbits = aoInfo.inCornerBits;

        final int co = unpackCO(e);
        final int cob = unpackCO(eb);

        final boolean c0oc = getCornerOcclusion(cob, cbits, 2) || getCornerOcclusion(co, cbits, 6);
        final boolean c1oc = getCornerOcclusion(cob, cbits, 0) || getCornerOcclusion(co, cbits, 4);
        final boolean c2oc = getCornerOcclusion(cob, cbits, 1) || getCornerOcclusion(co, cbits, 5);
        final boolean c3oc = getCornerOcclusion(cob, cbits, 3) || getCornerOcclusion(co, cbits, 7);

        float c0ao = c0oc ? 0.0f : 3.0f;
        float c1ao = c1oc ? 0.0f : 3.0f;
        float c2ao = c2oc ? 0.0f : 3.0f;
        float c3ao = c3oc ? 0.0f : 3.0f;

        // Edges
        cbits = aoInfo.outCornerBits;

        final int e1 = cache.get(x2, y2, z2, faces[1]);
        final int e1co = unpackCO(e1);
        c0ao += getCornerOcclusion(e1co, cbits, 1) ? 0.0f : 1.0f;
        c1ao += getCornerOcclusion(e1co, cbits, 3) ? 0.0f : 1.0f;

        final int e3 = cache.get(x2, y2, z2, faces[3]);
        final int e3co = unpackCO(e3);
        c2ao += getCornerOcclusion(e3co, cbits, 0) ? 0.0f : 1.0f;
        c3ao += getCornerOcclusion(e3co, cbits, 4) ? 0.0f : 1.0f;

        float[] ao = this.ao;
        ao[0] = c0ao * 0.25f;
        ao[1] = c1ao * 0.25f;
        ao[2] = c2ao * 0.25f;
        ao[3] = c3ao * 0.25f;
    }
}
