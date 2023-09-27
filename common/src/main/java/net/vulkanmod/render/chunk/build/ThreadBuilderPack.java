package net.vulkanmod.render.chunk.build;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;

import java.util.Map;
import java.util.stream.Collectors;

public class ThreadBuilderPack {
    private final Map<RenderType, TerrainBufferBuilder> builders = RenderType.chunkBufferLayers().stream().collect(Collectors.toMap(
            (renderType) -> renderType,
            (renderType) -> new TerrainBufferBuilder(renderType.bufferSize())));

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
