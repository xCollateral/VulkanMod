package net.vulkanmod.render.chunk;

import net.minecraft.util.Mth;
import net.vulkanmod.render.chunk.util.CircularIntList;
import net.vulkanmod.render.chunk.util.Util;
import org.joml.Vector3i;

public class ChunkAreaManager {
    static final int WIDTH = 8;
    static final int HEIGHT = 8;

    static final int BASE_SH_XZ = Util.flooredLog(WIDTH);
    static final int BASE_SH_Y = Util.flooredLog(HEIGHT);

    public final int size;
    final int sectionGridWidth;
    final int xzSize;
    final int ySize;
    final int minHeight;
    final ChunkArea[] chunkAreasArr;

    int prevX;
    int prevZ;

    public ChunkAreaManager(int width, int height, int minHeight) {
        this.minHeight = minHeight;
        this.sectionGridWidth = width;

        int t = (width >> BASE_SH_XZ) + 2;

        int relativeHeight = height - (minHeight >> 4);
        this.ySize = (relativeHeight & 0x5) == 0 ? (relativeHeight >> BASE_SH_Y) : (relativeHeight >> BASE_SH_Y) + 1;

        //check if width is even
        if((t & 1) == 0)
            t++;
        this.xzSize = t;
        //TODO make even size work

        this.size = xzSize * ySize * xzSize;
        this.chunkAreasArr = new ChunkArea[size];

        for(int j = 0; j < this.xzSize; ++j) {
            for(int k = 0; k < this.ySize; ++k) {
                for(int l = 0; l < this.xzSize; ++l) {
                    int i1 = this.getAreaIndex(j, k, l);
                    Vector3i vector3i = new Vector3i(j << BASE_SH_XZ + 4, k << BASE_SH_Y + 4, l << BASE_SH_XZ + 4);
                    this.chunkAreasArr[i1] = new ChunkArea(i1, vector3i, minHeight);
                }
            }
        }

        this.prevX = Integer.MIN_VALUE;
        this.prevZ = Integer.MIN_VALUE;
    }

    public void repositionAreas(int x, int z) {
        int s = BASE_SH_XZ + 4;
        int xS = x >> s;
        int zS = z >> s;

        int deltaX = Mth.clamp(xS - this.prevX, -this.xzSize, this.xzSize);
        int deltaZ = Mth.clamp(zS - this.prevZ, -this.xzSize, this.xzSize);

        int xAbsChunkIndex = xS - this.xzSize / 2;
        int xStart = Math.floorMod(xAbsChunkIndex, this.xzSize); // needs positive modulo
        int zAbsChunkIndex = zS - this.xzSize / 2;
        int zStart = Math.floorMod(zAbsChunkIndex, this.xzSize);

        CircularIntList xList = new CircularIntList(this.xzSize, xStart);
        CircularIntList zList = new CircularIntList(this.xzSize, zStart);
        CircularIntList.OwnIterator xIterator = xList.iterator();
        CircularIntList.OwnIterator zIterator = zList.iterator();

        int xRangeStart;
        int xRangeEnd;
        int xComplStart;
        int xComplEnd;
        if(deltaX >= 0) {
            xRangeStart = this.xzSize - deltaX;
            xRangeEnd = this.xzSize - 1;
            xComplStart = 0;
            xComplEnd = xRangeStart - 1;
        } else {
            xRangeStart = 0;
            xRangeEnd = - deltaX - 1;
            xComplStart = xRangeEnd;
            xComplEnd = this.xzSize - 1;
        }

        int zRangeStart;
        int zRangeEnd;
        if(deltaZ >= 0) {
            zRangeStart = this.xzSize - deltaZ;
            zRangeEnd = this.xzSize - 1;
        } else {
            zRangeStart = 0;
            zRangeEnd = - deltaZ - 1;
        }

        CircularIntList.RangeIterator xRangeIterator = xList.rangeIterator(xRangeStart, xRangeEnd);
        CircularIntList.RangeIterator xComplIterator = xList.rangeIterator(xComplStart, xComplEnd);
        CircularIntList.RangeIterator zRangeIterator = zList.rangeIterator(zRangeStart, zRangeEnd);

        xAbsChunkIndex = xS - this.xzSize / 2 + xRangeStart;
        for(int xRelativeIndex; xRangeIterator.hasNext(); xAbsChunkIndex++) {
            xRelativeIndex = xRangeIterator.next();
            int x1 = (xAbsChunkIndex << s);

            zIterator.restart();
            zAbsChunkIndex = zS - (this.xzSize >> 1);

            for(int zRelativeIndex; zIterator.hasNext(); zAbsChunkIndex++) {
                zRelativeIndex = zIterator.next();
                int z1 = (zAbsChunkIndex << s);

                for (int yRel = 0; yRel < this.ySize; ++yRel) {
                    int y1 = this.minHeight + (yRel << s);
                    ChunkArea chunkArea = this.chunkAreasArr[this.getAreaIndex(xRelativeIndex, yRel, zRelativeIndex)];

                    chunkArea.setPosition(x1, y1, z1);
                    chunkArea.releaseBuffers();

                }
            }
        }

        xAbsChunkIndex = xS - this.xzSize / 2 + xComplStart;
        for(int xRelativeIndex; xComplIterator.hasNext(); xAbsChunkIndex++) {
            xRelativeIndex = xComplIterator.next();
            int x1 = (xAbsChunkIndex << s);

            zRangeIterator.restart();
            zAbsChunkIndex = zS - (this.xzSize >> 1) + zRangeStart;

            for(int zRelativeIndex; zRangeIterator.hasNext(); zAbsChunkIndex++) {
                zRelativeIndex = zRangeIterator.next();
                int z1 = (zAbsChunkIndex << s);

                for(int yRel = 0; yRel < this.ySize; ++yRel) {
                    int y1 = this.minHeight + (yRel << s);
                    ChunkArea chunkArea = this.chunkAreasArr[this.getAreaIndex(xRelativeIndex, yRel, zRelativeIndex)];

                    chunkArea.setPosition(x1, y1, z1);
                    chunkArea.releaseBuffers();

                }
            }
        }

        this.prevX = xS;
        this.prevZ = zS;
    }

    public ChunkArea getChunkArea(RenderSection section, int x, int y, int z) {
        ChunkArea chunkArea;

        int shX = BASE_SH_XZ + 4;
        int shY = BASE_SH_Y + 4;
        int shZ = BASE_SH_XZ + 4;

        int AreaX = x >> shX;
        int AreaY = (y - this.minHeight) >> shY;
        int AreaZ = z >> shZ;

        int x1 = Math.floorMod(AreaX, this.xzSize);
        int z1 = Math.floorMod(AreaZ, this.xzSize);

        chunkArea = this.chunkAreasArr[this.getAreaIndex(x1, AreaY, z1)];

        return chunkArea;
    }

    public void updateFrustumVisibility(VFrustum frustum) {

        for(ChunkArea chunkArea : this.chunkAreasArr) {
            chunkArea.updateFrustum(frustum);
        }
    }

    public void resetQueues() {
        for(ChunkArea chunkArea : this.chunkAreasArr) {
            chunkArea.resetQueue();
        }
    }

    private int getAreaIndex(int x, int y, int z) {
        return (z * this.ySize + y) * this.xzSize + x;
    }

    public void releaseAllBuffers() {
        for(ChunkArea chunkArea : this.chunkAreasArr) {
            chunkArea.releaseBuffers();
        }
    }

}
