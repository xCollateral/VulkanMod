package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.vulkanmod.render.chunk.util.Util;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChunkAreaManager {

    static final int WIDTH = 8;
    static final int HEIGHT = 8;
    static final int DEPTH = 8;

    static final int BASE_SH_X = Util.flooredLog(WIDTH);
    static final int BASE_SH_Y = Util.flooredLog(HEIGHT);
    static final int BASE_SH_Z = Util.flooredLog(DEPTH);

    static final int LEVELS = 3;

    static final int LOWEST_LVL_SECTIONS = lowestLvlSections();

    private static int lowestLvlSections() {
        return  (WIDTH >> (LEVELS - 1)) * (HEIGHT >> (LEVELS - 1)) * (DEPTH >> (LEVELS - 1));
    }

    private List<ChunkLevel> chunkLevels = new ArrayList<>();

    public ChunkAreaManager() {
        for(int l = 0; l < LEVELS; ++l) {
            this.chunkLevels.add(new ChunkLevel(l));
        }
    }

//    public ChunkAreaManager(ViewArea viewArea, int viewDistance, Level level) {
//
//        this.setViewDistance(viewDistance, level);
//        this.generateChunkAreas(viewArea);
//    }

//    private void generateChunkAreas(ViewArea viewArea) {
//        this.viewArea = viewArea;
//
//        for(int l = 0; l < LEVELS; ++l) {
//            this.generateChunkAreaLevel(l);
//        }
//        this.setChunkAreas();
//    }

//    private void generateChunkAreaLevel(int level) {
//
//        int offset = 1 << level;
//
//        //util
////        int offsetSqr = offset << level;
////        int offsetCub = offsetSqr << level;
//        int xMul = WIDTH >> level;
//        int yMul = HEIGHT >> level;
//        int zMul = DEPTH >> level;
//
//        if(level == 0) {
//
//            for(int baseY = 0; baseY < this.chunkAreaSizeY; baseY += offset) {
//                for(int baseZ = 0; baseZ < this.chunkAreaSizeZ; baseZ += offset) {
//                    for(int baseX = 0; baseX < this.chunkAreaSizeX; baseX += offset) {
//
//                        Vector3i pos = new Vector3i(baseX, baseY, baseZ);
//
//                        int index = getChunkIndex(pos.x * xMul, pos.y * yMul, pos.z * zMul);
//                        BlockPos blockPos = this.viewArea.chunks[index].getOrigin();
//                        ChunkArea chunkArea = new ChunkArea(pos, index, level, null);
//                        this.addChunkAreaToLevel(level, chunkArea);
//
//                    }
//                }
//            }
//
//            return;
//        }
//
//        int j = 0;
//
//        for(int baseY = 0; baseY < this.chunkAreaSizeY << level; baseY += offset) {
//            for(int baseZ = 0; baseZ < this.chunkAreaSizeZ << level; baseZ += offset) {
//                for(int baseX = 0; baseX < this.chunkAreaSizeX << level; baseX += offset) {
//
//                    int prevLevel = level - 1;
//
//                    int nextX = baseX + offset;
//                    int nextY = baseY + offset;
//                    int nextZ = baseZ + offset;
//
//                    for(int AreaY = baseY >> 1; AreaY < nextY >> 1; AreaY++) {
//                        for(int AreaZ = baseZ >> 1; AreaZ < nextZ >> 1; AreaZ++) {
//                            for(int AreaX = baseX >> 1; AreaX < nextX >> 1; AreaX++) {
//
//                                ChunkArea parent = this.getChunkArea(prevLevel, AreaX, AreaY, AreaZ);
//
//                                ChunkArea[] children = new ChunkArea[8];
//                                int k = 0;
//                                for(int y = AreaY << 1; y < (AreaY + 1) << 1; y++) {
//                                    for(int z = AreaZ << 1; z < (AreaZ + 1) << 1; z++) {
//                                        for(int x = AreaX << 1; x < (AreaX + 1) << 1; x++) {
//
//                                            if(j == 65) {
//                                                System.out.println("i");
//                                            }
//
//                                            Vector3i pos = new Vector3i(x, y, z);
//
//                                            if(parent != null && (pos.x * xMul) < this.chunkGridSizeX &&
//                                            (pos.y * yMul) < this.chunkGridSizeY && (pos.z * zMul) < this.chunkGridSizeZ) {
//                                                int index = getChunkIndex(pos.x * xMul, pos.y * yMul, pos.z * zMul);
//                                                ChunkArea chunkArea = new ChunkArea(pos, index, level, parent);
//                                                this.addChunkAreaToLevel(level, chunkArea);
//                                                children[k] = chunkArea;
//                                            } else {
////                                                int index = -1;
////                                                ChunkArea chunkArea = new ChunkArea(pos, index, level, parent);
//                                                this.addChunkAreaToLevel(level, null);
//                                                children[k] = null;
//                                            }
//
//                                            ++k;
//                                            ++j;
//                                        }
//                                    }
//                                }
//                                parent.setChildren(children);
//                            }
//                        }
//                    }
//
//                }
//            }
//        }
//    }

//    private void setChunkAreas() {
//        int level = LEVELS - 1;
//        int offset = 1 << level;
//
//        //util
//        int offsetSqr = offset << level;
//        int offsetCub = offsetSqr << level;
//        int xMul = WIDTH >> level;
//        int yMul = HEIGHT >> level;
//        int zMul = DEPTH >> level;
//
//        yLabel:
//        for(int AreaY = 0; AreaY < this.chunkAreaSizeY << level; AreaY++) {
//            zLabel:
//            for(int AreaZ = 0; AreaZ < this.chunkAreaSizeZ << level; AreaZ++) {
//                xLabel:
//                for(int AreaX = 0; AreaX < this.chunkAreaSizeX << level; AreaX++) {
//
//                    ChunkArea chunkArea = getChunkArea(level, AreaX, AreaY, AreaZ);
//
//                    int baseX = AreaX * xMul;
//                    int baseY = AreaY * yMul;
//                    int baseZ = AreaZ * zMul;
//
//                    for(int y = 0; y < yMul; ++y) {
//                        int y1 = baseY + y;
////                        if(y1 >= this.chunkGridSizeY) break yLabel;
//                        if(y1 >= this.chunkGridSizeY) break;
//
//                        for(int z = 0; z < zMul; ++z) {
//                            int z1 = baseZ + z;
////                            if(z1 >= this.chunkGridSizeZ) break zLabel;
//                            if(z1 >= this.chunkGridSizeZ) break;
//
//                            for (int x = 0; x < xMul; ++x) {
//                                int x1 = baseX + x;
////                                if(x1 >= this.chunkGridSizeX) break xLabel;
//                                if(x1 >= this.chunkGridSizeX) break;
//
//                                Tree tree = new Tree(chunkArea);
//                                this.viewArea.chunks[getChunkIndex(x1, y1, z1)].setChunkAreaTree(tree);
//                            }
//                        }
//                    }
//
//                }
//            }
//        }
//    }

    public ChunkArea getChunkArea(RenderSection section, int x, int y, int z) {

        ChunkArea chunkArea = null;

        for(int level = 0; level < LEVELS; level++) {
            int shX = BASE_SH_X - level + 4;
            int shY = BASE_SH_Y - level + 4;
            int shZ = BASE_SH_Z - level + 4;
//            int level = LEVELS - sh;
            ChunkLevel chunkLevel = this.chunkLevels.get(level);

            int AreaX = x >> shX;
            int AreaY = y >> shY;
            int AreaZ = z >> shZ;

            ChunkArea parent = null;
            if(level > 0) {
                long l = Util.posLongHash(x >> (shX + 1), y >> (shY + 1), z >> (shZ + 1));
                parent = this.chunkLevels.get(level - 1).chunkAreas.get(l);
            }

            ChunkArea finalParent = parent;
            int finalLevel = level;
            long l = Util.posLongHash(AreaX, AreaY, AreaZ);
            chunkArea = chunkLevel.chunkAreas.computeIfAbsent(l,
                    (i -> {
                        Vector3i vec3i = new Vector3i(AreaX << shX, AreaY << shY, AreaZ << shZ);
                        return new ChunkArea(vec3i, finalLevel, finalParent);
                    }));

            if(level > 0) {
                int x1 = AreaX - (AreaX >> 1 << 1);
                int y1 = AreaY - (AreaY >> 1 << 1);
                int z1 = AreaZ - (AreaZ >> 1 << 1);

                int idx = 2 * (y1 * 2 + z1) + x1;

                parent.setChild(chunkArea, idx);
            }
        }

        //TODO shifts
        int sh = BASE_SH_X - (LEVELS - 1) + 4;
        int sh1 = BASE_SH_X - (LEVELS - 1);
        int areaX = x >> sh << sh1;
        int areaY = y >> sh << sh1;
        int areaZ = z >> sh << sh1;
        int width = WIDTH >> (LEVELS - 1);
        int depth = DEPTH >> (LEVELS - 1);
        int xRef = Math.abs((x >> 4) - areaX);
        int yRef = Math.abs((y >> 4) - areaY);
        int zRef = Math.abs((z >> 4) - areaZ);

        int idx = width * (yRef * depth + zRef) + xRef;
        chunkArea.setSection(section, idx);

        return chunkArea;
    }

//    private ChunkArea getChunkArea(int level, int x, int y, int z) {
//        ChunkLevel chunkLevel = this.chunkLevels.get(level);
//
//        int i;
//        if(level > 0) {
//            ChunkLevel prevLevel = this.chunkLevels.get(level - 1);
//
//            int baseX = x >> 1;
//            int baseY = y >> 1;
//            int baseZ = z >> 1;
//
//            i = (baseX + (baseZ + baseY * prevLevel.depth) * prevLevel.width) * 8 +
//                    (x & 1) + (z & 1) * 2 + (y & 1) * 4;
//        } else {
//            i = x + (z + y * chunkLevel.depth) * chunkLevel.width;
//        }
//
//        return chunkLevel.chunkAreas.get(i);
//    }

//    private void addChunkAreaToLevel(int level, ChunkArea chunkArea) {
//        this.chunkLevels.get(level).addChunkArea(chunkArea);
//    }

//    private int getChunkIndex(int x, int y, int z) {
//        return (z * this.chunkGridSizeY + y) * this.chunkGridSizeX + x;
//    }

//    private void setViewDistance(int viewDistance, Level level) {
//        int i = viewDistance * 2 + 1;
//        this.chunkGridSizeX = i;
//        this.chunkGridSizeY = level.getSectionsCount();
//        this.chunkGridSizeZ = i;
//
//        this.chunkAreaSizeX = Mth.ceil((float) this.chunkGridSizeX / WIDTH);
//        this.chunkAreaSizeY = Mth.ceil((float) this.chunkGridSizeY / HEIGHT);
//        this.chunkAreaSizeZ = Mth.ceil((float) this.chunkGridSizeZ / DEPTH);
//
//        this.chunkLevels = new ArrayList<>();
//
//        for(int l = 0; l < LEVELS; ++l) {
//            this.chunkLevels.add(new ChunkLevel(l));
//        }
//
//    }

    public void updateFrustumVisibility(VFrustum frustum) {
        for(ChunkArea chunkArea : this.chunkLevels.get(0).chunkAreas.values()) {
            chunkArea.updateFrustum(frustum);
        }
    }

    public void cleanOldAreas() {
        for(int lvl = LEVELS - 1; lvl >= 0; lvl--) {
            ChunkLevel chunkLevel = this.chunkLevels.get(lvl);

            if(lvl == LEVELS - 1) {
                for(ObjectIterator<Long2ObjectMap.Entry<ChunkArea>> it = chunkLevel.chunkAreas.long2ObjectEntrySet().fastIterator();
                    it.hasNext();) {
                    var entry = it.next();
                    ChunkArea area = entry.getValue();
                    if(area.hasNoSection()) {
                        it.remove();
                        area.parent.removeChild(area);
                    }
                }
            } else {
                for(ObjectIterator<Long2ObjectMap.Entry<ChunkArea>> it = chunkLevel.chunkAreas.long2ObjectEntrySet().fastIterator();
                    it.hasNext();) {
                    var entry = it.next();
                    ChunkArea area = entry.getValue();
                    if(area.hasNoChildren()) {
                        it.remove();
                        if(area.parent != null) area.parent.removeChild(area);
                    }
                }
            }

        }
    }

    private class ChunkLevel {
        final int level;

        Long2ObjectOpenHashMap<ChunkArea> chunkAreas;

        ChunkLevel(int level) {
            this.level = level;

            this.chunkAreas = new Long2ObjectOpenHashMap<>(100);
        }

//        void addChunkArea(ChunkArea chunkArea) {
//            this.chunkAreas.add(chunkArea);
//        }

    }

    public static class Tree {
        ChunkArea[] tree = new ChunkArea[LEVELS];

        Tree(ChunkArea chunkArea) {
            ChunkArea ref = chunkArea;
            for(int i = LEVELS - 1; i >= 0; --i) {
                tree[i] = ref;
                ref = chunkArea.parent;
            }
        }

        public int lowerVisibility() {
            return this.tree[LEVELS - 1].inFrustum();
        }

        public ChunkArea lower() {
            return this.tree[LEVELS - 1];
        }
    }

}
