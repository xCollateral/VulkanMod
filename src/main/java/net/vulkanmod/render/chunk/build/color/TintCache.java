package net.vulkanmod.render.chunk.build.color;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.vulkanmod.render.chunk.WorldRenderer;

import java.util.Arrays;

public class TintCache {
    private static final int SECTION_WIDTH = 16;

    private final Reference2ReferenceOpenHashMap<ColorResolver, Layer[]> layers;

    private int blendRadius, totalWidth;
    private int secX, secY, secZ;
    private int minX, minZ;
    private int maxX, maxZ;

    private int dataSize;
    private int[] temp;

    public TintCache() {
        this.layers = new Reference2ReferenceOpenHashMap<>();

        // Default resolvers
        this.layers.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, allocateLayers());
        this.layers.put(BiomeColors.GRASS_COLOR_RESOLVER, allocateLayers());
        this.layers.put(BiomeColors.WATER_COLOR_RESOLVER, allocateLayers());
    }

    public void init(int blendRadius, int secX, int secY, int secZ) {
        this.blendRadius = Minecraft.getInstance().options.biomeBlendRadius().get();
        this.totalWidth = (blendRadius * 2) + 16;

        this.secX = secX;
        this.secY = secY;
        this.secZ = secZ;

        this.minX = (secX << 4) - blendRadius;
        this.minZ = (secZ << 4) - blendRadius;
        this.maxX = (secX << 4) + 16 + blendRadius;
        this.maxZ = (secZ << 4) + 16 + blendRadius;

        int size = totalWidth * totalWidth;

        if (size != this.dataSize) {
            this.dataSize = size;

            for (Layer[] layers : layers.values()) {
                for (Layer layer : layers) {
                    layer.allocate(size);
                }
            }

            this.temp = new int[size];
        }
        else {
            for (Layer[] layers : layers.values()) {
                for (Layer layer : layers) {
                    layer.invalidate();
                }
            }
        }
    }

    public int getColor(BlockPos blockPos, ColorResolver colorResolver) {
        int relY = blockPos.getY() & 15;

        if (!this.layers.containsKey(colorResolver)) {
            addResolver(colorResolver);
        }

        Layer layer = this.layers.get(colorResolver)[relY];

        if (layer.invalidated) {
            calculateLayer(layer, colorResolver, relY);
        }

        int[] values = layer.getValues();

        int relX = blockPos.getX() & 15;
        int relZ = blockPos.getZ() & 15;
        int idx = this.totalWidth * (relZ + this.blendRadius) + (relX + this.blendRadius);
        return values[idx];
    }

    private void addResolver(ColorResolver colorResolver) {
        Layer[] layers1 = allocateLayers();

        for (Layer layer : layers1) {
            layer.allocate(this.dataSize);
        }

        this.layers.put(colorResolver, layers1);
    }

    private Layer[] allocateLayers() {
        Layer[] layers = new Layer[SECTION_WIDTH];

        Arrays.fill(layers, new Layer());
        return layers;
    }

    private void calculateLayer(Layer layer, ColorResolver colorResolver, int y) {
        Level level = WorldRenderer.getLevel();

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        int absY = (secY << 4) + y;

        int[] values = layer.values;

        for (int absZ = minZ; absZ < maxZ; absZ++) {
            for (int absX = minX; absX < maxX; absX++) {
                blockPos.set(absX, absY, absZ);
                Biome biome = level.getBiome(blockPos).value();

                final int idx = (absX - minX) + (absZ - minZ) * totalWidth;
                values[idx] = colorResolver.getColor(biome, absX, absZ);
            }
        }

        if (blendRadius > 0) {
            this.applyBlur(values);
        }

        layer.invalidated = false;
    }

    private void applyBlur(int[] buffer) {
        int value = buffer[0];
        boolean needsBlur = false;
        for (int i = 1; i < buffer.length; ++i) {
            if (value != buffer[i]) {
                needsBlur = true;
                break;
            }
        }

        if (needsBlur)
            BoxBlur.blur(buffer, temp, SECTION_WIDTH, blendRadius);
    }

    static class Layer {
        private boolean invalidated = true;
        private int[] values;

        void allocate(int size) {
            this.values = new int[size];
            invalidate();
        }

        void invalidate() {
            this.invalidated = true;
        }

        public int[] getValues() {
            return this.values;
        }
    }
}
