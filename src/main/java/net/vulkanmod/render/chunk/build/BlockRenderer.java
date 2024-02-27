package net.vulkanmod.render.chunk.build;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.BitSet;
import java.util.List;


public class BlockRenderer {
    private static final int FACE_CUBIC = 0;
    private static final int FACE_PARTIAL = 1;
    static final Direction[] DIRECTIONS = Direction.values();
    private static BlockColors blockColors;

    final Cache cache = new Cache();
    byte flags = 0;
    float[] fs = new float[DIRECTIONS.length * 2];

    final Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> occlusionCache = new Object2ByteLinkedOpenHashMap<>(2048, 0.25F) {
        protected void rehash(int i) {
        }
    };

    public BlockRenderer() {
        occlusionCache.defaultReturnValue((byte)127);
    }

    public static void setBlockColors(BlockColors blockColors) {
        BlockRenderer.blockColors = blockColors;
    }

    public void renderBatched(BlockState blockState, BlockPos blockPos, BlockAndTintGetter blockAndTintGetter, Vector3f pos, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource) {
        try {
            RenderShape renderShape = blockState.getRenderShape();
            if (renderShape == RenderShape.MODEL) {
                BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
                tessellateBlock(blockAndTintGetter, model, blockState, blockPos, pos, vertexConsumer, bl, randomSource, blockState.getSeed(blockPos));
            }

        } catch (Throwable var11) {
            CrashReport crashReport = CrashReport.forThrowable(var11, "Tesselating block in world");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashReportCategory, blockAndTintGetter, blockPos, blockState);
            throw new ReportedException(crashReport);
        }
    }

    public void tessellateBlock(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, Vector3f pos, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource, long l) {
        boolean bl2 = Minecraft.useAmbientOcclusion() && blockState.getLightEmission() == 0 && bakedModel.useAmbientOcclusion();
        Vec3 vec3 = blockState.getOffset(blockAndTintGetter, blockPos);

        pos.add((float) vec3.x, (float) vec3.y, (float) vec3.z);

        try {
            if (bl2) {
                tessellateWithAO(blockAndTintGetter, bakedModel, blockState, blockPos, pos, vertexConsumer, bl, randomSource, l);
            } else {
                tessellateWithoutAO(blockAndTintGetter, bakedModel, blockState, blockPos, pos, vertexConsumer, bl, randomSource, l);
            }

        } catch (Throwable var17) {
            CrashReport crashReport = CrashReport.forThrowable(var17, "Tesselating block model");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashReportCategory, blockAndTintGetter, blockPos, blockState);
            crashReportCategory.setDetail("Using AO", bl2);
            throw new ReportedException(crashReport);
        }
    }

    public void tessellateWithAO(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, Vector3f pos, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource, long l) {
//        float[] fs = new float[DIRECTIONS.length * 2];
//        BitSet bitSet = new BitSet(3);
//        Arrays.fill(fs, 0.0f);
        flags = 0;

        AmbientOcclusionFace ambientOcclusionFace = new AmbientOcclusionFace();
        BlockPos.MutableBlockPos mutableBlockPos = blockPos.mutable();

        for (Direction direction : DIRECTIONS) {
            randomSource.setSeed(l);
            List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, randomSource);
            if (!quads.isEmpty()) {
                mutableBlockPos.setWithOffset(blockPos, direction);
                if (!bl || shouldRenderFace(blockState, blockAndTintGetter, blockPos, direction, mutableBlockPos)) {
                    renderModelFaceAO(blockAndTintGetter, blockState, blockPos, pos, vertexConsumer, quads, ambientOcclusionFace);
                }
            }
        }

        randomSource.setSeed(l);
        List<BakedQuad> list2 = bakedModel.getQuads(blockState, null, randomSource);
        if (!list2.isEmpty()) {
            renderModelFaceAO(blockAndTintGetter, blockState, blockPos, pos, vertexConsumer, list2, ambientOcclusionFace);
        }

    }

    public void tessellateWithoutAO(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, Vector3f pos, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource, long l) {
        BitSet bitSet = new BitSet(3);
        BlockPos.MutableBlockPos mutableBlockPos = blockPos.mutable();

        for (Direction direction : DIRECTIONS) {
            randomSource.setSeed(l);
            List<BakedQuad> list = bakedModel.getQuads(blockState, direction, randomSource);
            if (!list.isEmpty()) {
                mutableBlockPos.setWithOffset(blockPos, direction);
                if (!bl || shouldRenderFace(blockState, blockAndTintGetter, blockPos, direction, mutableBlockPos)) {
                    int j = LevelRenderer.getLightColor(blockAndTintGetter, blockState, mutableBlockPos);
                    renderModelFaceFlat(blockAndTintGetter, blockState, blockPos, j, false, pos, vertexConsumer, list, bitSet);
                }
            }
        }

        randomSource.setSeed(l);
        List<BakedQuad> list2 = bakedModel.getQuads(blockState, null, randomSource);
        if (!list2.isEmpty()) {
            renderModelFaceFlat(blockAndTintGetter, blockState, blockPos, -1, true, pos, vertexConsumer, list2, bitSet);
        }

    }

    private void renderModelFaceAO(BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos, Vector3f pos, VertexConsumer vertexConsumer,
                                          List<BakedQuad> quads, AmbientOcclusionFace ambientOcclusionFace) {
        for (BakedQuad bakedQuad : quads) {
            calculateShape(blockAndTintGetter, blockState, blockPos, bakedQuad.getVertices(), bakedQuad.getDirection(), fs);
            ambientOcclusionFace.calculate(this.cache, blockAndTintGetter, blockState, blockPos, bakedQuad.getDirection(), fs, flags, bakedQuad.isShade());
            putQuadData(blockAndTintGetter, blockState, blockPos, vertexConsumer, pos, bakedQuad,
                    ambientOcclusionFace.brightness, ambientOcclusionFace.lightmap);
        }
    }

    private static void putQuadData(BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos, VertexConsumer vertexConsumer, Vector3f pos, BakedQuad bakedQuad,
                                    float[] brightness, int[] lightmap) {
        float r, g, b;
        if (bakedQuad.isTinted()) {
            int o = blockColors.getColor(blockState, blockAndTintGetter, blockPos, bakedQuad.getTintIndex());
            r = ColorUtil.ARGB.unpackR(o);
            g = ColorUtil.ARGB.unpackG(o);
            b = ColorUtil.ARGB.unpackB(o);
        } else {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        putBulkData(vertexConsumer, pos, bakedQuad, brightness, r, g, b, lightmap, OverlayTexture.NO_OVERLAY);
    }

    public static void putBulkData(VertexConsumer vertexConsumer, Vector3f pos, BakedQuad quad, float[] brightness, float red, float green, float blue, int[] lights, int overlay) {
        int[] js = quad.getVertices();
        Vec3i normal = quad.getDirection().getNormal();

        int j = js.length / 8;

        for (int k = 0; k < j; ++k) {
            float r, g, b;

            float quadR, quadG, quadB;

            int i = k * 8;
            float x = Float.intBitsToFloat(js[i]);
            float y = Float.intBitsToFloat(js[i + 1]);
            float z = Float.intBitsToFloat(js[i + 2]);

            quadR = ColorUtil.RGBA.unpackR(js[i + 3]);
            quadG = ColorUtil.RGBA.unpackR(js[i + 3]);
            quadB = ColorUtil.RGBA.unpackR(js[i + 3]);
            r = quadR * brightness[k] * red;
            g = quadG * brightness[k] * green;
            b = quadB * brightness[k] * blue;

            int light = lights[k];
            float u = Float.intBitsToFloat(js[i + 4]);
            float v = Float.intBitsToFloat(js[i + 5]);

            Vector4f vector4f = new Vector4f(x + pos.x(), y + pos.y(), z + pos.z(), 1.0f);

            vertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), r, g, b, 1.0f, u, v, overlay, light, normal.getX(), normal.getY(), normal.getZ());
        }

    }

    private void calculateShape(BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos, int[] is, Direction direction, @Nullable float[] fs) {
        float maxX = 32.0F;
        float maxY = 32.0F;
        float maxZ = 32.0F;
        float minX = -32.0F;
        float minY = -32.0F;
        float minZ = -32.0F;

        int l;
        for(l = 0; l < 4; ++l) {
            float m = Float.intBitsToFloat(is[l * 8]);
            float n = Float.intBitsToFloat(is[l * 8 + 1]);
            float o = Float.intBitsToFloat(is[l * 8 + 2]);
            maxX = Math.min(maxX, m);
            maxY = Math.min(maxY, n);
            maxZ = Math.min(maxZ, o);
            minX = Math.max(minX, m);
            minY = Math.max(minY, n);
            minZ = Math.max(minZ, o);
        }

        if (fs != null) {
            fs[Direction.WEST.get3DDataValue()] = maxX;
            fs[Direction.EAST.get3DDataValue()] = minX;
            fs[Direction.DOWN.get3DDataValue()] = maxY;
            fs[Direction.UP.get3DDataValue()] = minY;
            fs[Direction.NORTH.get3DDataValue()] = maxZ;
            fs[Direction.SOUTH.get3DDataValue()] = minZ;
            l = DIRECTIONS.length;
            fs[Direction.WEST.get3DDataValue() + l] = 1.0F - maxX;
            fs[Direction.EAST.get3DDataValue() + l] = 1.0F - minX;
            fs[Direction.DOWN.get3DDataValue() + l] = 1.0F - maxY;
            fs[Direction.UP.get3DDataValue() + l] = 1.0F - minY;
            fs[Direction.NORTH.get3DDataValue() + l] = 1.0F - maxZ;
            fs[Direction.SOUTH.get3DDataValue() + l] = 1.0F - minZ;
        }

        boolean f1 = false, f2 = false;
        switch (direction) {
            case DOWN -> {
                f1 = (maxX >= 0.0001F || maxZ >= 0.0001F || minX <= 0.9999F || minZ <= 0.9999F);
                f2 = (maxY == minY && (maxY < 0.0001F || blockState.isCollisionShapeFullBlock(blockAndTintGetter, blockPos)));
            }
            case UP -> {
                f1 = (maxX >= 0.0001F || maxZ >= 0.0001F || minX <= 0.9999F || minZ <= 0.9999F);
                f2 = (maxY == minY && (minY > 0.9999F || blockState.isCollisionShapeFullBlock(blockAndTintGetter, blockPos)));
            }
            case NORTH -> {
                f1 = (maxX >= 0.0001F || maxY >= 0.0001F || minX <= 0.9999F || minY <= 0.9999F);
                f2 = (maxZ == minZ && (maxZ < 0.0001F || blockState.isCollisionShapeFullBlock(blockAndTintGetter, blockPos)));
            }
            case SOUTH -> {
                f1 = (maxX >= 0.0001F || maxY >= 0.0001F || minX <= 0.9999F || minY <= 0.9999F);
                f2 = (maxZ == minZ && (minZ > 0.9999F || blockState.isCollisionShapeFullBlock(blockAndTintGetter, blockPos)));
            }
            case WEST -> {
                f1 = (maxY >= 0.0001F || maxZ >= 0.0001F || minY <= 0.9999F || minZ <= 0.9999F);
                f2 = (maxX == minX && (maxX < 0.0001F || blockState.isCollisionShapeFullBlock(blockAndTintGetter, blockPos)));
            }
            case EAST -> {
                f1 = (maxY >= 0.0001F || maxZ >= 0.0001F || minY <= 0.9999F || minZ <= 0.9999F);
                f2 = (maxX == minX && (minX > 0.9999F || blockState.isCollisionShapeFullBlock(blockAndTintGetter, blockPos)));
            }
        }

        flags |= (byte) (f1 ? 0b10 : 0);
        flags |= (byte) (f2 ? 0b01 : 0);

    }

    private void renderModelFaceFlat(BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos, int i, boolean bl, Vector3f pos, VertexConsumer vertexConsumer, List<BakedQuad> list, BitSet bitSet) {
        for (BakedQuad bakedQuad : list) {
            if (bl) {
                calculateShape(blockAndTintGetter, blockState, blockPos, bakedQuad.getVertices(), bakedQuad.getDirection(), null);
                BlockPos blockPos2 = bitSet.get(0) ? blockPos.relative(bakedQuad.getDirection()) : blockPos;
                i = LevelRenderer.getLightColor(blockAndTintGetter, blockState, blockPos2);
            }

            float f = blockAndTintGetter.getShade(bakedQuad.getDirection(), bakedQuad.isShade());
            putQuadData(blockAndTintGetter, blockState, blockPos, vertexConsumer, pos, bakedQuad, new float[]{f, f, f, f}, new int[] {i , i, i, i});
        }
    }

    public void renderModel(PoseStack.Pose pose, VertexConsumer vertexConsumer, @Nullable BlockState blockState, BakedModel bakedModel, float f, float g, float h, int i, int j) {
        RandomSource randomSource = RandomSource.create();
        long l = 42L;

        for (Direction direction : DIRECTIONS) {
            randomSource.setSeed(42L);
            renderQuadList(pose, vertexConsumer, f, g, h, bakedModel.getQuads(blockState, direction, randomSource), i, j);
        }

        randomSource.setSeed(42L);
        renderQuadList(pose, vertexConsumer, f, g, h, bakedModel.getQuads(blockState, null, randomSource), i, j);
    }

    private static void renderQuadList(PoseStack.Pose pose, VertexConsumer vertexConsumer, float f, float g, float h, List<BakedQuad> list, int i, int j) {
        BakedQuad bakedQuad;
        float k;
        float l;
        float m;
        for (BakedQuad quad : list) {
            bakedQuad = quad;
            if (bakedQuad.isTinted()) {
                k = Mth.clamp(f, 0.0F, 1.0F);
                l = Mth.clamp(g, 0.0F, 1.0F);
                m = Mth.clamp(h, 0.0F, 1.0F);
            } else {
                k = 1.0F;
                l = 1.0F;
                m = 1.0F;
            }

            vertexConsumer.putBulkData(pose, bakedQuad, k, l, m, i, j);
        }
    }

    public void enableCaching() {
        cache.enable();
    }

    public void clearCache() {
        cache.disable();
    }

    public boolean shouldRenderFace(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction, BlockPos blockPos2) {
        BlockState blockState2 = blockGetter.getBlockState(blockPos2);
        if (blockState.skipRendering(blockState2, direction)) {
            return false;
        } else if (blockState2.canOcclude()) {
            Block.BlockStatePairKey blockStatePairKey = new Block.BlockStatePairKey(blockState, blockState2, direction);

            byte b = occlusionCache.getAndMoveToFirst(blockStatePairKey);
            if (b != 127) {
                return b != 0;
            } else {
                VoxelShape voxelShape = blockState.getFaceOcclusionShape(blockGetter, blockPos, direction);
                if (voxelShape.isEmpty()) {
                    return true;
                } else {
                    VoxelShape voxelShape2 = blockState2.getFaceOcclusionShape(blockGetter, blockPos2, direction.getOpposite());
                    boolean bl = Shapes.joinIsNotEmpty(voxelShape, voxelShape2, BooleanOp.ONLY_FIRST);
                    if (occlusionCache.size() == 2048) {
                        occlusionCache.removeLastByte();
                    }

                    occlusionCache.putAndMoveToFirst(blockStatePairKey, (byte)(bl ? 1 : 0));
                    return bl;
                }
            }
        } else {
            return true;
        }
    }
    
    static class AmbientOcclusionFace {
        final float[] brightness = new float[4];
        final int[] lightmap = new int[4];

        public AmbientOcclusionFace() {
        }

        public void calculate(Cache cache, BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos, Direction direction, float[] fs, byte flags, boolean bl) {
            BlockPos blockPos2 = ((flags & 0b1) != 0) ? blockPos.relative(direction) : blockPos;
            AdjacencyInfo adjacencyInfo = AdjacencyInfo.fromFacing(direction);
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[0]);
            BlockState blockState2 = blockAndTintGetter.getBlockState(mutableBlockPos);
            int i = cache.getLightColor(blockState2, blockAndTintGetter, mutableBlockPos);
            float f = cache.getShadeBrightness(blockState2, blockAndTintGetter, mutableBlockPos);
            mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[1]);
            BlockState blockState3 = blockAndTintGetter.getBlockState(mutableBlockPos);
            int j = cache.getLightColor(blockState3, blockAndTintGetter, mutableBlockPos);
            float g = cache.getShadeBrightness(blockState3, blockAndTintGetter, mutableBlockPos);
            mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[2]);
            BlockState blockState4 = blockAndTintGetter.getBlockState(mutableBlockPos);
            int k = cache.getLightColor(blockState4, blockAndTintGetter, mutableBlockPos);
            float h = cache.getShadeBrightness(blockState4, blockAndTintGetter, mutableBlockPos);
            mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[3]);
            BlockState blockState5 = blockAndTintGetter.getBlockState(mutableBlockPos);
            int l = cache.getLightColor(blockState5, blockAndTintGetter, mutableBlockPos);
            float m = cache.getShadeBrightness(blockState5, blockAndTintGetter, mutableBlockPos);
            BlockState blockState6 = blockAndTintGetter.getBlockState(mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[0]).move(direction));
            boolean bl2 = !blockState6.isViewBlocking(blockAndTintGetter, mutableBlockPos) || blockState6.getLightBlock(blockAndTintGetter, mutableBlockPos) == 0;
            BlockState blockState7 = blockAndTintGetter.getBlockState(mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[1]).move(direction));
            boolean bl3 = !blockState7.isViewBlocking(blockAndTintGetter, mutableBlockPos) || blockState7.getLightBlock(blockAndTintGetter, mutableBlockPos) == 0;
            BlockState blockState8 = blockAndTintGetter.getBlockState(mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[2]).move(direction));
            boolean bl4 = !blockState8.isViewBlocking(blockAndTintGetter, mutableBlockPos) || blockState8.getLightBlock(blockAndTintGetter, mutableBlockPos) == 0;
            BlockState blockState9 = blockAndTintGetter.getBlockState(mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[3]).move(direction));
            boolean bl5 = !blockState9.isViewBlocking(blockAndTintGetter, mutableBlockPos) || blockState9.getLightBlock(blockAndTintGetter, mutableBlockPos) == 0;

            float n;
            int o;
            BlockState blockState10;
            if (!bl4 && !bl2) {
                n = f;
                o = i;
            } else {
                mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[0]).move(adjacencyInfo.corners[2]);
                blockState10 = blockAndTintGetter.getBlockState(mutableBlockPos);
                n = cache.getShadeBrightness(blockState10, blockAndTintGetter, mutableBlockPos);
                o = cache.getLightColor(blockState10, blockAndTintGetter, mutableBlockPos);
            }

            float p;
            int q;
            if (!bl5 && !bl2) {
                p = f;
                q = i;
            } else {
                mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[0]).move(adjacencyInfo.corners[3]);
                blockState10 = blockAndTintGetter.getBlockState(mutableBlockPos);
                p = cache.getShadeBrightness(blockState10, blockAndTintGetter, mutableBlockPos);
                q = cache.getLightColor(blockState10, blockAndTintGetter, mutableBlockPos);
            }

            float r;
            int s;
            if (!bl4 && !bl3) {
                r = f;
                s = i;
            } else {
                mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[1]).move(adjacencyInfo.corners[2]);
                blockState10 = blockAndTintGetter.getBlockState(mutableBlockPos);
                r = cache.getShadeBrightness(blockState10, blockAndTintGetter, mutableBlockPos);
                s = cache.getLightColor(blockState10, blockAndTintGetter, mutableBlockPos);
            }

            float t;
            int u;
            if (!bl5 && !bl3) {
                t = f;
                u = i;
            } else {
                mutableBlockPos.setWithOffset(blockPos2, adjacencyInfo.corners[1]).move(adjacencyInfo.corners[3]);
                blockState10 = blockAndTintGetter.getBlockState(mutableBlockPos);
                t = cache.getShadeBrightness(blockState10, blockAndTintGetter, mutableBlockPos);
                u = cache.getLightColor(blockState10, blockAndTintGetter, mutableBlockPos);
            }

            int v = cache.getLightColor(blockState, blockAndTintGetter, blockPos);
            mutableBlockPos.setWithOffset(blockPos, direction);
            BlockState blockState11 = blockAndTintGetter.getBlockState(mutableBlockPos);
            if (((flags & 0b1) != 0) || !blockState11.isSolidRender(blockAndTintGetter, mutableBlockPos)) {
                v = cache.getLightColor(blockState11, blockAndTintGetter, mutableBlockPos);
            }

            float w = ((flags & 0b1) != 0) ? cache.getShadeBrightness(blockAndTintGetter.getBlockState(blockPos2), blockAndTintGetter, blockPos2) : cache.getShadeBrightness(blockAndTintGetter.getBlockState(blockPos), blockAndTintGetter, blockPos);
            AmbientVertexRemap ambientVertexRemap = AmbientVertexRemap.fromFacing(direction);
            float x;
            float y;
            float z;
            float aa;
            if (((flags & 0b10) != 0) && adjacencyInfo.doNonCubicWeight) {
                x = (m + f + p + w) * 0.25F;
                y = (h + f + n + w) * 0.25F;
                z = (h + g + r + w) * 0.25F;
                aa = (m + g + t + w) * 0.25F;
                float ab = fs[adjacencyInfo.vert0Weights[0].shape] * fs[adjacencyInfo.vert0Weights[1].shape];
                float ac = fs[adjacencyInfo.vert0Weights[2].shape] * fs[adjacencyInfo.vert0Weights[3].shape];
                float ad = fs[adjacencyInfo.vert0Weights[4].shape] * fs[adjacencyInfo.vert0Weights[5].shape];
                float ae = fs[adjacencyInfo.vert0Weights[6].shape] * fs[adjacencyInfo.vert0Weights[7].shape];
                float af = fs[adjacencyInfo.vert1Weights[0].shape] * fs[adjacencyInfo.vert1Weights[1].shape];
                float ag = fs[adjacencyInfo.vert1Weights[2].shape] * fs[adjacencyInfo.vert1Weights[3].shape];
                float ah = fs[adjacencyInfo.vert1Weights[4].shape] * fs[adjacencyInfo.vert1Weights[5].shape];
                float ai = fs[adjacencyInfo.vert1Weights[6].shape] * fs[adjacencyInfo.vert1Weights[7].shape];
                float aj = fs[adjacencyInfo.vert2Weights[0].shape] * fs[adjacencyInfo.vert2Weights[1].shape];
                float ak = fs[adjacencyInfo.vert2Weights[2].shape] * fs[adjacencyInfo.vert2Weights[3].shape];
                float al = fs[adjacencyInfo.vert2Weights[4].shape] * fs[adjacencyInfo.vert2Weights[5].shape];
                float am = fs[adjacencyInfo.vert2Weights[6].shape] * fs[adjacencyInfo.vert2Weights[7].shape];
                float an = fs[adjacencyInfo.vert3Weights[0].shape] * fs[adjacencyInfo.vert3Weights[1].shape];
                float ao = fs[adjacencyInfo.vert3Weights[2].shape] * fs[adjacencyInfo.vert3Weights[3].shape];
                float ap = fs[adjacencyInfo.vert3Weights[4].shape] * fs[adjacencyInfo.vert3Weights[5].shape];
                float aq = fs[adjacencyInfo.vert3Weights[6].shape] * fs[adjacencyInfo.vert3Weights[7].shape];
                this.brightness[ambientVertexRemap.vert0] = x * ab + y * ac + z * ad + aa * ae;
                this.brightness[ambientVertexRemap.vert1] = x * af + y * ag + z * ah + aa * ai;
                this.brightness[ambientVertexRemap.vert2] = x * aj + y * ak + z * al + aa * am;
                this.brightness[ambientVertexRemap.vert3] = x * an + y * ao + z * ap + aa * aq;
                int ar = this.blend(l, i, q, v);
                int as = this.blend(k, i, o, v);
                int at = this.blend(k, j, s, v);
                int au = this.blend(l, j, u, v);
                this.lightmap[ambientVertexRemap.vert0] = this.blend(ar, as, at, au, ab, ac, ad, ae);
                this.lightmap[ambientVertexRemap.vert1] = this.blend(ar, as, at, au, af, ag, ah, ai);
                this.lightmap[ambientVertexRemap.vert2] = this.blend(ar, as, at, au, aj, ak, al, am);
                this.lightmap[ambientVertexRemap.vert3] = this.blend(ar, as, at, au, an, ao, ap, aq);
            } else {
                x = (m + f + p + w) * 0.25F;
                y = (h + f + n + w) * 0.25F;
                z = (h + g + r + w) * 0.25F;
                aa = (m + g + t + w) * 0.25F;
                this.lightmap[ambientVertexRemap.vert0] = this.blend(l, i, q, v);
                this.lightmap[ambientVertexRemap.vert1] = this.blend(k, i, o, v);
                this.lightmap[ambientVertexRemap.vert2] = this.blend(k, j, s, v);
                this.lightmap[ambientVertexRemap.vert3] = this.blend(l, j, u, v);
                this.brightness[ambientVertexRemap.vert0] = x;
                this.brightness[ambientVertexRemap.vert1] = y;
                this.brightness[ambientVertexRemap.vert2] = z;
                this.brightness[ambientVertexRemap.vert3] = aa;
            }

            x = blockAndTintGetter.getShade(direction, bl);

            for(int av = 0; av < this.brightness.length; ++av) {
                this.brightness[av] *= x;
            }

        }

        private int blend(int i, int j, int k, int l) {
            if (i == 0) {
                i = l;
            }

            if (j == 0) {
                j = l;
            }

            if (k == 0) {
                k = l;
            }

            return i + j + k + l >> 2 & 16711935;
        }

        private int blend(int i, int j, int k, int l, float f, float g, float h, float m) {
            int n = (int)((float)(i >> 16 & 255) * f + (float)(j >> 16 & 255) * g + (float)(k >> 16 & 255) * h + (float)(l >> 16 & 255) * m) & 255;
            int o = (int)((float)(i & 255) * f + (float)(j & 255) * g + (float)(k & 255) * h + (float)(l & 255) * m) & 255;
            return n << 16 | o;
        }
    }
    
    public static class Cache {
        private boolean enabled;
        private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
            Long2IntLinkedOpenHashMap long2IntLinkedOpenHashMap = new Long2IntLinkedOpenHashMap(100, 0.25F) {
                protected void rehash(int i) {
                }
            };
            long2IntLinkedOpenHashMap.defaultReturnValue(Integer.MAX_VALUE);
            return long2IntLinkedOpenHashMap;
        });
        private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
            Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
                protected void rehash(int i) {
                }
            };
            long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
            return long2FloatLinkedOpenHashMap;
        });

        public void enable() {
            this.enabled = true;
        }

        public void disable() {
            this.enabled = false;
            this.colorCache.clear();
            this.brightnessCache.clear();
        }

        public int getLightColor(BlockState blockState, BlockAndTintGetter blockAndTintGetter, BlockPos blockPos) {
            long l = blockPos.asLong();
            int i;
            if (this.enabled) {
                i = this.colorCache.get(l);
                if (i != Integer.MAX_VALUE) {
                    return i;
                }
            }

            i = LevelRenderer.getLightColor(blockAndTintGetter, blockState, blockPos);
            if (this.enabled) {
                if (this.colorCache.size() == 100) {
                    this.colorCache.removeFirstInt();
                }

                this.colorCache.put(l, i);
            }

            return i;
        }

        public float getShadeBrightness(BlockState blockState, BlockAndTintGetter blockAndTintGetter, BlockPos blockPos) {
            long l = blockPos.asLong();
            float f;
            if (this.enabled) {
                f = this.brightnessCache.get(l);
                if (!Float.isNaN(f)) {
                    return f;
                }
            }

            f = blockState.getShadeBrightness(blockAndTintGetter, blockPos);
            if (this.enabled) {
                if (this.brightnessCache.size() == 100) {
                    this.brightnessCache.removeFirstFloat();
                }

                this.brightnessCache.put(l, f);
            }

            return f;
        }
    }
    
    protected enum AdjacencyInfo {
        DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true, new SizeInfo[]{SizeInfo.FLIP_WEST, SizeInfo.SOUTH, SizeInfo.FLIP_WEST, SizeInfo.FLIP_SOUTH, SizeInfo.WEST, SizeInfo.FLIP_SOUTH, SizeInfo.WEST, SizeInfo.SOUTH}, new SizeInfo[]{SizeInfo.FLIP_WEST, SizeInfo.NORTH, SizeInfo.FLIP_WEST, SizeInfo.FLIP_NORTH, SizeInfo.WEST, SizeInfo.FLIP_NORTH, SizeInfo.WEST, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.FLIP_EAST, SizeInfo.NORTH, SizeInfo.FLIP_EAST, SizeInfo.FLIP_NORTH, SizeInfo.EAST, SizeInfo.FLIP_NORTH, SizeInfo.EAST, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.FLIP_EAST, SizeInfo.SOUTH, SizeInfo.FLIP_EAST, SizeInfo.FLIP_SOUTH, SizeInfo.EAST, SizeInfo.FLIP_SOUTH, SizeInfo.EAST, SizeInfo.SOUTH}),
        UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true, new SizeInfo[]{SizeInfo.EAST, SizeInfo.SOUTH, SizeInfo.EAST, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_EAST, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_EAST, SizeInfo.SOUTH}, new SizeInfo[]{SizeInfo.EAST, SizeInfo.NORTH, SizeInfo.EAST, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_EAST, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_EAST, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.WEST, SizeInfo.NORTH, SizeInfo.WEST, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_WEST, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_WEST, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.WEST, SizeInfo.SOUTH, SizeInfo.WEST, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_WEST, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_WEST, SizeInfo.SOUTH}),
        NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true, new SizeInfo[]{SizeInfo.UP, SizeInfo.FLIP_WEST, SizeInfo.UP, SizeInfo.WEST, SizeInfo.FLIP_UP, SizeInfo.WEST, SizeInfo.FLIP_UP, SizeInfo.FLIP_WEST}, new SizeInfo[]{SizeInfo.UP, SizeInfo.FLIP_EAST, SizeInfo.UP, SizeInfo.EAST, SizeInfo.FLIP_UP, SizeInfo.EAST, SizeInfo.FLIP_UP, SizeInfo.FLIP_EAST}, new SizeInfo[]{SizeInfo.DOWN, SizeInfo.FLIP_EAST, SizeInfo.DOWN, SizeInfo.EAST, SizeInfo.FLIP_DOWN, SizeInfo.EAST, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_EAST}, new SizeInfo[]{SizeInfo.DOWN, SizeInfo.FLIP_WEST, SizeInfo.DOWN, SizeInfo.WEST, SizeInfo.FLIP_DOWN, SizeInfo.WEST, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_WEST}),
        SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true, new SizeInfo[]{SizeInfo.UP, SizeInfo.FLIP_WEST, SizeInfo.FLIP_UP, SizeInfo.FLIP_WEST, SizeInfo.FLIP_UP, SizeInfo.WEST, SizeInfo.UP, SizeInfo.WEST}, new SizeInfo[]{SizeInfo.DOWN, SizeInfo.FLIP_WEST, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_WEST, SizeInfo.FLIP_DOWN, SizeInfo.WEST, SizeInfo.DOWN, SizeInfo.WEST}, new SizeInfo[]{SizeInfo.DOWN, SizeInfo.FLIP_EAST, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_EAST, SizeInfo.FLIP_DOWN, SizeInfo.EAST, SizeInfo.DOWN, SizeInfo.EAST}, new SizeInfo[]{SizeInfo.UP, SizeInfo.FLIP_EAST, SizeInfo.FLIP_UP, SizeInfo.FLIP_EAST, SizeInfo.FLIP_UP, SizeInfo.EAST, SizeInfo.UP, SizeInfo.EAST}),
        WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new SizeInfo[]{SizeInfo.UP, SizeInfo.SOUTH, SizeInfo.UP, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_UP, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_UP, SizeInfo.SOUTH}, new SizeInfo[]{SizeInfo.UP, SizeInfo.NORTH, SizeInfo.UP, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_UP, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_UP, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.DOWN, SizeInfo.NORTH, SizeInfo.DOWN, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_NORTH, SizeInfo.FLIP_DOWN, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.DOWN, SizeInfo.SOUTH, SizeInfo.DOWN, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_SOUTH, SizeInfo.FLIP_DOWN, SizeInfo.SOUTH}),
        EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new SizeInfo[]{SizeInfo.FLIP_DOWN, SizeInfo.SOUTH, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_SOUTH, SizeInfo.DOWN, SizeInfo.FLIP_SOUTH, SizeInfo.DOWN, SizeInfo.SOUTH}, new SizeInfo[]{SizeInfo.FLIP_DOWN, SizeInfo.NORTH, SizeInfo.FLIP_DOWN, SizeInfo.FLIP_NORTH, SizeInfo.DOWN, SizeInfo.FLIP_NORTH, SizeInfo.DOWN, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.FLIP_UP, SizeInfo.NORTH, SizeInfo.FLIP_UP, SizeInfo.FLIP_NORTH, SizeInfo.UP, SizeInfo.FLIP_NORTH, SizeInfo.UP, SizeInfo.NORTH}, new SizeInfo[]{SizeInfo.FLIP_UP, SizeInfo.SOUTH, SizeInfo.FLIP_UP, SizeInfo.FLIP_SOUTH, SizeInfo.UP, SizeInfo.FLIP_SOUTH, SizeInfo.UP, SizeInfo.SOUTH});

        final Direction[] corners;
        final boolean doNonCubicWeight;
        final SizeInfo[] vert0Weights;
        final SizeInfo[] vert1Weights;
        final SizeInfo[] vert2Weights;
        final SizeInfo[] vert3Weights;
        private static final AdjacencyInfo[] BY_FACING = (AdjacencyInfo[])Util.make(new AdjacencyInfo[6], (adjacencyInfos) -> {
            adjacencyInfos[Direction.DOWN.get3DDataValue()] = DOWN;
            adjacencyInfos[Direction.UP.get3DDataValue()] = UP;
            adjacencyInfos[Direction.NORTH.get3DDataValue()] = NORTH;
            adjacencyInfos[Direction.SOUTH.get3DDataValue()] = SOUTH;
            adjacencyInfos[Direction.WEST.get3DDataValue()] = WEST;
            adjacencyInfos[Direction.EAST.get3DDataValue()] = EAST;
        });

        AdjacencyInfo(Direction[] directions, float f, boolean bl, SizeInfo[] sizeInfos, SizeInfo[] sizeInfos2, SizeInfo[] sizeInfos3, SizeInfo[] sizeInfos4) {
            this.corners = directions;
            this.doNonCubicWeight = bl;
            this.vert0Weights = sizeInfos;
            this.vert1Weights = sizeInfos2;
            this.vert2Weights = sizeInfos3;
            this.vert3Weights = sizeInfos4;
        }

        public static AdjacencyInfo fromFacing(Direction direction) {
            return BY_FACING[direction.get3DDataValue()];
        }
    }
    
    protected enum SizeInfo {
        DOWN(Direction.DOWN, false),
        UP(Direction.UP, false),
        NORTH(Direction.NORTH, false),
        SOUTH(Direction.SOUTH, false),
        WEST(Direction.WEST, false),
        EAST(Direction.EAST, false),
        FLIP_DOWN(Direction.DOWN, true),
        FLIP_UP(Direction.UP, true),
        FLIP_NORTH(Direction.NORTH, true),
        FLIP_SOUTH(Direction.SOUTH, true),
        FLIP_WEST(Direction.WEST, true),
        FLIP_EAST(Direction.EAST, true);

        final int shape;

        SizeInfo(Direction direction, boolean bl) {
            this.shape = direction.get3DDataValue() + (bl ? DIRECTIONS.length : 0);
        }
    }

    
    private enum AmbientVertexRemap {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        final int vert0;
        final int vert1;
        final int vert2;
        final int vert3;
        private static final AmbientVertexRemap[] BY_FACING = Util.make(new AmbientVertexRemap[6], (ambientVertexRemaps) -> {
            ambientVertexRemaps[Direction.DOWN.get3DDataValue()] = DOWN;
            ambientVertexRemaps[Direction.UP.get3DDataValue()] = UP;
            ambientVertexRemaps[Direction.NORTH.get3DDataValue()] = NORTH;
            ambientVertexRemaps[Direction.SOUTH.get3DDataValue()] = SOUTH;
            ambientVertexRemaps[Direction.WEST.get3DDataValue()] = WEST;
            ambientVertexRemaps[Direction.EAST.get3DDataValue()] = EAST;
        });

        AmbientVertexRemap(int j, int k, int l, int m) {
            this.vert0 = j;
            this.vert1 = k;
            this.vert2 = l;
            this.vert3 = m;
        }

        public static AmbientVertexRemap fromFacing(Direction direction) {
            return BY_FACING[direction.get3DDataValue()];
        }
    }
}

