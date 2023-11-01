package net.vulkanmod.render.chunk.build;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ThreadBuilderPack {
    private static Function<RenderType, TerrainBufferBuilder> terrainBuilderConstructor;

    public static void defaultTerrainBuilderConstructor() {
        terrainBuilderConstructor = renderType -> new TerrainBufferBuilder(renderType.bufferSize());
    }

    public static void setTerrainBuilderConstructor(Function<RenderType, TerrainBufferBuilder> constructor) {
        terrainBuilderConstructor = constructor;
    }

    private final Map<RenderType, TerrainBufferBuilder> builders;

    public ThreadBuilderPack() {
        builders = RenderType.chunkBufferLayers().stream().collect(Collectors.toMap(
                (renderType) -> renderType,
                renderType -> terrainBuilderConstructor.apply(renderType)));
    }

    public TerrainBufferBuilder builder(RenderType renderType) {
        return this.builders.get(renderType);
    }

    public void clearAll() {
        this.builders.values().forEach(TerrainBufferBuilder::clear);
    }

    public void discardAll() {
        this.builders.values().forEach(TerrainBufferBuilder::discard);
    }

}
