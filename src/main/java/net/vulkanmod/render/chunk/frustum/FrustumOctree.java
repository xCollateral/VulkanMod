package net.vulkanmod.render.chunk.frustum;

import net.vulkanmod.render.chunk.ChunkArea;
import org.joml.FrustumIntersection;

import java.util.Arrays;

import static net.vulkanmod.render.chunk.ChunkAreaManager.AREA_SH_XZ;

public class FrustumOctree {
    static final int LEVELS = 2;

    public static void updateFrustumVisibility(VFrustum frustum, ChunkArea[] chunkAreas) {
        int width = 1 << (AREA_SH_XZ + 4);

        for (ChunkArea chunkArea : chunkAreas) {
            var position = chunkArea.getPosition();
            int minX2 = position.x;
            int minY2 = position.y;
            int minZ2 = position.z;

            int frustumResult = frustum.cubeInFrustum(minX2, minY2, minZ2,
                    minX2 + width, minY2 + width, minZ2 + width);

            byte[] buffer = chunkArea.getFrustumBuffer();

            if (frustumResult != FrustumIntersection.INTERSECT)
                Arrays.fill(buffer, (byte) frustumResult);
            else
                innerCube(frustum, buffer, LEVELS, minX2, minY2, minZ2, width, 0);
        }
    }

    static void innerCube(VFrustum frustum, byte[] buffer, int level,
                          float xMin, float yMin, float zMin,
                          int prevWidth, int beginIdx) {

        if (level == 1) {
            lastInnerCube(frustum, buffer, xMin, yMin, zMin, prevWidth, beginIdx);
            return;
        }

        int width = prevWidth >> 1;
        final int lvlShift = (level - 1) * 3;

        for (int x = 0; x < 2; x++) {
            float xMin2 = xMin + x * width;
            float xMax2 = xMin2 + width;

            for (int y = 0; y < 2; y++) {
                float yMin2 = yMin + y * width;
                float yMax2 = yMin2 + width;

                for (int z = 0; z < 2; z++) {
                    float zMin2 = zMin + z * width;
                    float zMax2 = zMin2 + width;

                    int frustumResult = frustum.cubeInFrustum(xMin2, yMin2, zMin2,
                            xMax2, yMax2, zMax2);

                    int idx = beginIdx + getOffset(lvlShift, x, y, z);
                    int endIdx = idx + (1 << lvlShift);

                    if (frustumResult != FrustumIntersection.INTERSECT)
                        fillResultBuffer(buffer, idx, endIdx, (byte) frustumResult);
                    else
                        innerCube(frustum, buffer, level - 1, xMin2, yMin2, zMin2, width, idx);
                }

            }
        }
    }

    static void lastInnerCube(VFrustum frustum, byte[] buffer,
                              float xMin, float yMin, float zMin,
                              int prevWidth, int beginIdx) {
        int width = prevWidth >> 1;

        for (int x = 0; x < 2; x++) {
            float xMin2 = xMin + x * width;
            float xMax2 = xMin2 + width;

            for (int y = 0; y < 2; y++) {
                float yMin2 = yMin + y * width;
                float yMax2 = yMin2 + width;

                for (int z = 0; z < 2; z++) {
                    float zMin2 = zMin + z * width;
                    float zMax2 = zMin2 + width;

                    final int frustumResult = frustum.cubeInFrustum(xMin2, yMin2, zMin2,
                            xMax2, yMax2, zMax2);

                    int idx = beginIdx + (x << 2) + (y << 1) + z;

                    buffer[idx] = (byte) frustumResult;
                }

            }
        }

    }

    static int getOffset(int baseShift, int x, int y, int z) {
        return ((x << (2 + baseShift)) + (y << (1 + baseShift)) + (z << (baseShift)));
    }

    static void fillResultBuffer(byte[] buffer, int beginIdx, int endIdx, byte result) {
        for (int i = beginIdx; i < endIdx; ++i) {
            buffer[i] = result;
        }
    }
}
