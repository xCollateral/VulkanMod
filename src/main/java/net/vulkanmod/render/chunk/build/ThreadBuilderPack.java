package net.vulkanmod.render.chunk.build;

import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.EnumMap;
import java.util.function.Function;

public class ThreadBuilderPack {
    private static final Function<TerrainRenderType, TerrainBufferBuilder> terrainBuilderConstructor = renderType -> new TerrainBufferBuilder(renderType.bufferSize);


    private final EnumMap<TerrainRenderType, TerrainBufferBuilder> builders=new EnumMap<>(TerrainRenderType.class);

    public ThreadBuilderPack() {
        for (TerrainRenderType renderType : TerrainRenderType.getActiveLayers()) {
            builders.put(renderType, terrainBuilderConstructor.apply(renderType));
        }
    }

    public TerrainBufferBuilder builder(TerrainRenderType renderType) {
        return this.builders.get(renderType);
    }

    public void clearAll() {
        this.builders.values().forEach(TerrainBufferBuilder::clear);
    }

    public void discardAll() {
        this.builders.values().forEach(TerrainBufferBuilder::discard);
    }

}
