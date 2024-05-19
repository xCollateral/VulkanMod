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

    public static final int REGION_WIDTH = 16 + 2;
    public static final int BLOCKS = REGION_WIDTH * REGION_WIDTH * REGION_WIDTH;

    private final int minSecX, minSecY, minSecZ;
    private final int minX, minY, minZ;
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
        this.minX = (minSecX << 4) + 15;
        this.minZ = (minSecZ << 4) + 15;
        this.minY = (minSecY << 4) + 15;

        this.blockDataContainers = blockData;
        this.lightData = lightData;
        this.blockEntityMap = blockEntityMap;

        this.blockData = new BlockState[BLOCKS];

        this.blockStateGetter = level.isDebug() ? this::debugBlockState : this::defaultBlockState;

        this.blendRadius = Minecraft.getInstance().options.biomeBlendRadius().get();
    }

    public void loadBlockStates() {
        int maxSecX = minSecX + 2;
        int maxSecY = minSecY + 2;
        int maxSecZ = minSecZ + 2;

        int maxX = (maxSecX << 4);
        int maxZ = (maxSecZ << 4);
        int maxY = (maxSecY << 4);

        Arrays.fill(blockData, Blocks.AIR.defaultBlockState());

        for(int x = minSecX; x <= maxSecX; ++x) {
            for(int z = minSecZ; z <= maxSecZ; ++z) {
                for(int y = minSecY; y <= maxSecY; ++y) {
                    final int idx = getSectionIdx((x - minSecX), (y - minSecY), (z - minSecZ));

                    PalettedContainer<BlockState> container = blockDataContainers[idx];

                    if(container == null)
                        continue;

                    int tMinX = Math.max(minX, x << 4);
                    int tMinY = Math.max(minY, y << 4);
                    int tMinZ = Math.max(minZ, z << 4);

                    int tMaxX = Math.min(maxX, (x << 4) + 15);
                    int tMaxY = Math.min(maxY, (y << 4) + 15);
                    int tMaxZ = Math.min(maxZ, (z << 4) + 15);

                    loadSectionBlockStates(container, blockData,
                            tMinX, tMinY, tMinZ, tMaxX, tMaxY, tMaxZ);
                    
                }
            }
        }
    }

    void loadSectionBlockStates(PalettedContainer<BlockState> container, BlockState[] blockStates,
                                int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        for(int x = minX; x <= maxX; ++x) {
            for(int z = minZ; z <= maxZ; ++z) {
                for(int y = minY; y <= maxY; ++y) {
                    final int idx = getBlockIdx(x, y, z);

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
        return blockStateGetter.apply(blockPos).getFluidState();
    }

    public float getShade(Direction direction, boolean bl) {
        return this.level.getShade(direction, bl);
    }

    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
        int secX = SectionPos.blockToSectionCoord(blockPos.getX()) - this.minSecX;
        int secY = SectionPos.blockToSectionCoord(blockPos.getY()) - this.minSecY;
        int secZ = SectionPos.blockToSectionCoord(blockPos.getZ()) - this.minSecZ;

        DataLayer dataLayer = this.lightData[getSectionIdx(secX, secY, secZ)][lightLayer.ordinal()];
        return dataLayer == null ? 0 : dataLayer.get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);

    }

    public int getRawBrightness(BlockPos blockPos, int i) {
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
        x -= minX;
        y -= minY;
        z -= minZ;
        return REGION_WIDTH * ((REGION_WIDTH * y) + z) + x;
    }

    public BlockState defaultBlockState(BlockPos blockPos) {
        return blockData[getBlockIdx(blockPos.getX(), blockPos.getY(), blockPos.getZ())];
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
