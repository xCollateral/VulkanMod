package net.vulkanmod.render.vertex;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.EnumSet;

public enum TerrainRenderType {
    SOLID(RenderType.solid(), 0.0f),
    CUTOUT_MIPPED(RenderType.cutoutMipped(), 0.5f),
    CUTOUT(RenderType.cutout(), 0.1f),
    TRANSLUCENT(RenderType.translucent(), 0.0f),
    TRIPWIRE(RenderType.tripwire(), 0.1f);

    public static final TerrainRenderType[] VALUES = TerrainRenderType.values();

    public static final EnumSet<TerrainRenderType> COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, TRANSLUCENT);
    public static final EnumSet<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT, CUTOUT_MIPPED, TRANSLUCENT);

    final float alphaCutout;
    public final int maxSize;

    TerrainRenderType(RenderType renderType, float alphaCutout) {
        this.alphaCutout = alphaCutout;
        this.maxSize = renderType.bufferSize();
    }

    public void setCutoutUniform() {
        VRenderSystem.alphaCutout = this.alphaCutout;
    }

    public static TerrainRenderType get(String renderType) {
        return switch (renderType)
        {
            case "solid" -> SOLID;
            case "cutout_mipped" -> CUTOUT_MIPPED;
            case "cutout" -> CUTOUT;
            case "translucent" -> TRANSLUCENT;
            case "tripwire" -> TRIPWIRE;
            default -> throw new IllegalStateException("Unexpected value: " + renderType);
        };
    }

//    public static TerrainRenderType get2(String renderType) {
//        return TerrainRenderType.valueOf(renderType);
//    }

}
