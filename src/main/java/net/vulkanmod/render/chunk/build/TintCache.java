package net.vulkanmod.render.chunk.build;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.biome.BoxBlur;

import java.util.Arrays;
import java.util.function.BiFunction;

public class TintCache {
    private static final int SECTION_WIDTH = 16;

    private final Layer[] layers = new Layer[SECTION_WIDTH];

    private int blendRadius, totalWidth;
    private int secX, secY, secZ;
    private int minX, minZ;
    private int maxX, maxZ;

    private int dataSize;
    private int[] temp;

    public TintCache() {
        Arrays.fill(layers, new Layer());
    }

    public void init(int blendRadius, int secX, int secY, int secZ) {
        this.blendRadius = Minecraft.getInstance().options.biomeBlendRadius().get();
        this.totalWidth = (blendRadius * 2) + 16;

        this.secX = secX;
        this.secY = secY;
        this.secZ = secZ;

        minX = (secX << 4) - blendRadius;
        minZ = (secZ << 4) - blendRadius;
        maxX = (secX << 4) + 16 + blendRadius;
        maxZ = (secZ << 4) + 16 + blendRadius;

        int size = totalWidth * totalWidth;

        if(size != dataSize) {
            this.dataSize = size;
            for (Layer layer : layers) {
                layer.allocate(size);
            }
            temp = new int[size];
        } else {
            for (Layer layer : layers) {
                layer.invalidate();
            }
        }
    }

    public int getColor(BlockPos blockPos, ColorResolver colorResolver) {
        int relY = blockPos.getY() & 15;
        Layer layer = layers[relY];
        if(layer.invalidated)
            calculateLayer(relY);

        int[] values = layer.getValues(colorResolver);
        int relX = blockPos.getX() & 15;
        int relZ = blockPos.getZ() & 15;
        int idx = totalWidth * (relZ + blendRadius) + (relX + blendRadius);
        return values[idx];
    }

    public void calculateLayer(int y) {
        Level level = WorldRenderer.getLevel();
        Layer layer = layers[y];

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        int absY = (secY << 4) + y;

        Biome tB = level.getBiome(blockPos.set(minX, absY, minZ)).value();
        boolean mixed = false;
        for (int absZ = minZ; absZ < maxZ ; absZ++) {
            for (int absX = minX; absX < maxX ; absX++) {
                blockPos.set(absX, absY, absZ);
                Biome biome = level.getBiome(blockPos).value();

                if (biome != tB)
                    mixed = true;

                final int idx = (absX - minX) + (absZ - minZ) * totalWidth;
                layer.grass[idx] = biome.getGrassColor(absX, absZ);
                layer.foliage[idx] = biome.getFoliageColor();
                layer.water[idx] = biome.getWaterColor();
            }
        }

        if(mixed && blendRadius > 0) {
            BoxBlur.blur(layer.grass, temp, SECTION_WIDTH, blendRadius);
            BoxBlur.blur(layer.foliage, temp, SECTION_WIDTH, blendRadius);
            BoxBlur.blur(layer.water, temp, SECTION_WIDTH, blendRadius);
        }

        layer.invalidated = false;
    }

    static class Layer {
        private boolean invalidated = true;

        private int[] grass;
        private int[] foliage;
        private int[] water;

        void allocate(int size) {
            grass = new int[size];
            foliage = new int[size];
            water = new int[size];
            invalidate();
        }

        void invalidate() {
            this.invalidated = true;
        }

        public int[] getValues(ColorResolver colorResolver) {
            if(colorResolver == BiomeColors.GRASS_COLOR_RESOLVER)
                return grass;
            else if(colorResolver == BiomeColors.FOLIAGE_COLOR_RESOLVER)
                return foliage;
            else if(colorResolver == BiomeColors.WATER_COLOR_RESOLVER)
                return water;

            throw new IllegalArgumentException("Unexpected resolver: " + colorResolver.toString());
        }
    }
}
