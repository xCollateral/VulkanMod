package net.vulkanmod.render.chunk.build.thread;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.LiquidRenderer;
import net.vulkanmod.render.chunk.build.RenderRegion;
import net.vulkanmod.render.chunk.build.TintCache;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.ArrayLightDataCache;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.build.light.flat.FlatLightPipeline;
import net.vulkanmod.render.chunk.build.light.smooth.NewSmoothLightPipeline;
import net.vulkanmod.render.chunk.build.light.smooth.SmoothLightPipeline;

public class BuilderResources {
    public final ThreadBuilderPack builderPack = new ThreadBuilderPack();
    public final BlockRenderer blockRenderer = new BlockRenderer();
    public final LiquidRenderer liquidRenderer = new LiquidRenderer();

    public final TintCache tintCache = new TintCache();

    public RenderRegion region;

    public final ArrayLightDataCache lightDataCache = new ArrayLightDataCache();
    public final QuadLightData quadLightData = new QuadLightData();

    public final LightPipeline smoothLightPipeline;
    public final LightPipeline flatLightPipeline;

    private int totalBuildTime = 0, buildCount = 0;

    public BuilderResources() {
        this.flatLightPipeline = new FlatLightPipeline(lightDataCache);

        if(Initializer.CONFIG.ambientOcclusion == LightMode.SUB_BLOCK)
            this.smoothLightPipeline = new NewSmoothLightPipeline(lightDataCache);
        else
            this.smoothLightPipeline = new SmoothLightPipeline(lightDataCache);
    }

    public void update(RenderRegion region, RenderSection renderSection) {
        this.region = region;

        lightDataCache.reset(region, renderSection.xOffset(), renderSection.yOffset(), renderSection.zOffset());

        blockRenderer.setResources(this);
        liquidRenderer.setResources(this);
    }

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
