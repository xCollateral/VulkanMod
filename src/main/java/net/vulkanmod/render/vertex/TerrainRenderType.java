package net.vulkanmod.render.vertex;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderType;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Collectors;

public enum TerrainRenderType {
    SOLID(RenderType.solid()),
    CUTOUT_MIPPED(RenderType.cutoutMipped()),
    CUTOUT(RenderType.cutout()),
    TRANSLUCENT(RenderType.translucent()),
    TRIPWIRE(RenderType.tripwire());

    public static final TerrainRenderType[] VALUES = TerrainRenderType.values();

    private static final Map<RenderType, TerrainRenderType> RENDER_TYPE_MAP = new Hashtable<>(
            Arrays.stream(TerrainRenderType.values()).collect(Collectors.toMap(
                    (terrainRenderType) -> terrainRenderType.renderType, (terrainRenderType) -> terrainRenderType)));

    public static final ObjectArrayList<RenderType> COMPACT_RENDER_TYPES = new ObjectArrayList<>();
    public static final ObjectArrayList<RenderType> SEMI_COMPACT_RENDER_TYPES = new ObjectArrayList<>();

    static {
        SEMI_COMPACT_RENDER_TYPES.add(RenderType.cutout());
        COMPACT_RENDER_TYPES.add(RenderType.cutoutMipped());
        SEMI_COMPACT_RENDER_TYPES.add(RenderType.cutoutMipped());
        COMPACT_RENDER_TYPES.add(RenderType.translucent());
        SEMI_COMPACT_RENDER_TYPES.add(RenderType.translucent());
    }

    final RenderType renderType;


    TerrainRenderType(RenderType renderType) {
        this.renderType = renderType;
    }



    public static TerrainRenderType get(RenderType renderType) {
        return RENDER_TYPE_MAP.get(renderType);
    }
}
