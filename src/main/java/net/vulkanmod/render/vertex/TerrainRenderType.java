package net.vulkanmod.render.vertex;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Collectors;

public enum TerrainRenderType {
    SOLID(RenderType.solid(), 0.0f),
    CUTOUT_MIPPED(RenderType.cutoutMipped(), 0.5f),
    CUTOUT(RenderType.cutout(), 0.1f),
    TRANSLUCENT(RenderType.translucent(), 0.0f),
    TRIPWIRE(RenderType.tripwire(), 0.1f);

    public static final TerrainRenderType[] VALUES = TerrainRenderType.values();

    private static final Map<RenderType, TerrainRenderType> map = new Hashtable<>(
            Arrays.stream(TerrainRenderType.values()).collect(Collectors.toMap(
                    (terrainRenderType) -> terrainRenderType.renderType, (terrainRenderType) -> terrainRenderType)));

    final RenderType renderType;
    final float alphaCutout;

    TerrainRenderType(RenderType renderType, float alphaCutout) {
        this.renderType = renderType;
        this.alphaCutout = alphaCutout;
    }

    public void setCutoutUniform() {
        VRenderSystem.alphaCutout = this.alphaCutout;
    }

    public static TerrainRenderType get(RenderType renderType) {
        return map.get(renderType);
    }
}
