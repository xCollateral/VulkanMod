package net.vulkanmod.render.vertex;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.interfaces.ExtendedRenderType;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.EnumSet;

public enum TerrainRenderType {
    SOLID(0.0f),
    CUTOUT_MIPPED(0.5f),
    CUTOUT(0.1f),
    TRANSLUCENT(0.0f),
    TRIPWIRE(0.1f);
    //Changed to EnumSet, in case it generates better ASM than a standard array (e.g. Compile time length guarantees)
    public static final EnumSet<TerrainRenderType> VALUES = EnumSet.allOf(TerrainRenderType.class);

    public static final EnumSet<TerrainRenderType> COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, TRANSLUCENT);
    public static final EnumSet<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, CUTOUT, TRANSLUCENT);

    public final float alphaCutout;

    TerrainRenderType(float alphaCutout) {
        this.alphaCutout = alphaCutout;
    }

    public void setCutoutUniform() {
        VRenderSystem.alphaCutout = this.alphaCutout;
    }

    public static TerrainRenderType get(RenderType renderType) {
        return ((ExtendedRenderType)renderType).getTerrainRenderType();
    }

    public static TerrainRenderType get(String name) {
        return switch (name) {
            case "solid" -> TerrainRenderType.SOLID;
            case "cutout" -> TerrainRenderType.CUTOUT;
            case "cutout_mipped" -> TerrainRenderType.CUTOUT_MIPPED;
            case "translucent" -> TerrainRenderType.TRANSLUCENT;
            case "tripwire" -> TerrainRenderType.TRIPWIRE;
            default -> null;
        };
    }

    public static RenderType getRenderType(TerrainRenderType renderType) {
        return switch (renderType) {
            case SOLID -> RenderType.solid();
            case CUTOUT -> RenderType.cutout();
            case CUTOUT_MIPPED -> RenderType.cutoutMipped();
            case TRANSLUCENT -> RenderType.translucent();
            case TRIPWIRE -> RenderType.tripwire();
        };
    }
}
