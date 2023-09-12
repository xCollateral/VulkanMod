package net.vulkanmod.render.chunk;

import net.minecraft.core.BlockPos;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import org.joml.FrustumIntersection;
import org.joml.Vector3i;

import java.util.Arrays;

public class ChunkArea {
    public final int index;
    private final byte[] inFrustum = new byte[64];

    final Vector3i position;

    DrawBuffers drawBuffers;

    final ResettableQueue<RenderSection> sectionQueue = new ResettableQueue<>();

    public ChunkArea(int i, Vector3i origin) {
        this.index = i;
        this.position = origin;
        this.drawBuffers = new DrawBuffers();
    }

    public void updateFrustum(VFrustum frustum) {
        //TODO: maybe move to an aux class
        int frustumResult = frustum.cubeInFrustum(this.position.x(), this.position.y(), this.position.z(),
                this.position.x() + (8 << 4) , this.position.y() + (8 << 4), this.position.z() + (8 << 4));

        //Inner cubes
        if (frustumResult == FrustumIntersection.INTERSECT) {
            int width = 8 << 4;
            int l = width >> 1;

            for(int x1 = 0; x1 < 2; x1++) {
                float xMin = this.position.x() + (x1 * l);
                float xMax = xMin + l;
                for(int y1 = 0; y1 < 2; y1++) {
                    float yMin = this.position.y() + (y1 * l);
                    float yMax = yMin + l;
                    for (int z1 = 0; z1 < 2; z1++) {
                        float zMin = this.position.z() + (z1 * l);
                        float zMax = zMin + l;

                        frustumResult = frustum.cubeInFrustum(xMin, yMin, zMin,
                                xMax , yMax, zMax);

                        int beginIdx = (x1 << 5) + (y1 << 4) + (z1 << 3);
                        if (frustumResult == FrustumIntersection.INTERSECT) {
                            int l2 = width >> 2;
                            for (int x2 = 0; x2 < 2; x2++) {
                                float xMin2 = xMin + x2 * l2;
                                float xMax2 = xMin2 + l2;
                                for (int y2 = 0; y2 < 2; y2++) {
                                    float yMin2 = yMin + y2 * l2;
                                    float yMax2 = yMin2 + l2;
                                    for (int z2 = 0; z2 < 2; z2++) {
                                        float zMin2 = zMin + z2 * l2;
                                        float zMax2 = zMin2 + l2;

                                        frustumResult = frustum.cubeInFrustum(xMin2, yMin2, zMin2,
                                                xMax2, yMax2, zMax2);

                                        int idx = beginIdx + (x2 << 2) + (y2 << 1) + z2;

                                        this.inFrustum[idx] = (byte) frustumResult;
                                    }

                                }
                            }
                        }
                        else {
                            int end = beginIdx + 8;

                            for(int i = beginIdx; i < end; ++i) {
                                this.inFrustum[i] = (byte) frustumResult;
                            }
                        }

                    }
                }
            }
        } else {
            Arrays.fill(inFrustum, (byte) frustumResult);
        }

    }

    public byte getFrustumIndex(BlockPos pos) {
        return getFrustumIndex(pos.getX(), pos.getY(), pos.getZ());
    }

    public byte getFrustumIndex(int x, int y, int z) {
        int dx = x - this.position.x;
        int dy = y - this.position.y;
        int dz = z - this.position.z;

        int i = (dx >> 6 << 5)
                + (dy >> 6 << 4)
                + (dz >> 6 << 3);

        int xSub = (dx >> 3) & 0b100;
        int ySub = (dy >> 4) & 0b10;
        int zSub = (dz >> 5) & 0b1;

        return (byte) (i + xSub + ySub + zSub);
    }

    public byte inFrustum(byte i) {
        return this.inFrustum[i];
    }

    public DrawBuffers getDrawBuffers() {
        if(!this.drawBuffers.isAllocated())
            drawBuffers.allocateBuffers();

        return this.drawBuffers;
    }

    private void allocateDrawBuffers() {
        this.drawBuffers = new DrawBuffers();
    }

    public void addSection(RenderSection section) {
        this.sectionQueue.add(section);
    }

    public void resetQueue() {
        this.sectionQueue.clear();
    }

    public void setPosition(int x, int y, int z) {
        this.position.set(x, y, z);
    }

    public void releaseBuffers() {
        this.drawBuffers.releaseBuffers();
    }
}
