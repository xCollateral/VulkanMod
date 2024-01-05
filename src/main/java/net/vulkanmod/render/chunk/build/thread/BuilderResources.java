package net.vulkanmod.render.chunk.build.thread;

import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.LiquidRenderer;
import net.vulkanmod.render.chunk.build.TintCache;

public class BuilderResources {
    public final ThreadBuilderPack builderPack = new ThreadBuilderPack();
    public final BlockRenderer blockRenderer = new BlockRenderer();
    public final LiquidRenderer liquidRenderer = new LiquidRenderer();

    public final TintCache tintCache = new TintCache();

    private int totalBuildTime = 0, buildCount = 0;

    public void clear() {
        builderPack.clearAll();
    }

    public void updateBuildStats(int buildTime) {
        this.buildCount++;
        this.totalBuildTime += buildTime;
    }

    public int getTotalBuildTime() {
        return totalBuildTime;
    }

    public int getBuildCount() {
        return buildCount;
    }

    public void resetCounters() {
        totalBuildTime = 0;
        buildCount = 0;
    }
}
