package net.vulkanmod.render.chunk.build;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public class RenderRegionBuilder {
    private static final int MAX_CACHE_ENTRIES = 256;
    private final Long2ReferenceLinkedOpenHashMap<LevelChunk> levelChunkCache = new Long2ReferenceLinkedOpenHashMap<>(MAX_CACHE_ENTRIES);

    public RenderRegion createRegion(Level level, int secX, int secY, int secZ) {
        LevelChunk levelChunk = getLevelChunk(level, secX, secZ);
        var sections = levelChunk.getSections();
        LevelChunkSection section = sections[secY];

        //TODO
        if(section == null || section.hasOnlyAir())
            return null;

        var entityMap = levelChunk.getBlockEntities();

        int minSecX = secX - 1;
        int minSecZ = secZ - 1;
        int minSecY = secY - 1;
        int maxSecX = secX + 1;
        int maxSecZ = secZ + 1;
        int maxSecY = secY + 1;

        PalettedContainer<BlockState>[] sections1 = new PalettedContainer[RenderRegion.SIZE];

        for(int x = minSecX; x <= maxSecX; ++x) {
            for(int z = minSecZ; z <= maxSecZ; ++z) {
                LevelChunk levelChunk1 = getLevelChunk(level, x, z);
                sections = levelChunk1.getSections();
                for(int y = minSecY; y <= maxSecY; ++y) {
                    section = y > 0 && y < sections.length ? sections[y] : null;
                    PalettedContainer<BlockState> values = section == null || section.hasOnlyAir() ? null : section.getStates().copy();

                    final int relX = (x - minSecX), relY = (y - minSecY), relZ = (z - minSecZ);
                    sections1[(relY * RenderRegion.WIDTH + relZ) * RenderRegion.WIDTH + relX] = values;
                }
            }
        }

        return new RenderRegion(level, secX, secY, secZ, sections1, entityMap);
    }

    private LevelChunk getLevelChunk(Level level, int x, int z) {
        long l = ChunkPos.asLong(x, z);
        LevelChunk chunk = this.levelChunkCache.getAndMoveToFirst(l);

        if(chunk == null) {
            chunk = level.getChunk(x, z);

            while(levelChunkCache.size() >= MAX_CACHE_ENTRIES) {
                levelChunkCache.removeLast();
            }

            this.levelChunkCache.putAndMoveToFirst(l, chunk);
        }

        return chunk;
    }

    public void clear() {
        this.levelChunkCache.clear();
    }
}
