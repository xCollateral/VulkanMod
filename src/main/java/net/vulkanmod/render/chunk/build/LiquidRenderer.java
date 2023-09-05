package net.vulkanmod.render.chunk.build;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class LiquidRenderer {
    private static final float MAX_FLUID_HEIGHT = 0.8888889F;
    private final TextureAtlasSprite[] lavaIcons = new TextureAtlasSprite[2];
    private final TextureAtlasSprite[] waterIcons = new TextureAtlasSprite[2];
    private TextureAtlasSprite waterOverlay;

    public void setupSprites() {
        this.lavaIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon();
        this.lavaIcons[1] = ModelBakery.LAVA_FLOW.sprite();
        this.waterIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
        this.waterIcons[1] = ModelBakery.WATER_FLOW.sprite();
        this.waterOverlay = ModelBakery.WATER_OVERLAY.sprite();
    }

    private static boolean isNeighborSameFluid(FluidState fluidState, FluidState fluidState2) {
        return fluidState2.getType().isSame(fluidState.getType());
    }

    private static boolean isFaceOccludedByState(BlockGetter blockGetter, Direction direction, float f, BlockPos blockPos, BlockState blockState) {
        if (blockState.canOcclude()) {
            VoxelShape voxelShape = Shapes.box(0.0, 0.0, 0.0, 1.0, f, 1.0);
            VoxelShape voxelShape2 = blockState.getOcclusionShape(blockGetter, blockPos);
            return Shapes.blockOccudes(voxelShape, voxelShape2, direction);
        } else {
            return false;
        }
    }

    private static boolean isFaceOccludedByNeighbor(BlockGetter blockGetter, BlockPos blockPos, Direction direction, float f, BlockState blockState) {
        return isFaceOccludedByState(blockGetter, direction, f, blockPos.relative(direction), blockState);
    }

    private static boolean isFaceOccludedBySelf(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, Direction direction) {
        return isFaceOccludedByState(blockGetter, direction.getOpposite(), 1.0F, blockPos, blockState);
    }

    public static boolean shouldRenderFace(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, FluidState fluidState, BlockState blockState, Direction direction, FluidState fluidState2) {
        return !isFaceOccludedBySelf(blockAndTintGetter, blockPos, blockState, direction) && !isNeighborSameFluid(fluidState, fluidState2);
    }

    public void tessellate(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState) {
        boolean bl = fluidState.is(FluidTags.LAVA);
        TextureAtlasSprite[] textureAtlasSprites = bl ? this.lavaIcons : this.waterIcons;
        int i = bl ? 16777215 : BiomeColors.getAverageWaterColor(blockAndTintGetter, blockPos);
        float f = (float)(i >> 16 & 255) / 255.0F;
        float g = (float)(i >> 8 & 255) / 255.0F;
        float h = (float)(i & 255) / 255.0F;

        BlockState blockState2 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.DOWN));
        FluidState fluidState2 = blockState2.getFluidState();
        BlockState blockState3 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.UP));
        FluidState fluidState3 = blockState3.getFluidState();
        BlockState blockState4 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.NORTH));
        FluidState fluidState4 = blockState4.getFluidState();
        BlockState blockState5 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.SOUTH));
        FluidState fluidState5 = blockState5.getFluidState();
        BlockState blockState6 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.WEST));
        FluidState fluidState6 = blockState6.getFluidState();
        BlockState blockState7 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.EAST));
        FluidState fluidState7 = blockState7.getFluidState();

        boolean bl2 = !isNeighborSameFluid(fluidState, fluidState3);
        boolean renderDownFace = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.DOWN, fluidState2) && !isFaceOccludedByNeighbor(blockAndTintGetter, blockPos, Direction.DOWN, 0.8888889F, blockState2);
        boolean bl4 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.NORTH, fluidState4);
        boolean bl5 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.SOUTH, fluidState5);
        boolean bl6 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.WEST, fluidState6);
        boolean bl7 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.EAST, fluidState7);

        if (bl2 || renderDownFace || bl7 || bl6 || bl4 || bl5)
        {
            float j = blockAndTintGetter.getShade(Direction.DOWN, true);
            float k = blockAndTintGetter.getShade(Direction.UP, true);
            float l = blockAndTintGetter.getShade(Direction.NORTH, true);
            float m = blockAndTintGetter.getShade(Direction.WEST, true);
            Fluid fluid = fluidState.getType();
            float n = this.getHeight(blockAndTintGetter, fluid, blockPos, blockState, fluidState);
            float o;
            float p;
            float q;
            float r;
            if (n >= 1.0F) {
                o = 1.0F;
                p = 1.0F;
                q = 1.0F;
                r = 1.0F;
            } else {
                float s = this.getHeight(blockAndTintGetter, fluid, blockPos.north(), blockState4, fluidState4);
                float t = this.getHeight(blockAndTintGetter, fluid, blockPos.south(), blockState5, fluidState5);
                float u = this.getHeight(blockAndTintGetter, fluid, blockPos.east(), blockState7, fluidState7);
                float v = this.getHeight(blockAndTintGetter, fluid, blockPos.west(), blockState6, fluidState6);
                o = this.calculateAverageHeight(blockAndTintGetter, fluid, n, s, u, blockPos.relative(Direction.NORTH).relative(Direction.EAST));
                p = this.calculateAverageHeight(blockAndTintGetter, fluid, n, s, v, blockPos.relative(Direction.NORTH).relative(Direction.WEST));
                q = this.calculateAverageHeight(blockAndTintGetter, fluid, n, t, u, blockPos.relative(Direction.SOUTH).relative(Direction.EAST));
                r = this.calculateAverageHeight(blockAndTintGetter, fluid, n, t, v, blockPos.relative(Direction.SOUTH).relative(Direction.WEST));
            }

            double x0 = (blockPos.getX() & 15);
            double y0 = (blockPos.getY() & 15);
            double z0 = (blockPos.getZ() & 15);
            float x = 0.001F;
            float y = renderDownFace ? 0.001F : 0.0F;
            float z;
            float ab;
            float ad;
            float af;
            float aa;
            float ac;
            float ae;
            float ag;

            if (bl2 && !isFaceOccludedByNeighbor(blockAndTintGetter, blockPos, Direction.UP, Math.min(Math.min(p, r), Math.min(q, o)), blockState3)) {
                p -= 0.001F;
                r -= 0.001F;
                q -= 0.001F;
                o -= 0.001F;
                Vec3 vec3 = fluidState.getFlow(blockAndTintGetter, blockPos);
                TextureAtlasSprite textureAtlasSprite;
                float ah;
                float ai;
                float r1;
                if (vec3.x == 0.0 && vec3.z == 0.0) {
                    textureAtlasSprite = textureAtlasSprites[0];
                    z = textureAtlasSprite.getU(0.0);
                    aa = textureAtlasSprite.getV(0.0);
                    ab = z;
                    ac = textureAtlasSprite.getV(16.0);
                    ad = textureAtlasSprite.getU(16.0);
                    ae = ac;
                    af = ad;
                    ag = aa;
                } else {
                    textureAtlasSprite = textureAtlasSprites[1];
                    ah = (float)Mth.atan2(vec3.z, vec3.x) - 1.5707964F;
                    ai = Mth.sin(ah) * 0.25F;
                    float aj = Mth.cos(ah) * 0.25F;
                    r1 = 8.0F;
                    z = textureAtlasSprite.getU((8.0F + (-aj - ai) * 16.0F));
                    aa = textureAtlasSprite.getV((8.0F + (-aj + ai) * 16.0F));
                    ab = textureAtlasSprite.getU((8.0F + (-aj + ai) * 16.0F));
                    ac = textureAtlasSprite.getV((8.0F + (aj + ai) * 16.0F));
                    ad = textureAtlasSprite.getU((8.0F + (aj + ai) * 16.0F));
                    ae = textureAtlasSprite.getV((8.0F + (aj - ai) * 16.0F));
                    af = textureAtlasSprite.getU((8.0F + (aj - ai) * 16.0F));
                    ag = textureAtlasSprite.getV((8.0F + (-aj - ai) * 16.0F));
                }

                float al = (z + ab + ad + af) / 4.0F;
                ah = (aa + ac + ae + ag) / 4.0F;
                ai = textureAtlasSprites[0].uvShrinkRatio();
                z = Mth.lerp(ai, z, al);
                ab = Mth.lerp(ai, ab, al);
                ad = Mth.lerp(ai, ad, al);
                af = Mth.lerp(ai, af, al);
                aa = Mth.lerp(ai, aa, ah);
                ac = Mth.lerp(ai, ac, ah);
                ae = Mth.lerp(ai, ae, ah);
                ag = Mth.lerp(ai, ag, ah);
                int am = this.getLightColor(blockAndTintGetter, blockPos);
                r1 = k * f;
                float g1 = k * g;
                float b1 = k * h;

//                this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)p, z0 + 0.0, r1, g1, b1, z, aa, am);
//                this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)r, z0 + 1.0, r1, g1, b1, ab, ac, am);
//                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)q, z0 + 1.0, r1, g1, b1, ad, ae, am);
//                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)o, z0 + 0.0, r1, g1, b1, af, ag, am);
//                if (fluidState.shouldRenderBackwardUpFace(blockAndTintGetter, blockPos.above())) {
//                    this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)p, z0 + 0.0, r1, g1, b1, z, aa, am);
//                    this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)o, z0 + 0.0, r1, g1, b1, af, ag, am);
//                    this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)q, z0 + 1.0, r1, g1, b1, ad, ae, am);
//                    this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)r, z0 + 1.0, r1, g1, b1, ab, ac, am);
//                }

                Vector3f normal = new Vector3f(0.0f, r - p, 1.0f).cross(1.0f, q - p, 1.0f);
                normal.normalize();

                this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)p, z0 + 0.0, r1, g1, b1, z, aa, am, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)r, z0 + 1.0, r1, g1, b1, ab, ac, am, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)q, z0 + 1.0, r1, g1, b1, ad, ae, am, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)o, z0 + 0.0, r1, g1, b1, af, ag, am, normal.x(), normal.y(), normal.z());
                if (fluidState.shouldRenderBackwardUpFace(blockAndTintGetter, blockPos.above())) {
                    this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)p, z0 + 0.0, r1, g1, b1, z, aa, am, normal.x(), normal.y(), normal.z());
                    this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)o, z0 + 0.0, r1, g1, b1, af, ag, am, normal.x(), normal.y(), normal.z());
                    this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)q, z0 + 1.0, r1, g1, b1, ad, ae, am, normal.x(), normal.y(), normal.z());
                    this.vertex(vertexConsumer, x0 + 0.0, y0 + (double)r, z0 + 1.0, r1, g1, b1, ab, ac, am, normal.x(), normal.y(), normal.z());
                }
            }

            if (renderDownFace) {
                z = textureAtlasSprites[0].getU0();
                ab = textureAtlasSprites[0].getU1();
                ad = textureAtlasSprites[0].getV0();
                af = textureAtlasSprites[0].getV1();
                int ap = this.getLightColor(blockAndTintGetter, blockPos.below());
                ac = j * f;
                ae = j * g;
                ag = j * h;

//                this.vertex(vertexConsumer, x0, y0 + (double)y, z0 + 1.0, ac, ae, ag, z, af, ap);
//                this.vertex(vertexConsumer, x0, y0 + (double)y, z0, ac, ae, ag, z, ad, ap);
//                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)y, z0, ac, ae, ag, ab, ad, ap);
//                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)y, z0 + 1.0, ac, ae, ag, ab, af, ap);

                Vector3f normal = new Vector3f(0.0f, 0.0f, -1.0f).cross(1.0f, 0.0f, -1.0f);
                normal.normalize();

                this.vertex(vertexConsumer, x0, y0 + (double)y, z0 + 1.0, ac, ae, ag, z, af, ap, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, x0, y0 + (double)y, z0, ac, ae, ag, z, ad, ap, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)y, z0, ac, ae, ag, ab, ad, ap, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, x0 + 1.0, y0 + (double)y, z0 + 1.0, ac, ae, ag, ab, af, ap,normal.x(), normal.y(), normal.z());
            }

            int aq = this.getLightColor(blockAndTintGetter, blockPos);
            Iterator<Direction> var76 = Direction.Plane.HORIZONTAL.iterator();

            while(true) {
                Direction direction;
                double ar;
                double at;
                double as;
                double au;
                boolean bl8;
                do {
                    do {
                        if (!var76.hasNext()) {
                            return;
                        }

                        direction = var76.next();
                        switch (direction) {
                            case NORTH -> {
                                af = p;
                                aa = o;
                                ar = x0;
                                as = x0 + 1.0;
                                at = z0 + 0.0010000000474974513;
                                au = z0 + 0.0010000000474974513;
                                bl8 = bl4;
                            }
                            case SOUTH -> {
                                af = q;
                                aa = r;
                                ar = x0 + 1.0;
                                as = x0;
                                at = z0 + 1.0 - 0.0010000000474974513;
                                au = z0 + 1.0 - 0.0010000000474974513;
                                bl8 = bl5;
                            }
                            case WEST -> {
                                af = r;
                                aa = p;
                                ar = x0 + 0.0010000000474974513;
                                as = x0 + 0.0010000000474974513;
                                at = z0 + 1.0;
                                au = z0;
                                bl8 = bl6;
                            }
                            default -> {
                                af = o;
                                aa = q;
                                ar = x0 + 1.0 - 0.0010000000474974513;
                                as = x0 + 1.0 - 0.0010000000474974513;
                                at = z0;
                                au = z0 + 1.0;
                                bl8 = bl7;
                            }
                        }
                    } while(!bl8);
                } while(isFaceOccludedByNeighbor(blockAndTintGetter, blockPos, direction, Math.max(af, aa), blockAndTintGetter.getBlockState(blockPos.relative(direction))));

                BlockPos blockPos2 = blockPos.relative(direction);
                TextureAtlasSprite textureAtlasSprite2 = textureAtlasSprites[1];
                if (!bl) {
                    Block block = blockAndTintGetter.getBlockState(blockPos2).getBlock();
                    if (block instanceof HalfTransparentBlock || block instanceof LeavesBlock) {
                        textureAtlasSprite2 = this.waterOverlay;
                    }
                }

                float av = textureAtlasSprite2.getU(0.0);
                float aw = textureAtlasSprite2.getU(8.0);
                float ax = textureAtlasSprite2.getV((double)((1.0F - af) * 16.0F * 0.5F));
                float ay = textureAtlasSprite2.getV((double)((1.0F - aa) * 16.0F * 0.5F));
                float az = textureAtlasSprite2.getV(8.0);
                float ba = direction.getAxis() == Direction.Axis.Z ? l : m;
                float bb = k * ba * f;
                float bc = k * ba * g;
                float bd = k * ba * h;

//                this.vertex(vertexConsumer, ar, y0 + (double)af, at, bb, bc, bd, av, ax, aq);
//                this.vertex(vertexConsumer, as, y0 + (double)aa, au, bb, bc, bd, aw, ay, aq);
//                this.vertex(vertexConsumer, as, y0 + (double)y, au, bb, bc, bd, aw, az, aq);
//                this.vertex(vertexConsumer, ar, y0 + (double)y, at, bb, bc, bd, av, az, aq);
//                if (textureAtlasSprite2 != this.waterOverlay) {
//                    this.vertex(vertexConsumer, ar, y0 + (double)y, at, bb, bc, bd, av, az, aq);
//                    this.vertex(vertexConsumer, as, y0 + (double)y, au, bb, bc, bd, aw, az, aq);
//                    this.vertex(vertexConsumer, as, y0 + (double)aa, au, bb, bc, bd, aw, ay, aq);
//                    this.vertex(vertexConsumer, ar, y0 + (double)af, at, bb, bc, bd, av, ax, aq);
//                }

                Vector3f normal = new Vector3f((float) (as - ar), (aa - af), (float) (au - at)).cross((float) (as - ar), (y - af), (float) (au - at));
                normal.normalize();

                this.vertex(vertexConsumer, ar, y0 + (double)af, at, bb, bc, bd, av, ax, aq, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, as, y0 + (double)aa, au, bb, bc, bd, aw, ay, aq, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, as, y0 + (double)y, au, bb, bc, bd, aw, az, aq, normal.x(), normal.y(), normal.z());
                this.vertex(vertexConsumer, ar, y0 + (double)y, at, bb, bc, bd, av, az, aq, normal.x(), normal.y(), normal.z());
                if (textureAtlasSprite2 != this.waterOverlay) {
                    this.vertex(vertexConsumer, ar, y0 + (double)y, at, bb, bc, bd, av, az, aq);
                    this.vertex(vertexConsumer, as, y0 + (double)y, au, bb, bc, bd, aw, az, aq);
                    this.vertex(vertexConsumer, as, y0 + (double)aa, au, bb, bc, bd, aw, ay, aq);
                    this.vertex(vertexConsumer, ar, y0 + (double)af, at, bb, bc, bd, av, ax, aq);
                }
            }
        }
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
        return this.getHeight(blockAndTintGetter, fluid, blockPos, blockState, blockState.getFluidState());
    }

    private float getHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
        if (fluid.isSame(fluidState.getType())) {
            BlockState blockState2 = blockAndTintGetter.getBlockState(blockPos.above());
            return fluid.isSame(blockState2.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
        } else {
            return !blockState.isSolid() ? 0.0F : -1.0F;
        }
    }

    private void vertex(VertexConsumer vertexConsumer, double x, double y, double z, float r, float g, float b, float u, float v, int l) {
//        vertexConsumer.vertex(x, y, z).color(r, g, b, 1.0F).uv(u, v).uv2(l).normal(0.0F, 1.0F, 0.0F).endVertex();
        vertexConsumer.vertex((float) x, (float) y, (float) z, r, g, b, 1.0f, u, v, 0, l, 0.0F, 1.0F, 0.0F);
    }

    private void vertex(VertexConsumer vertexConsumer, double x, double y, double z, float r, float g, float b, float u, float v, int l, float normalX, float normalY, float normalZ) {
//        vertexConsumer.vertex(x, y, z).color(r, g, b, 1.0F).uv(u, v).uv2(l).normal(0.0F, 1.0F, 0.0F).endVertex();
        vertexConsumer.vertex((float) x, (float) y, (float) z, r, g, b, 1.0f, u, v, 0, l, normalX, normalY, normalZ);
    }

    private void putQuad() {

    }

    private int getLightColor(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos) {
        int i = LevelRenderer.getLightColor(blockAndTintGetter, blockPos);
        int j = LevelRenderer.getLightColor(blockAndTintGetter, blockPos.above());
        int k = i & 255;
        int l = j & 255;
        int m = i >> 16 & 255;
        int n = j >> 16 & 255;
        return (Math.max(k, l)) | (Math.max(m, n)) << 16;
    }
}
