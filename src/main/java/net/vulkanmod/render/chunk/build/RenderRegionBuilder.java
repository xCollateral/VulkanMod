package net.vulkanmod.render.chunk.build;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public class RenderRegionBuilder {
    private static final DataLayer DEFAULT_SKY_LIGHT_DATA_LAYER = new DataLayer(15);
    private static final DataLayer DEFAULT_BLOCK_LIGHT_DATA_LAYER = new DataLayer(0);

    private static final int MAX_CACHE_ENTRIES = 256;
    private final Long2ReferenceLinkedOpenHashMap<LevelChunk> levelChunkCache = new Long2ReferenceLinkedOpenHashMap<>(MAX_CACHE_ENTRIES);

    public RenderRegion createRegion(Level level, int secX, int secY, int secZ) {
        LevelChunk levelChunk = getLevelChunk(level, secX, secZ);
        var sections = levelChunk.getSections();
        LevelChunkSection section = sections[level.getSectionIndexFromSectionY(secY)];

        if (section == null || section.hasOnlyAir())
            return null;

        var entityMap = levelChunk.getBlockEntities();

        int minSecX = secX - 1;
        int minSecZ = secZ - 1;
        int minSecY = secY - 1;
        int maxSecX = secX + 1;
        int maxSecZ = secZ + 1;
        int maxSecY = secY + 1;

        PalettedContainer<BlockState>[] blockData = new PalettedContainer[RenderRegion.SIZE];

        DataLayer[][] lightData = new DataLayer[RenderRegion.SIZE][2 /* Light types */];

        final int minHeightSec = level.getMinBuildHeight() >> 4;
        for (int x = minSecX; x <= maxSecX; ++x) {
            for (int z = minSecZ; z <= maxSecZ; ++z) {
                LevelChunk levelChunk1 = getLevelChunk(level, x, z);
                sections = levelChunk1.getSections();

                for (int y = minSecY; y <= maxSecY; ++y) {
                    int sectionIdx = y - minHeightSec;
                    section = sectionIdx >= 0 && sectionIdx < sections.length ? sections[sectionIdx] : null;

                    final int relX = (x - minSecX), relY = (y - minSecY), relZ = (z - minSecZ);
                    final int idx = (relY * RenderRegion.WIDTH + relZ) * RenderRegion.WIDTH + relX;

                    PalettedContainer<BlockState> values = section == null || section.hasOnlyAir() ? null : section.getStates().copy();

                    blockData[idx] = values;

                    SectionPos pos = SectionPos.of(x, y, z);
                    DataLayer[] dataLayers = getSectionDataLayers(level, pos);

                    lightData[idx] = dataLayers;
                }
            }
        }

        return new RenderRegion(level, secX, secY, secZ, blockData, lightData, entityMap);
    }

    private DataLayer[] getSectionDataLayers(Level level, SectionPos pos) {
        DataLayer[] dataLayers = new DataLayer[2];

        DataLayer blockDataLayer;
        blockDataLayer = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getDataLayerData(pos);

        if (blockDataLayer == null)
            blockDataLayer = DEFAULT_BLOCK_LIGHT_DATA_LAYER;

        dataLayers[LightLayer.BLOCK.ordinal()] = blockDataLayer;

        DataLayer skyDataLayer;
        if (level.dimensionType().hasSkyLight()) {
            skyDataLayer = level.getLightEngine().getLayerListener(LightLayer.SKY).getDataLayerData(pos);

            if (skyDataLayer == null)
                skyDataLayer = DEFAULT_SKY_LIGHT_DATA_LAYER;
        } else
            skyDataLayer = null;

        dataLayers[LightLayer.SKY.ordinal()] = skyDataLayer;

        return dataLayers;
    }

    private LevelChunk getLevelChunk(Level level, int x, int z) {
        long l = ChunkPos.asLong(x, z);
        LevelChunk chunk = this.levelChunkCache.getAndMoveToFirst(l);

        if (chunk == null) {
            chunk = level.getChunk(x, z);

            while (levelChunkCache.size() >= MAX_CACHE_ENTRIES) {
                levelChunkCache.removeLast();
            }

            this.levelChunkCache.putAndMoveToFirst(l, chunk);
        }

        return chunk;
    }

    public void remove(int x, int z) {
        levelChunkCache.remove(ChunkPos.asLong(x, z));
    }

    public void clear() {
        this.levelChunkCache.clear();
    }
}
