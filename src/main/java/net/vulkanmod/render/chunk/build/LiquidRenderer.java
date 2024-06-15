package net.vulkanmod.render.chunk.build;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.model.quad.ModelQuad;
import net.vulkanmod.render.model.quad.ModelQuadFlags;
import net.vulkanmod.render.model.quad.QuadUtils;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Vector3f;

public class LiquidRenderer {
    private static final float MAX_FLUID_HEIGHT = 0.8888889F;

    private final BlockPos.MutableBlockPos mBlockPos = new BlockPos.MutableBlockPos();

    private final ModelQuad modelQuad = new ModelQuad();

    BuilderResources resources;

    private final int[] quadColors = new int[4];

    public void setResources(BuilderResources resources) {
        this.resources = resources;
    }

    public void renderLiquid(BlockState blockState, FluidState fluidState, BlockPos blockPos, TerrainBufferBuilder vertexConsumer) {
        tessellate(blockState, fluidState, blockPos, vertexConsumer);
    }

    private boolean isFaceOccludedByState(BlockGetter blockGetter, float h, Direction direction, BlockPos blockPos, BlockState blockState) {
        mBlockPos.set(blockPos).offset(Direction.DOWN.getNormal());

        if (blockState.canOcclude()) {
            VoxelShape occlusionShape = blockState.getOcclusionShape(blockGetter, mBlockPos);

            if (occlusionShape == Shapes.block()) {
                return direction != Direction.UP;
            } else if (occlusionShape.isEmpty()) {
                return false;
            }

            VoxelShape voxelShape = Shapes.box(0.0, 0.0, 0.0, 1.0, h, 1.0);
            return Shapes.blockOccudes(voxelShape, occlusionShape, direction);
        } else {
            return false;
        }
    }

    public static boolean shouldRenderFace(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, FluidState fluidState, BlockState blockState, Direction direction, BlockState adjBlockState) {

        if (adjBlockState.getFluidState().getType().isSame(fluidState.getType()))
            return false;

        // self-occlusion by waterlogging
        if (blockState.canOcclude()) {
            return !blockState.isFaceSturdy(blockAndTintGetter, blockPos, direction);
        }

        return true;
    }

