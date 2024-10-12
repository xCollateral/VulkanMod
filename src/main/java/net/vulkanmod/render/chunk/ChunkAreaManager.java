package net.vulkanmod.render.chunk;

import net.minecraft.util.Mth;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.frustum.FrustumOctree;
import net.vulkanmod.render.chunk.frustum.VFrustum;
import net.vulkanmod.render.chunk.util.CircularIntList;
import net.vulkanmod.render.chunk.util.Util;
import org.joml.Vector3i;

public class ChunkAreaManager {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    public static final int AREA_SH_XZ = Util.flooredLog(WIDTH);
    public static final int AREA_SH_Y = Util.flooredLog(HEIGHT);

    public static final int SEC_SH = 4;
    public static final int BLOCK_TO_AREA_SH_XZ = AREA_SH_XZ + SEC_SH;
    public static final int BLOCK_TO_AREA_SH_Y = AREA_SH_Y + SEC_SH;

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

        int t = (width >> AREA_SH_XZ) + 2;

        int relativeHeight = height - (minHeight >> 4);
        this.ySize = (relativeHeight & 0x5) == 0 ? (relativeHeight >> AREA_SH_Y) : (relativeHeight >> AREA_SH_Y) + 1;

        //check if width is even
        if ((t & 1) == 0)
            t++;
        this.xzSize = t;
        //TODO make even size work

        this.size = xzSize * ySize * xzSize;
        this.chunkAreasArr = new ChunkArea[size];

        for (int z = 0; z < this.xzSize; ++z) {
            for (int y = 0; y < this.ySize; ++y) {
                for (int x = 0; x < this.xzSize; ++x) {
                    int idx = this.getAreaIndex(x, y, z);
                    Vector3i origin = new Vector3i(x << BLOCK_TO_AREA_SH_XZ, y << BLOCK_TO_AREA_SH_Y, z << BLOCK_TO_AREA_SH_XZ);
                    this.chunkAreasArr[idx] = new ChunkArea(idx, origin, minHeight);
                }
            }
        }

