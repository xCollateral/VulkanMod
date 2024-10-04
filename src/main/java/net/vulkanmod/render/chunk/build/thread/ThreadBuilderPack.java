package net.vulkanmod.render.chunk.build.thread;

import net.vulkanmod.render.vertex.TerrainBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class ThreadBuilderPack {
    private static Function<TerrainRenderType, TerrainBuilder> terrainBuilderConstructor;

    public static void defaultTerrainBuilderConstructor() {
        terrainBuilderConstructor = renderType -> new TerrainBuilder(TerrainRenderType.getRenderType(renderType).bufferSize());
    }

    public static void setTerrainBuilderConstructor(Function<TerrainRenderType, TerrainBuilder> constructor) {
        terrainBuilderConstructor = constructor;
    }

    private final Map<TerrainRenderType, TerrainBuilder> builders;

    public ThreadBuilderPack() {
        var map = new EnumMap<TerrainRenderType, TerrainBuilder>(TerrainRenderType.class);
        Arrays.stream(TerrainRenderType.values()).forEach(
                terrainRenderType -> map.put(terrainRenderType,
                        terrainBuilderConstructor.apply(terrainRenderType))
        );
        builders = map;
    }

    public TerrainBuilder builder(TerrainRenderType renderType) {
        return this.builders.get(renderType);
    }

    public void clearAll() {
        this.builders.values().forEach(TerrainBuilder::clear);
    }

}
