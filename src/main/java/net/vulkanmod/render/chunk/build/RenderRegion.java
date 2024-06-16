package net.vulkanmod.render.chunk.build;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public class RenderRegion implements BlockAndTintGetter {
    public static final int WIDTH = 3;
    public static final int SIZE = WIDTH * WIDTH * WIDTH;

    public static final int BOUNDARY_BLOCK_WIDTH = 2;
    public static final int REGION_BLOCK_WIDTH = 16 + BOUNDARY_BLOCK_WIDTH * 2;
    public static final int BLOCK_COUNT = REGION_BLOCK_WIDTH * REGION_BLOCK_WIDTH * REGION_BLOCK_WIDTH;

    public static final BlockState AIR_BLOCK_STATE = Blocks.AIR.defaultBlockState();

    private final int minSecX, minSecY, minSecZ;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final Level level;
    private final int blendRadius;

    private final PalettedContainer<BlockState>[] blockDataContainers;
    private final BlockState[] blockData;
    private final DataLayer[][] lightData;

    private TintCache tintCache;

    private final Map<BlockPos, BlockEntity> blockEntityMap;

    private final Function<BlockPos, BlockState> blockStateGetter;

    RenderRegion(Level level, int x, int y, int z, PalettedContainer<BlockState>[] blockData, DataLayer[][] lightData, Map<BlockPos, BlockEntity> blockEntityMap) {
        this.level = level;

        this.minSecX = x - 1;
        this.minSecY = y - 1;
        this.minSecZ = z - 1;
        this.minX = (minSecX << 4) + 16 - BOUNDARY_BLOCK_WIDTH;
        this.minZ = (minSecZ << 4) + 16 - BOUNDARY_BLOCK_WIDTH;
        this.minY = (minSecY << 4) + 16 - BOUNDARY_BLOCK_WIDTH;
        this.maxX = minX + REGION_BLOCK_WIDTH;
        this.maxZ = minZ + REGION_BLOCK_WIDTH;
        this.maxY = minY + REGION_BLOCK_WIDTH;

        this.blockDataContainers = blockData;
        this.lightData = lightData;
        this.blockEntityMap = blockEntityMap;

        this.blockData = new BlockState[BLOCK_COUNT];

        this.blockStateGetter = level.isDebug() ? this::debugBlockState : this::defaultBlockState;

        this.blendRadius = Minecraft.getInstance().options.biomeBlendRadius().get();
    }

    public void loadBlockStates() {
        Arrays.fill(blockData, Blocks.AIR.defaultBlockState());

        for(int x = 0; x <= 2; ++x) {
            for(int z = 0; z <= 2; ++z) {
                for(int y = 0; y <= 2; ++y) {
                    final int idx = getSectionIdx(x, y, z);

                    PalettedContainer<BlockState> container = blockDataContainers[idx];

                    if(container == null)
                        continue;

                    int absBlockX = (x + minSecX) << 4;
                    int absBlockY = (y + minSecY) << 4;
                    int absBlockZ = (z + minSecZ) << 4;

                    int tMinX = Math.max(minX, absBlockX);
                    int tMinY = Math.max(minY, absBlockY);
                    int tMinZ = Math.max(minZ, absBlockZ);

                    int tMaxX = Math.min(maxX, absBlockX + 16);
                    int tMaxY = Math.min(maxY, absBlockY + 16);
                    int tMaxZ = Math.min(maxZ, absBlockZ + 16);

                    loadSectionBlockStates(container, blockData,
                            tMinX, tMinY, tMinZ, tMaxX, tMaxY, tMaxZ);
                    
                }
            }
        }
    }

    void loadSectionBlockStates(PalettedContainer<BlockState> container, BlockState[] blockStates,
                                int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        for (int y = minY; y < maxY; ++y) {
            for (int z = minZ; z < maxZ; ++z) {
                for (int x = minX; x < maxX; ++x) {
                    final int idx = getBlockIdx(x - this.minX, y - this.minY, z - this.minZ);

                    blockStates[idx] = container != null ?
                            container.get(x & 15, y & 15, z & 15)
                            : Blocks.AIR.defaultBlockState();
                }
            }
        }
    }

    public void initTintCache(TintCache tintCache) {
        this.tintCache = tintCache;
        this.tintCache.init(blendRadius, minSecX + 1, minSecY + 1, minSecZ + 1);
    }

    public BlockState getBlockState(BlockPos blockPos) {
        return blockStateGetter.apply(blockPos);
    }

    public FluidState getFluidState(BlockPos blockPos) {
        return this.getBlockState(blockPos).getFluidState();
    }

    public float getShade(Direction direction, boolean bl) {
        return this.level.getShade(direction, bl);
    }

    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
        if (outsideRegion(blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
            return 0;
        }

        int secX = SectionPos.blockToSectionCoord(blockPos.getX()) - this.minSecX;
        int secY = SectionPos.blockToSectionCoord(blockPos.getY()) - this.minSecY;
        int secZ = SectionPos.blockToSectionCoord(blockPos.getZ()) - this.minSecZ;

        DataLayer dataLayer = this.lightData[getSectionIdx(secX, secY, secZ)][lightLayer.ordinal()];
        return dataLayer == null ? 0 : dataLayer.get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
    }

    public int getRawBrightness(BlockPos blockPos, int i) {
        if (outsideRegion(blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
            return 0;
        }

        int secX = SectionPos.blockToSectionCoord(blockPos.getX()) - this.minSecX;
        int secY = SectionPos.blockToSectionCoord(blockPos.getY()) - this.minSecY;
        int secZ = SectionPos.blockToSectionCoord(blockPos.getZ()) - this.minSecZ;

        DataLayer[] dataLayers = this.lightData[getSectionIdx(secX, secY, secZ)];
        DataLayer skyLightLayer = dataLayers[LightLayer.SKY.ordinal()];
        DataLayer blockLightLayer = dataLayers[LightLayer.BLOCK.ordinal()];

        int relX = blockPos.getX() & 15;
        int relY = blockPos.getY() & 15;
        int relZ = blockPos.getZ() & 15;

        int skyLight = skyLightLayer == null ? 0 : skyLightLayer.get(relX, relY, relZ) - i;
        int blockLight = blockLightLayer == null ? 0 : blockLightLayer.get(relX, relY, relZ);
        return Math.max(skyLight, blockLight);
    }

    @Nullable
    public BlockEntity getBlockEntity(@NotNull BlockPos blockPos) {
        return this.blockEntityMap.get(blockPos);
    }

    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return tintCache.getColor(blockPos, colorResolver);
    }

    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    public int getHeight() {
        return this.level.getHeight();
    }

    public int getSectionIdx(int secX, int secY, int secZ) {
        return WIDTH * ((WIDTH * secY) + secZ) + secX;
    }

    public int getBlockIdx(int x, int y, int z) {
        return REGION_BLOCK_WIDTH * ((REGION_BLOCK_WIDTH * y) + z) + x;
    }

    public boolean outsideRegion(int x, int y, int z) {
        return x < minX || x >= maxX || y < minY || y >= maxY || z < minZ || z >= maxZ;
    }

    public BlockState defaultBlockState(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();

        if (outsideRegion(x, y, z)) {
            return AIR_BLOCK_STATE;
        }

        x -= minX;
        y -= minY;
        z -= minZ;

        return blockData[getBlockIdx(x, y, z)];
    }

    public BlockState debugBlockState(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();

        BlockState blockState = null;
        if (y == 60) {
            blockState = Blocks.BARRIER.defaultBlockState();
        }
        else if (y == 70) {
            blockState = DebugLevelSource.getBlockStateFor(x, z);
        }

        return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
    }
}