    public BlockState getAdjBlockState(BlockAndTintGetter blockAndTintGetter, int x, int y, int z, Direction dir) {
        mBlockPos.set(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
        return blockAndTintGetter.getBlockState(mBlockPos);
    }

    public void tessellate(BlockState blockState, FluidState fluidState, BlockPos blockPos, TerrainBufferBuilder vertexConsumer) {
        BlockAndTintGetter region = this.resources.region;

        final FluidRenderHandler handler = getFluidRenderHandler(fluidState);
        int color = handler.getFluidColor(region, blockPos, fluidState);

        TextureAtlasSprite[] sprites = handler.getFluidSprites(region, blockPos, fluidState);

        float r = ColorUtil.ARGB.unpackR(color);
        float g = ColorUtil.ARGB.unpackG(color);
        float b = ColorUtil.ARGB.unpackB(color);

        final int posX = blockPos.getX();
        final int posY = blockPos.getY();
        final int posZ = blockPos.getZ();

        boolean useAO = blockState.getLightEmission() == 0 && Minecraft.useAmbientOcclusion();
        LightPipeline lightPipeline = useAO ? this.resources.smoothLightPipeline : this.resources.flatLightPipeline;

        BlockState downState = getAdjBlockState(region, posX, posY, posZ, Direction.DOWN);
        BlockState upState = getAdjBlockState(region, posX, posY, posZ, Direction.UP);
        BlockState northState = getAdjBlockState(region, posX, posY, posZ, Direction.NORTH);
        BlockState southState = getAdjBlockState(region, posX, posY, posZ, Direction.SOUTH);
        BlockState westState = getAdjBlockState(region, posX, posY, posZ, Direction.WEST);
        BlockState eastState = getAdjBlockState(region, posX, posY, posZ, Direction.EAST);

//        boolean rUf = !isNeighborSameFluid(fluidState, upFluid);
        boolean rUf = shouldRenderFace(region, blockPos, fluidState, blockState, Direction.UP, upState);
        boolean rDf = shouldRenderFace(region, blockPos, fluidState, blockState, Direction.DOWN, downState)
                && !isFaceOccludedByState(region, MAX_FLUID_HEIGHT, Direction.DOWN, blockPos, downState);
        boolean rNf = shouldRenderFace(region, blockPos, fluidState, blockState, Direction.NORTH, northState);
        boolean rSf = shouldRenderFace(region, blockPos, fluidState, blockState, Direction.SOUTH, southState);
        boolean rWf = shouldRenderFace(region, blockPos, fluidState, blockState, Direction.WEST, westState);
        boolean rEf = shouldRenderFace(region, blockPos, fluidState, blockState, Direction.EAST, eastState);

        if (!(rUf || rDf || rEf || rWf || rNf || rSf))
            return;

        float brightnessUp = region.getShade(Direction.UP, true);

        Fluid fluid = fluidState.getType();
        float height = this.getHeight(region, fluid, blockPos, blockState);
        float neHeight;
        float nwHeight;
        float seHeight;
        float swHeight;
        if (height >= 1.0F) {
            neHeight = 1.0F;
            nwHeight = 1.0F;
            seHeight = 1.0F;
            swHeight = 1.0F;
        } else {
            float s = this.getHeight(region, fluid, mBlockPos.set(blockPos).offset(Direction.NORTH.getNormal()), northState);
            float t = this.getHeight(region, fluid, mBlockPos.set(blockPos).offset(Direction.SOUTH.getNormal()), southState);
            float u = this.getHeight(region, fluid, mBlockPos.set(blockPos).offset(Direction.EAST.getNormal()), eastState);
            float v = this.getHeight(region, fluid, mBlockPos.set(blockPos).offset(Direction.WEST.getNormal()), westState);
            neHeight = this.calculateAverageHeight(region, fluid, height, s, u, mBlockPos.set(blockPos).offset(Direction.NORTH.getNormal()).offset(Direction.EAST.getNormal()));
            nwHeight = this.calculateAverageHeight(region, fluid, height, s, v, mBlockPos.set(blockPos).offset(Direction.NORTH.getNormal()).offset(Direction.WEST.getNormal()));
            seHeight = this.calculateAverageHeight(region, fluid, height, t, u, mBlockPos.set(blockPos).offset(Direction.SOUTH.getNormal()).offset(Direction.EAST.getNormal()));
            swHeight = this.calculateAverageHeight(region, fluid, height, t, v, mBlockPos.set(blockPos).offset(Direction.SOUTH.getNormal()).offset(Direction.WEST.getNormal()));
        }

        float x0 = (posX & 15);
        float y0 = (posY & 15);
        float z0 = (posZ & 15);
//            float x = 0.001F;
        float y = rDf ? 0.001F : 0.0F;

        modelQuad.setFlags(0);

        if (rUf && !isFaceOccludedByState(region, Math.min(Math.min(nwHeight, swHeight), Math.min(seHeight, neHeight)), Direction.UP, blockPos, upState)) {
            float u0, u1, u2, u3;
            float v0, v1, v2, v3;

            nwHeight -= 0.001F;
            swHeight -= 0.001F;
            seHeight -= 0.001F;
            neHeight -= 0.001F;
            Vec3 vec3 = fluidState.getFlow(region, blockPos);
            TextureAtlasSprite sprite;

            if (vec3.x == 0.0 && vec3.z == 0.0) {
                sprite = sprites[0];
                u0 = sprite.getU(0.0F);
                v0 = sprite.getV(0.0F);
                u1 = u0;
                v1 = sprite.getV(1.0F);
                u2 = sprite.getU(1.0F);
                v2 = v1;
                u3 = u2;
                v3 = v0;
            } else {
                sprite = sprites[1];
                float ah = (float) Mth.atan2(vec3.z, vec3.x) - 1.5707964F;
                float ai = Mth.sin(ah) * 0.25F;
                float aj = Mth.cos(ah) * 0.25F;

                u0 = sprite.getU(0.5F + (-aj - ai));
                v0 = sprite.getV(0.5F - aj + ai);
                u1 = sprite.getU(0.5F - aj + ai);
                v1 = sprite.getV(0.5F + aj + ai);
                u2 = sprite.getU(0.5F + aj + ai);
                v2 = sprite.getV(0.5F + (aj - ai));
                u3 = sprite.getU(0.5F + (aj - ai));
                v3 = sprite.getV(0.5F + (-aj - ai));
            }

            float uA = (u0 + u1 + u2 + u3) / 4.0F;
            float vA = (v0 + v1 + v2 + v3) / 4.0F;
            float ai = sprites[0].uvShrinkRatio();
            u0 = Mth.lerp(ai, u0, uA);
            u1 = Mth.lerp(ai, u1, uA);
            u2 = Mth.lerp(ai, u2, uA);
            u3 = Mth.lerp(ai, u3, uA);
            v0 = Mth.lerp(ai, v0, vA);
            v1 = Mth.lerp(ai, v1, vA);
            v2 = Mth.lerp(ai, v2, vA);
            v3 = Mth.lerp(ai, v3, vA);

            float brightness = brightnessUp;

            setVertex(modelQuad, 0, 0.0f, nwHeight, 0.0f, u0, v0);
            setVertex(modelQuad, 1, 0.0f, swHeight, 1.0f, u1, v1);
            setVertex(modelQuad, 2, 1.0f, seHeight, 1.0f, u2, v2);
            setVertex(modelQuad, 3, 1.0f, neHeight, 0.0f, u3, v3);

            updateQuad(this.modelQuad, blockPos, lightPipeline, Direction.UP);
            updateColor(r, g, b, brightness);

            putQuad(modelQuad, vertexConsumer, x0, y0, z0, false);

            if (fluidState.shouldRenderBackwardUpFace(region, blockPos.above())) {
                putQuad(modelQuad, vertexConsumer, x0, y0, z0, true);
            }

        }

        if (rDf) {
            float u0, u1, v0, v1;

            u0 = sprites[0].getU0();
            u1 = sprites[0].getU1();
            v0 = sprites[0].getV0();
            v1 = sprites[0].getV1();

            float brightness = region.getShade(Direction.DOWN, true);

            setVertex(modelQuad, 0, 0.0f, y, 1.0f, u0, v1);
            setVertex(modelQuad, 1, 0.0f, y, 0.0f, u0, v0);
            setVertex(modelQuad, 2, 1.0f, y, 0.0f, u1, v0);
            setVertex(modelQuad, 3, 1.0f, y, 1.0f, u1, v1);

            updateQuad(this.modelQuad, blockPos, lightPipeline, Direction.DOWN);
            updateColor(r, g, b, brightness);

            putQuad(modelQuad, vertexConsumer, x0, y0, z0, false);

        }

        modelQuad.setFlags(ModelQuadFlags.IS_PARALLEL | ModelQuadFlags.IS_ALIGNED);

        for (Direction direction : Util.XZ_DIRECTIONS) {
            float h1;
            float h2;

            float x1;
            float z1;
            float x2;
            float z2;

            final float E = 0.001f;
            final float E2 = 0.999f;

            BlockState adjState;
            switch (direction) {
                case NORTH -> {
                    if (!rNf)
                        continue;

                    h1 = nwHeight;
                    h2 = neHeight;
                    x1 = 0.0f;
                    x2 = 1.0f;
                    z1 = E;
                    z2 = E;

                    adjState = northState;
                }
                case SOUTH -> {
                    if (!rSf)
                        continue;

                    h1 = seHeight;
                    h2 = swHeight;
                    x1 = 1.0f;
                    x2 = 0.0f;
                    z1 = E2;
                    z2 = E2;

                    adjState = southState;
                }
                case WEST -> {
                    if (!rWf)
                        continue;

                    h1 = swHeight;
                    h2 = nwHeight;
                    x1 = E;
                    x2 = E;
                    z1 = 1.0f;
                    z2 = 0.0f;

                    adjState = westState;
                }
                case EAST -> {
                    if (!rEf)
                        continue;

                    h1 = neHeight;
                    h2 = seHeight;
                    x1 = E2;
                    x2 = E2;
                    z1 = 0.0f;
                    z2 = 1.0f;

                    adjState = eastState;
                }

                default -> {
                    continue;
                }
            }

            if (isFaceOccludedByState(region, Math.max(h1, h2), direction, blockPos, adjState))
                continue;

            TextureAtlasSprite sprite = sprites[1];
            boolean isOverlay = false;

            if (sprites.length > 2) {
                if (FluidRenderHandlerRegistry.INSTANCE.isBlockTransparent(adjState.getBlock())) {
                    sprite = sprites[2];
                    isOverlay = true;
                }
            }

            float u0 = sprite.getU(0.0F);
            float u1 = sprite.getU(0.5F);
            float v0 = sprite.getV((1.0F - h1) * 0.5F);
            float v1 = sprite.getV((1.0F - h2) * 0.5F);
            float v2 = sprite.getV(0.5F);

            float brightness = region.getShade(direction, true);

            setVertex(modelQuad, 0, x2, h2, z2, u1, v1);
            setVertex(modelQuad, 1, x2, y, z2, u1, v2);
            setVertex(modelQuad, 2, x1, y, z1, u0, v2);
            setVertex(modelQuad, 3, x1, h1, z1, u0, v0);

            updateQuad(this.modelQuad, blockPos, lightPipeline, direction);
            updateColor(r, g, b, brightness);

            putQuad(modelQuad, vertexConsumer, x0, y0, z0, false);

            if (!isOverlay) {
                putQuad(modelQuad, vertexConsumer, x0, y0, z0, true);
            }

        }
    }

    private static FluidRenderHandler getFluidRenderHandler(FluidState fluidState) {
        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());

        // Fallback to water in case no handler was found
        if (handler == null) {
            handler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER);
        }