        this.prevX = Integer.MIN_VALUE;
        this.prevZ = Integer.MIN_VALUE;
    }

    public void repositionAreas(int secX, int secZ) {
        int xS = secX >> AREA_SH_XZ;
        int zS = secZ >> AREA_SH_XZ;

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
        if (deltaX >= 0) {
            xRangeStart = this.xzSize - deltaX;
            xRangeEnd = this.xzSize - 1;
            xComplStart = 0;
            xComplEnd = xRangeStart - 1;
        } else {
            xRangeStart = 0;
            xRangeEnd = -deltaX - 1;
            xComplStart = xRangeEnd;
            xComplEnd = this.xzSize - 1;
        }

        int zRangeStart;
        int zRangeEnd;
        if (deltaZ >= 0) {
            zRangeStart = this.xzSize - deltaZ;
            zRangeEnd = this.xzSize - 1;
        } else {
            zRangeStart = 0;
            zRangeEnd = -deltaZ - 1;
        }

        CircularIntList.RangeIterator xRangeIterator = xList.rangeIterator(xRangeStart, xRangeEnd);
        CircularIntList.RangeIterator xComplIterator = xList.rangeIterator(xComplStart, xComplEnd);
        CircularIntList.RangeIterator zRangeIterator = zList.rangeIterator(zRangeStart, zRangeEnd);

        xAbsChunkIndex = xS - this.xzSize / 2 + xRangeStart;
        for (int xRelativeIndex; xRangeIterator.hasNext(); xAbsChunkIndex++) {
            xRelativeIndex = xRangeIterator.next();
            int x1 = (xAbsChunkIndex << (AREA_SH_XZ + SEC_SH));

            zIterator.restart();
            zAbsChunkIndex = zS - (this.xzSize >> 1);

            for (int zRelativeIndex; zIterator.hasNext(); zAbsChunkIndex++) {
                zRelativeIndex = zIterator.next();
                int z1 = (zAbsChunkIndex << (AREA_SH_XZ + SEC_SH));

                for (int yRel = 0; yRel < this.ySize; ++yRel) {
                    int y1 = this.minHeight + (yRel << (AREA_SH_Y + SEC_SH));
                    ChunkArea chunkArea = this.chunkAreasArr[this.getAreaIndex(xRelativeIndex, yRel, zRelativeIndex)];

                    chunkArea.setPosition(x1, y1, z1);
                    chunkArea.releaseBuffers();

                }
            }
        }

        xAbsChunkIndex = xS - this.xzSize / 2 + xComplStart;
        for (int xRelativeIndex; xComplIterator.hasNext(); xAbsChunkIndex++) {
            xRelativeIndex = xComplIterator.next();
            int x1 = (xAbsChunkIndex << (AREA_SH_XZ + SEC_SH));

            zRangeIterator.restart();
            zAbsChunkIndex = zS - (this.xzSize >> 1) + zRangeStart;

            for (int zRelativeIndex; zRangeIterator.hasNext(); zAbsChunkIndex++) {
                zRelativeIndex = zRangeIterator.next();
                int z1 = (zAbsChunkIndex << (AREA_SH_XZ + SEC_SH));

                for (int yRel = 0; yRel < this.ySize; ++yRel) {
                    int y1 = this.minHeight + (yRel << (AREA_SH_Y + SEC_SH));
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

        int shX = AREA_SH_XZ + 4;
        int shY = AREA_SH_Y + 4;
        int shZ = AREA_SH_XZ + 4;

        int AreaX = x >> shX;
        int AreaY = (y - this.minHeight) >> shY;
        int AreaZ = z >> shZ;

        int x1 = Math.floorMod(AreaX, this.xzSize);
        int z1 = Math.floorMod(AreaZ, this.xzSize);

        chunkArea = this.chunkAreasArr[this.getAreaIndex(x1, AreaY, z1)];

        return chunkArea;
    }

    public ChunkArea getChunkArea(int idx) {
        return idx >= 0 && idx < chunkAreasArr.length ? this.chunkAreasArr[idx] : null;
    }

    public void updateFrustumVisibility(VFrustum frustum) {
        FrustumOctree.updateFrustumVisibility(frustum, this.chunkAreasArr);
    }

    public void resetQueues() {
        for (ChunkArea chunkArea : this.chunkAreasArr) {
            chunkArea.resetQueue();
        }
    }

    private int getAreaIndex(int x, int y, int z) {
        return (z * this.ySize + y) * this.xzSize + x;
    }

    public void releaseAllBuffers() {
        for (ChunkArea chunkArea : this.chunkAreasArr) {
            chunkArea.releaseBuffers();
        }
    }

    public String[] getStats() {
        long vbSize = 0, ibSize = 0, frag = 0;
        long vbUsed = 0, ibUsed = 0;
        int count = 0;

        for (ChunkArea chunkArea : this.chunkAreasArr) {
            DrawBuffers drawBuffers = chunkArea.drawBuffers;
            if (drawBuffers.isAllocated()) {

                var vertexBuffers = drawBuffers.getVertexBuffers();

                for (var buffer : vertexBuffers.values()) {
                    vbSize += buffer.getSize();
                    vbUsed += buffer.getUsed();
                    frag += buffer.fragmentation();
                }

                var indexBuffer = drawBuffers.getIndexBuffer();
                if (indexBuffer != null) {
                    ibSize += indexBuffer.getSize();
                    ibUsed += indexBuffer.getUsed();
                    frag += indexBuffer.fragmentation();
                }

                count++;
            }
        }

        vbSize /= 1024 * 1024;
        vbUsed /= 1024 * 1024;
        ibSize /= 1024 * 1024;
        ibUsed /= 1024 * 1024;
        frag /= 1024 * 1024;

        return new String[]{
                String.format("Vertex Buffers: %d/%d MB", vbUsed, vbSize),
                String.format("Index Buffers: %d/%d MB", ibUsed, ibSize),
                String.format("Allocations: %d Frag: %d MB", count, frag)
        };
    }

}
