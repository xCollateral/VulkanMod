package net.vulkanmod.render.chunk.build.task;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.VisibilitySetExtended;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.*;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BuildTask extends ChunkTask {
    @Nullable
    protected RenderRegion region;

    public BuildTask(RenderSection renderSection, RenderRegion renderRegion, boolean highPriority) {
        super(renderSection);
        this.region = renderRegion;
        this.highPriority = highPriority;
    }

    public String name() {
        return "rend_chk_rebuild";
    }

    public CompletableFuture<Result> doTask(BuilderResources builderResources) {
        long startTime = System.nanoTime();

        if (this.cancelled.get()) {
            return CompletableFuture.completedFuture(Result.CANCELLED);
        }

        Vec3 vec3 = WorldRenderer.getCameraPos();
        float x = (float)vec3.x;
        float y = (float)vec3.y;
        float z = (float)vec3.z;
        CompileResult compileResult = this.compile(x, y, z, builderResources);

        if (this.cancelled.get()) {
            compileResult.renderedLayers.values().forEach(UploadBuffer::release);
            return CompletableFuture.completedFuture(Result.CANCELLED);
        }

        CompiledSection compiledSection = new CompiledSection();
//                    compiledSection.visibilitySet = compileResult.visibilitySet;
        compiledSection.renderableBlockEntities.addAll(compileResult.blockEntities);
        compiledSection.transparencyState = compileResult.transparencyState;

        compiledSection.isCompletelyEmpty = compileResult.renderedLayers.isEmpty();

        taskDispatcher.scheduleSectionUpdate(renderSection, compileResult.renderedLayers);
        compiledSection.renderTypes.addAll(compileResult.renderedLayers.keySet());

        this.renderSection.updateGlobalBlockEntities(compileResult.globalBlockEntities);
        this.renderSection.setCompiledSection(compiledSection);
        this.renderSection.setVisibility(((VisibilitySetExtended)compileResult.visibilitySet).getVisibility());
        this.renderSection.setCompletelyEmpty(compiledSection.isCompletelyEmpty);

        float buildTime = (System.nanoTime() - startTime) * 0.000001f;

        if(BENCH) {
            builderResources.updateBuildStats((int) buildTime);
        }

        return CompletableFuture.completedFuture(Result.SUCCESSFUL);
    }

    private CompileResult compile(float camX, float camY, float camZ, BuilderResources builderResources) {
        CompileResult compileResults = new CompileResult();
//            CompileResult compileResults = threadResources.compileResult;
//            compileResults.reset();

        BlockPos startBlockPos = new BlockPos(renderSection.xOffset(), renderSection.yOffset(), renderSection.zOffset()).immutable();
//            BlockPos endBlockPos = startBlockPos.offset(15, 15, 15);
        VisGraph visGraph = new VisGraph();

        if(region == null) {
            compileResults.visibilitySet = visGraph.resolve();
            return compileResults;
        }

        Vector3f pos = new Vector3f();
        ThreadBuilderPack bufferBuilders = builderResources.builderPack;
        region.initTintCache(builderResources.tintCache);

        BlockRenderer blockRenderer = builderResources.blockRenderer;
        blockRenderer.enableCaching();

        LiquidRenderer liquidRenderer = builderResources.liquidRenderer;
        liquidRenderer.setupSprites();

        Set<TerrainRenderType> set = EnumSet.noneOf(TerrainRenderType.class);
        RandomSource randomSource = RandomSource.create();
//            BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        for(int y = 0; y < 16; ++y) {
            for(int z = 0; z < 16; ++z) {
                for(int x = 0; x < 16; ++x) {
                    blockPos.set(renderSection.xOffset() + x, renderSection.yOffset() + y, renderSection.zOffset() + z);

                    BlockState blockState = region.getBlockState(blockPos);
                    if (blockState.isSolidRender(region, blockPos)) {
                        visGraph.setOpaque(blockPos);
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity blockEntity = region.getBlockEntity(blockPos);
                        if (blockEntity != null) {
                            this.handleBlockEntity(compileResults, blockEntity);
                        }
                    }

//                    BlockState blockState2 = region.getBlockState(blockPos);
                    FluidState fluidState = blockState.getFluidState();
                    TerrainRenderType renderType;
                    TerrainBufferBuilder bufferBuilder;
                    if (!fluidState.isEmpty()) {
                        renderType = TerrainRenderType.get(ItemBlockRenderTypes.getRenderLayer(fluidState));

                        bufferBuilder = setupBufferBuilder(bufferBuilders, set, renderType, fluidState.createLegacyBlock());

                        liquidRenderer.renderLiquid(blockPos, region, bufferBuilder, blockState, fluidState);
                    }

                    if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                        renderType = TerrainRenderType.get(ItemBlockRenderTypes.getChunkRenderType(blockState));

                        bufferBuilder = setupBufferBuilder(bufferBuilders, set, renderType, fluidState.createLegacyBlock());

                        pos.set(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
                        blockRenderer.renderBatched(blockState, blockPos, region, pos, bufferBuilder, true, randomSource);
                    }
                }
            }
        }

        if (set.contains(TerrainRenderType.TRANSLUCENT)) {
            TerrainBufferBuilder bufferBuilder2 = bufferBuilders.builder(TerrainRenderType.TRANSLUCENT);
            if (!bufferBuilder2.isCurrentBatchEmpty()) {
                bufferBuilder2.setQuadSortOrigin(camX - (float)startBlockPos.getX(), camY - (float)startBlockPos.getY(), camZ - (float)startBlockPos.getZ());
                compileResults.transparencyState = bufferBuilder2.getSortState();
            }
        }

        for(TerrainRenderType renderType2 : set) {
            TerrainBufferBuilder.RenderedBuffer renderedBuffer = bufferBuilders.builder(renderType2).endOrDiscardIfEmpty();
            if (renderedBuffer != null) {
                UploadBuffer uploadBuffer = new UploadBuffer(renderedBuffer);
                compileResults.renderedLayers.put(renderType2, uploadBuffer);
            }

            if(renderedBuffer != null)
                renderedBuffer.release();
        }

        blockRenderer.clearCache();

        compileResults.visibilitySet = visGraph.resolve();
        this.region = null;
        return compileResults;
    }

    TerrainBufferBuilder setupBufferBuilder(ThreadBuilderPack bufferBuilders, Set<TerrainRenderType> set, TerrainRenderType renderType, BlockState blockState) {
        //Force compact RenderType
        renderType = compactRenderTypes(renderType);

        TerrainBufferBuilder bufferBuilder = bufferBuilders.builder(renderType);
        if (set.add(renderType)) {
            bufferBuilder.begin(VertexFormat.Mode.QUADS, PipelineManager.TERRAIN_VERTEX_FORMAT);
        }

        bufferBuilder.setBlockAttributes(blockState);
        return bufferBuilder;
    }

    private TerrainRenderType compactRenderTypes(TerrainRenderType renderType) {

        if(Initializer.CONFIG.uniqueOpaqueLayer) {
            if (renderType != TerrainRenderType.TRANSLUCENT) {
                renderType = renderType == TerrainRenderType.TRIPWIRE ? TerrainRenderType.TRANSLUCENT : TerrainRenderType.CUTOUT_MIPPED;
            }
        }
        else {
            if (renderType != TerrainRenderType.TRANSLUCENT && renderType != TerrainRenderType.CUTOUT) {
                renderType = renderType == TerrainRenderType.TRIPWIRE ? TerrainRenderType.TRANSLUCENT : TerrainRenderType.CUTOUT_MIPPED;
            }
        }

        return renderType;
    }

    private <E extends BlockEntity> void handleBlockEntity(CompileResult compileResult, E blockEntity) {
        BlockEntityRenderer<E> blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
        if (blockEntityRenderer != null) {
            compileResult.blockEntities.add(blockEntity);
            if (blockEntityRenderer.shouldRenderOffScreen(blockEntity)) {
                compileResult.globalBlockEntities.add(blockEntity);
            }
        }

    }
}