        return handler;
    }

    private float calculateAverageHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, float f, float g, float h, BlockPos blockPos) {
        if (!(h >= 1.0F) && !(g >= 1.0F)) {
            float[] fs = new float[2];
            if (h > 0.0F || g > 0.0F) {
                float i = this.getHeight(blockAndTintGetter, fluid, blockPos);
                if (i >= 1.0F) {
                    return 1.0F;
                }

                this.addWeightedHeight(fs, i);
            }

            this.addWeightedHeight(fs, f);
            this.addWeightedHeight(fs, h);
            this.addWeightedHeight(fs, g);
            return fs[0] / fs[1];
        } else {
            return 1.0F;
        }
    }

    private void addWeightedHeight(float[] fs, float f) {
        if (f >= 0.8F) {
            fs[0] += f * 10.0F;
            fs[1] += 10.0F;
        } else if (f >= 0.0F) {
            fs[0] += f;
            fs[1]++;
        }

    }

    private float getHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, BlockPos blockPos) {
        BlockState blockState = blockAndTintGetter.getBlockState(blockPos);
        return this.getHeight(blockAndTintGetter, fluid, blockPos, blockState);
    }

    private float getHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, BlockPos blockPos, BlockState adjBlockState) {
        FluidState adjFluidState = adjBlockState.getFluidState();
        if (fluid.isSame(adjFluidState.getType())) {
            BlockState blockState2 = blockAndTintGetter.getBlockState(blockPos.offset(Direction.UP.getNormal()));
            return fluid.isSame(blockState2.getFluidState().getType()) ? 1.0F : adjFluidState.getOwnHeight();
        } else {
            return !adjBlockState.isSolid() ? 0.0F : -1.0f;
        }
    }

    private int calculateNormal(ModelQuad quad) {
        // TODO
        Vector3f normal = new Vector3f(quad.getX(1), quad.getY(1), quad.getZ(1))
                .cross(quad.getX(3), quad.getY(3), quad.getZ(3));
        normal.normalize();

        return VertexUtil.packNormal(normal.x(), normal.y(), normal.z());
    }

    private void putQuad(ModelQuad quad, TerrainBufferBuilder bufferBuilder, float xOffset, float yOffset, float zOffset, boolean flip) {
        QuadLightData quadLightData = resources.quadLightData;

        // Rotate triangles if needed to fix AO anisotropy
        int k = QuadUtils.getIterationStartIdx(quadLightData.br);

        bufferBuilder.ensureCapacity();

        int i;
        for (int j = 0; j < 4; j++) {
            i = k;

            final float x = xOffset + quad.getX(i);
            final float y = yOffset + quad.getY(i);
            final float z = zOffset + quad.getZ(i);

            bufferBuilder.vertex(x, y, z, this.quadColors[i], quad.getU(i), quad.getV(i), quadLightData.lm[i], 0);

            k += (flip ? -1 : +1);
            k &= 0b11;
        }

    }

    private void setVertex(ModelQuad quad, int i, float x, float y, float z, float u, float v) {
        quad.setX(i, x);
        quad.setY(i, y);
        quad.setZ(i, z);
        quad.setU(i, u);
        quad.setV(i, v);
    }

    private void updateQuad(ModelQuad quad, BlockPos blockPos,
                            LightPipeline lightPipeline, Direction dir) {

        lightPipeline.calculate(quad, blockPos, resources.quadLightData, null, dir, false);

    }

    private void updateColor(float r, float g, float b, float brightness) {
        QuadLightData quadLightData = resources.quadLightData;

        for (int i = 0; i < 4; i++) {
            float br = quadLightData.br[i] * brightness;
            float r1 = r * br;
            float g1 = g * br;
            float b1 = b * br;

            this.quadColors[i] = ColorUtil.RGBA.pack(r1, g1, b1, 1.0f);
        }
    }
}
