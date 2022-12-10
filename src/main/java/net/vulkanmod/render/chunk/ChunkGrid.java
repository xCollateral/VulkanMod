package net.vulkanmod.render.chunk;

import net.minecraft.client.Minecraft;
import net.vulkanmod.render.chunk.util.CircularIntList;
import net.vulkanmod.render.chunk.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ChunkGrid {

    protected final Level level;
    protected int chunkGridSizeY;
    protected int chunkGridSizeX;
    protected int chunkGridSizeZ;
    public RenderSection[] chunks;
    private ChunkAreaManager chunkAreaManager;

    private int prevSecX;
    private int prevSecZ;

    public ChunkGrid(Level level, int viewDistance) {
        this.level = level;
        this.setViewDistance(viewDistance);
        this.createChunks();
        this.chunkAreaManager = new ChunkAreaManager();

        this.prevSecX = Integer.MIN_VALUE;
        this.prevSecZ = Integer.MIN_VALUE;
    }

    protected void createChunks() {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("createChunks called from wrong thread: " + Thread.currentThread().getName());
        } else {
            int i = this.chunkGridSizeX * this.chunkGridSizeY * this.chunkGridSizeZ;
            this.chunks = new RenderSection[i];

            for(int j = 0; j < this.chunkGridSizeX; ++j) {
                for(int k = 0; k < this.chunkGridSizeY; ++k) {
                    for(int l = 0; l < this.chunkGridSizeZ; ++l) {
                        int i1 = this.getChunkIndex(j, k, l);
                        RenderSection renderSection = new RenderSection(i1, j * 16, k * 16, l * 16);
                        this.chunks[i1] = renderSection;
                    }
                }
            }
            this.setYNeighbours();
        }
    }

    public void releaseAllBuffers() {
        for(RenderSection renderSection : this.chunks) {
            renderSection.releaseBuffers();
        }

    }

    private int getChunkIndex(int x, int y, int z) {
        return (z * this.chunkGridSizeY + y) * this.chunkGridSizeX + x;
    }

    protected void setViewDistance(int p_110854_) {
        int i = p_110854_ * 2 + 1;
        this.chunkGridSizeX = i;
        this.chunkGridSizeY = this.level.getSectionsCount();
        this.chunkGridSizeZ = i;
    }

    public void repositionCamera(double x, double z) {
//        int i = Mth.ceil(x);
//        int j = Mth.ceil(z);
//
//      int count = 0;
//      for(int k = 0; k < this.chunkGridSizeX; ++k) {
//         int l = this.chunkGridSizeX * 16;
//         int i1 = i - 8 - l / 2;
//         int j1 = i1 + Math.floorMod(k * 16 - i1, l);
//
////         i = i;
//         for(int k1 = 0; k1 < this.chunkGridSizeZ; ++k1) {
//            int l1 = this.chunkGridSizeZ * 16;
//            int i2 = j - 8 - l1 / 2;
//            int j2 = i2 + Math.floorMod(k1 * 16 - i2, l1);
//
//            for(int k2 = 0; k2 < this.chunkGridSizeY; ++k2) {
//               int l2 = this.level.getMinBuildHeight() + k2 * 16;
//               RenderSection renderSection = this.chunks[this.getChunkIndex(k, k2, k1)];
//               BlockPos blockpos = renderSection.getOrigin();
//               if (j1 != blockpos.getX() || l2 != blockpos.getY() || j2 != blockpos.getZ()) {
//                  renderSection.setOrigin(j1, l2, j2);
////                  count++;
//               }
//
//                this.setChunkArea(renderSection, j1, l2, j2);
////               else {
////                  i=i;
////               }
//            }
//         }
//      }

//      count = count;

        int i = Mth.floor(x);
        int j = Mth.floor(z);

        int deltaX = Mth.clamp((i >> 4) - this.prevSecX, -this.chunkGridSizeX, this.chunkGridSizeX);
        int deltaZ = Mth.clamp((j >> 4) - this.prevSecZ, - this.chunkGridSizeZ, this.chunkGridSizeZ);

        int xAbsChunkIndex = (i >> 4) - this.chunkGridSizeX / 2;
        int xStart = Math.floorMod(xAbsChunkIndex, this.chunkGridSizeX); // needs positive modulo
        int zAbsChunkIndex = (j >> 4) - this.chunkGridSizeZ / 2;
        int zStart = Math.floorMod(zAbsChunkIndex, this.chunkGridSizeZ);

        CircularIntList xList = new CircularIntList(this.chunkGridSizeX, xStart);
        CircularIntList zList = new CircularIntList(this.chunkGridSizeZ, zStart);
        CircularIntList.OwnIterator xIterator = xList.iterator();
        CircularIntList.OwnIterator zIterator = zList.iterator();

        int xRangeStart;
        int xRangeEnd;
        int xComplStart;
        int xComplEnd;
        if(deltaX >= 0) {
            xRangeStart = this.chunkGridSizeX - deltaX;
            xRangeEnd = this.chunkGridSizeX - 1;
            xComplStart = 0;
            xComplEnd = xRangeStart - 1;
        } else {
            xRangeStart = 0;
            xRangeEnd = - deltaX - 1;
            xComplStart = xRangeEnd;
            xComplEnd = this.chunkGridSizeX - 1;
        }

        int zRangeStart;
        int zRangeEnd;
        if(deltaZ >= 0) {
            zRangeStart = this.chunkGridSizeZ - deltaZ;
            zRangeEnd = this.chunkGridSizeZ - 1;
        } else {
            zRangeStart = 0;
            zRangeEnd = - deltaZ - 1;
        }

        CircularIntList.RangeIterator xRangeIterator = xList.rangeIterator(xRangeStart, xRangeEnd);
        CircularIntList.RangeIterator xComplIterator = xList.rangeIterator(xComplStart, xComplEnd);
        CircularIntList.RangeIterator zRangeIterator = zList.rangeIterator(zRangeStart, zRangeEnd);

        xAbsChunkIndex = (i >> 4) - this.chunkGridSizeX / 2 + xRangeStart;
        for(int xRelativeIndex; xRangeIterator.hasNext(); xAbsChunkIndex++) {
            xRelativeIndex = xRangeIterator.next();
            int x1 = (xAbsChunkIndex << 4);

            zIterator.restart();
            zAbsChunkIndex = (j >> 4) - (this.chunkGridSizeZ >> 1);

            for(int zRelativeIndex; zIterator.hasNext(); zAbsChunkIndex++) {
                zRelativeIndex = zIterator.next();
                int z1 = (zAbsChunkIndex << 4);

                for (int yRel = 0;
                     yRel < this.chunkGridSizeY; ++yRel) {
                    int y1 = this.level.getMinBuildHeight() + (yRel << 4);
                    RenderSection renderSection = this.chunks[this.getChunkIndex(xRelativeIndex, yRel, zRelativeIndex)];
                    BlockPos blockpos = renderSection.getOrigin();

                    //maybe later no need to check
                    if (x1 != blockpos.getX() || y1 != blockpos.getY() || z1 != blockpos.getZ()) {
                        renderSection.setOrigin(x1, y1, z1);
//                        count++;
                    }

                    this.unsetNeighbours(renderSection);
                    this.unbindChunkArea(renderSection);

                    this.setNeighbours(renderSection, xList, zList, xRangeIterator.getCurrentIndex(), zIterator.getCurrentIndex(),
                            xRelativeIndex, yRel, zRelativeIndex);

                    this.setChunkArea(renderSection, x1, y1, z1);

                }
            }
        }

        xAbsChunkIndex = (i >> 4) - this.chunkGridSizeX / 2 + xComplStart;
        for(int xRelativeIndex; xComplIterator.hasNext(); xAbsChunkIndex++) {
            xRelativeIndex = xComplIterator.next();
            int x1 = (xAbsChunkIndex << 4);

            zRangeIterator.restart();
            zAbsChunkIndex = (j >> 4) - (this.chunkGridSizeZ >> 1) + zRangeStart;

            for(int zRelativeIndex; zRangeIterator.hasNext(); zAbsChunkIndex++) {
                zRelativeIndex = zRangeIterator.next();
                int z1 = (zAbsChunkIndex << 4);

                for(int yRel = 0;
                    yRel < this.chunkGridSizeY; ++yRel) {
                    int y1 = this.level.getMinBuildHeight() + (yRel << 4);
                    RenderSection renderSection = this.chunks[this.getChunkIndex(xRelativeIndex, yRel, zRelativeIndex)];
                    BlockPos blockpos = renderSection.getOrigin();

                    //maybe later no need to check
                    if (x1 != blockpos.getX() || y1 != blockpos.getY() || z1 != blockpos.getZ()) {
                        renderSection.setOrigin(x1, y1, z1);
//                        count++;
                    }

                    this.unsetNeighbours(renderSection);
                    this.unbindChunkArea(renderSection);

                    this.setNeighbours(renderSection, xList, zList, xComplIterator.getCurrentIndex(), zRangeIterator.getCurrentIndex(),
                            xRelativeIndex, yRel, zRelativeIndex);

                    this.setChunkArea(renderSection, x1, y1, z1);

                }
            }
        }



//        int i = Mth.ceil(x);
//        int j = Mth.ceil(z);
//
//        int xAbsChunkIndex = (i >> 4) - this.chunkGridSizeX / 2;
//        int xStart = Math.floorMod(xAbsChunkIndex, this.chunkGridSizeX); // needs positive modulo
//        int zAbsChunkIndex = (j >> 4) - this.chunkGridSizeZ / 2;
//        int zStart = Math.floorMod(zAbsChunkIndex, this.chunkGridSizeZ);
//
//        CircularIntList xList = new CircularIntList(this.chunkGridSizeX, xStart);
//        CircularIntList zList = new CircularIntList(this.chunkGridSizeZ, zStart);
//        CircularIntList.OwnIterator xIterator = xList.iterator();
//        CircularIntList.OwnIterator zIterator = zList.iterator();
//
//        int count = 0;
//        for(int xRelativeIndex; xIterator.hasNext(); xAbsChunkIndex++) {
//            xRelativeIndex = xIterator.next();
//            int x1 = (xAbsChunkIndex << 4);
//
//            zIterator.restart();
//            zAbsChunkIndex = (j >> 4) - (this.chunkGridSizeZ >> 1);
//
//            for(int zRelativeIndex; zIterator.hasNext(); zAbsChunkIndex++) {
//                zRelativeIndex = zIterator.next();
//                int z1 = (zAbsChunkIndex << 4);
//
//                for(int yRel = 0;
//                    yRel < this.chunkGridSizeY; ++yRel) {
//                    int y1 = this.level.getMinBuildHeight() + (yRel << 4);
//                    RenderSection renderSection = this.chunks[this.getChunkIndex(xRelativeIndex, yRel, zRelativeIndex)];
//                    BlockPos blockpos = renderSection.getOrigin();
//
//                    //maybe later no need to check
//                    if (x1 != blockpos.getX() || y1 != blockpos.getY() || z1 != blockpos.getZ()) {
//                        renderSection.setOrigin(x1, y1, z1);
//                        count++;
//                    }
//
//                    this.setNeighbours(renderSection, xList, zList, xIterator.getCurrentIndex(), zIterator.getCurrentIndex(),
//                            xRelativeIndex, yRel, zRelativeIndex);
//
//                    this.setChunkArea(renderSection, x1, y1, z1);
//
//                }
//            }
//        }

        this.chunkAreaManager.cleanOldAreas();

        this.prevSecX = i >> 4;
        this.prevSecZ = j >> 4;
    }

    //TODO: delete
    public void repositionCameraOld(double x, double z) {
        int i = Mth.ceil(x);
        int j = Mth.ceil(z);

//      int count = 0;
//      for(int k = 0; k < this.chunkGridSizeX; ++k) {
//         int l = this.chunkGridSizeX * 16;
//         int i1 = i - 8 - l / 2;
//         int j1 = i1 + Math.floorMod(k * 16 - i1, l);
//
////         i = i;
//         for(int k1 = 0; k1 < this.chunkGridSizeZ; ++k1) {
//            int l1 = this.chunkGridSizeZ * 16;
//            int i2 = j - 8 - l1 / 2;
//            int j2 = i2 + Math.floorMod(k1 * 16 - i2, l1);
//
//            for(int k2 = 0; k2 < this.chunkGridSizeY; ++k2) {
//               int l2 = this.level.getMinBuildHeight() + k2 * 16;
//               ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = this.chunks[this.getChunkIndex(k, k2, k1)];
//               BlockPos blockpos = chunkrenderdispatcher$renderchunk.getOrigin();
//               if (j1 != blockpos.getX() || l2 != blockpos.getY() || j2 != blockpos.getZ()) {
//                  chunkrenderdispatcher$renderchunk.setOrigin(j1, l2, j2);
////                  count++;
//               }
////               else {
////                  i=i;
////               }
//            }
//         }
//      }

//      count = count;

        int deltaX = i - this.prevSecX;
        int deltaZ = j - this.prevSecZ;

        i = Mth.floor(x);
        j = Mth.floor(z);

        int xAbsChunkIndex = (i >> 4) - this.chunkGridSizeX / 2;
        int xStart = Math.floorMod(xAbsChunkIndex, this.chunkGridSizeX); // needs positive modulo
        int zAbsChunkIndex = (j >> 4) - this.chunkGridSizeZ / 2;
        int zStart = Math.floorMod(zAbsChunkIndex, this.chunkGridSizeZ);

        CircularIntList xList = new CircularIntList(this.chunkGridSizeX, xStart);
        CircularIntList zList = new CircularIntList(this.chunkGridSizeZ, zStart);
        CircularIntList.OwnIterator xIterator = xList.iterator();
        CircularIntList.OwnIterator zIterator = zList.iterator();

        int count = 0;
        for(int xRelativeIndex; xIterator.hasNext(); xAbsChunkIndex++) {
            xRelativeIndex = xIterator.next();
            int x1 = (xAbsChunkIndex << 4);

            zIterator.restart();
            zAbsChunkIndex = (j >> 4) - (this.chunkGridSizeZ >> 1);

            for(int zRelativeIndex; zIterator.hasNext(); zAbsChunkIndex++) {
                zRelativeIndex = zIterator.next();
                int z1 = (zAbsChunkIndex << 4);

                for(int yRel = 0;
                    yRel < this.chunkGridSizeY; ++yRel) {
                    int y1 = this.level.getMinBuildHeight() + (yRel << 4);
                    RenderSection renderSection = this.chunks[this.getChunkIndex(xRelativeIndex, yRel, zRelativeIndex)];
                    BlockPos blockpos = renderSection.getOrigin();

                    //maybe later no need to check
                    if (x1 != blockpos.getX() || y1 != blockpos.getY() || z1 != blockpos.getZ()) {
//                        renderSection.setOrigin(x1, y1, z1);
                        count++;
                    }

//                    this.setNeighbours(renderSection, xList, zList, xIterator.getCurrentIndex(), zIterator.getCurrentIndex(),
//                            xRelativeIndex, yRel, zRelativeIndex);
//
//                    this.setChunkArea(renderSection, x1, y1, z1);

                }
            }
        }

        this.prevSecX = i;
        this.prevSecZ = j;
    }

    private void setNeighbours(RenderSection section, CircularIntList xList, CircularIntList zList,
                               int xIdx, int zIdx, int x, int y, int z) {
        //TODO: maybe connect neighbours on section compile

        int eastX = xList.getNext(xIdx);
        int westX = xList.getPrevious(xIdx);
        int northZ = zList.getPrevious(zIdx);
        int southZ = zList.getNext(zIdx);

        if(eastX != -1) {
            RenderSection neighbour = this.chunks[getChunkIndex(eastX, y, z)];
            section.setNeighbour(Direction.EAST.ordinal(), neighbour);
            neighbour.setNeighbour(Direction.WEST.ordinal(), section);
        }

        if(westX != -1) {
            RenderSection neighbour = this.chunks[getChunkIndex(westX, y, z)];
            section.setNeighbour(Direction.WEST.ordinal(), neighbour);
            neighbour.setNeighbour(Direction.EAST.ordinal(), section);
        }

        if(northZ != -1) {
            RenderSection neighbour = this.chunks[getChunkIndex(x, y, northZ)];
            section.setNeighbour(Direction.NORTH.ordinal(), neighbour);
            neighbour.setNeighbour(Direction.SOUTH.ordinal(), section);
        }

        if(southZ != -1) {
            RenderSection neighbour = this.chunks[getChunkIndex(x, y, southZ)];
            section.setNeighbour(Direction.SOUTH.ordinal(), neighbour);
            neighbour.setNeighbour(Direction.NORTH.ordinal(), section);
        }
    }

    private void unsetNeighbours(RenderSection section) {

        for(Direction dir : Util.XZ_DIRECTIONS) {
//        for(Direction dir : Util.DIRECTIONS) {
            RenderSection neighbour = section.getNeighbour(dir.ordinal());
            if(neighbour != null)
                neighbour.setNeighbour(dir.getOpposite().ordinal(), null);
        }

    }

    private void setYNeighbours() {
        for(int j = 0; j < this.chunkGridSizeX; ++j) {
            for(int k = 0; k < this.chunkGridSizeY; ++k) {
                for(int l = 0; l < this.chunkGridSizeZ; ++l) {
                    int i1 = this.getChunkIndex(j, k, l);
                    this.setYNeighbours(this.chunks[i1], j, k, l);
                }
            }
        }
    }

    private void setYNeighbours(RenderSection section, int x, int y, int z) {
        if(y != this.chunkGridSizeY - 1) {
            RenderSection neighbour = this.chunks[getChunkIndex(x, y + 1, z)];
            section.setNeighbour(Direction.UP.ordinal(), neighbour);
            neighbour.setNeighbour(Direction.DOWN.ordinal(), section);
        }

        if(y != 0) {
            RenderSection neighbour = this.chunks[getChunkIndex(x , y - 1, z)];
            section.setNeighbour(Direction.DOWN.ordinal(), neighbour);
            neighbour.setNeighbour(Direction.UP.ordinal(), section);
        }
    }

    private void unbindChunkArea(RenderSection section) {
        ChunkArea chunkArea = section.getChunkArea();
        if(chunkArea != null) chunkArea.unbindSection(section);
    }

    private void setChunkArea(RenderSection section, int x, int y, int z) {
//        section.setChunkArea(this.chunkAreaManager.getChunkAreaV2(x, y, z));
        //debug
        ChunkArea chunkArea = this.chunkAreaManager.getChunkArea(section, x, y, z);
//        if(!chunkArea.isSectionPresent(section)) chunkArea.addSection(section);
        section.setChunkArea(chunkArea);
    }

    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean p_110863_) {
        int i = Math.floorMod(sectionX, this.chunkGridSizeX);
        int j = Math.floorMod(sectionY - this.level.getMinSection(), this.chunkGridSizeY);
        int k = Math.floorMod(sectionZ, this.chunkGridSizeZ);
        RenderSection renderSection = this.chunks[this.getChunkIndex(i, j, k)];
        renderSection.setDirty(p_110863_);
    }

    @Nullable
    protected RenderSection getRenderChunkAt(BlockPos blockPos) {
//      int i = Mth.intFloorDiv(blockPos.getX(), 16);
//      int j = Mth.intFloorDiv(blockPos.getY() - this.level.getMinBuildHeight(), 16);
//      int k = Mth.intFloorDiv(blockPos.getZ(), 16);

        int i = blockPos.getX() >> 4;
        int j = (blockPos.getY() - this.level.getMinBuildHeight()) >> 4;
        int k = blockPos.getZ() >> 4;

        if (j >= 0 && j < this.chunkGridSizeY) {
            i = Mth.positiveModulo(i, this.chunkGridSizeX);
            k = Mth.positiveModulo(k, this.chunkGridSizeZ);
            return this.chunks[this.getChunkIndex(i, j, k)];
        } else {
            return null;
        }
    }

    public void updateFrustumVisibility(VFrustum frustum) {
        this.chunkAreaManager.updateFrustumVisibility(frustum);
    }
}
