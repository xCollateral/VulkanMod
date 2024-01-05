package net.vulkanmod.render.chunk.build;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public class RenderRegion implements BlockAndTintGetter {
    public static final int WIDTH = 3;
    public static final int SIZE = WIDTH * WIDTH * WIDTH;

    private final int minSectionX, minSectionY, minSectionZ;
    private final PalettedContainer<BlockState>[] sections;
    private final Level level;
    private final int blendRadius;

    private TintCache tintCache;

    private final Map<BlockPos, BlockEntity> blockEntityMap;
    private final int minHeight;

    private final Function<BlockPos, BlockState> blockStateGetter;

    RenderRegion(Level level, int x, int y, int z, PalettedContainer<BlockState>[] sections, Map<BlockPos, BlockEntity> blockEntityMap) {
        this.level = level;
        this.minHeight = level.getMinBuildHeight();
        this.minSectionX = x - 1;
        this.minSectionY = y - 1;
        this.minSectionZ = z - 1;
        this.sections = sections;
        this.blockEntityMap = blockEntityMap;

        this.blockStateGetter = level.isDebug() ? this::debugBlockState : this::defaultBlockState;

        this.blendRadius = Minecraft.getInstance().options.biomeBlendRadius().get();
    }

    public void initTintCache(TintCache tintCache) {
        this.tintCache = tintCache;
        this.tintCache.init(blendRadius, minSectionX + 1, minSectionY + 1, minSectionZ + 1);
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

    public BlockState defaultBlockState(BlockPos blockPos) {
        try {
            int secX = SectionPos.blockToSectionCoord(blockPos.getX()) - this.minSectionX;
            int secY = SectionPos.blockToSectionCoord(blockPos.getY() - minHeight) - this.minSectionY;
            int secZ = SectionPos.blockToSectionCoord(blockPos.getZ()) - this.minSectionZ;

            PalettedContainer<BlockState> container = this.sections[WIDTH * ((WIDTH * secY) + secZ) + secX];

            return container != null ?
                    container.get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15)
                    : Blocks.AIR.defaultBlockState();
        }
        catch (Throwable var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
            crashReportCategory.setDetail("Location",
                    () -> CrashReportCategory.formatLocation(
                            this, blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            throw new ReportedException(crashReport);
        }
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
