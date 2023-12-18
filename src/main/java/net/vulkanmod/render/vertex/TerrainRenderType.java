package net.vulkanmod.render.vertex;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.EnumSet;

public enum TerrainRenderType {
    SOLID(RenderType.solid()),
    CUTOUT_MIPPED(RenderType.cutoutMipped()),
    CUTOUT(RenderType.cutout()),
    TRANSLUCENT(RenderType.translucent()),
    TRIPWIRE(RenderType.tripwire());

    public static final TerrainRenderType[] VALUES = TerrainRenderType.values();

    public static final EnumSet<TerrainRenderType> COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, TRANSLUCENT);
    public static final EnumSet<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, CUTOUT, TRANSLUCENT);

    public final int bufferSize;
    TerrainRenderType(RenderType renderType) {
        this.bufferSize = renderType.bufferSize();
    }
    public static EnumSet<TerrainRenderType> getActiveLayers() {
        return Initializer.CONFIG.uniqueOpaqueLayer ? COMPACT_RENDER_TYPES : SEMI_COMPACT_RENDER_TYPES;
    }

    public static TerrainRenderType getCompact(String renderType) {
        if(Initializer.CONFIG.uniqueOpaqueLayer) {
            return switch (renderType)
            {
                case "solid", "cutout", "cutout_mipped" -> CUTOUT_MIPPED;
                default -> TRANSLUCENT;
            };

        }
        else {
            return switch (renderType)
            {
                case "solid", "cutout_mipped" -> CUTOUT_MIPPED;
                case "cutout" -> CUTOUT;
                default -> TRANSLUCENT;
            };
        }


    }



    public static TerrainRenderType get(String renderType) {
        return switch (renderType)
        {
            case "solid" -> SOLID;
            case "cutout_mipped" -> CUTOUT_MIPPED;
            case "cutout" -> CUTOUT;
            case "tripwire" -> TRIPWIRE;
            default -> TRANSLUCENT;
        };
    }
}
