package net.vulkanmod.render.chunk.build;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.VisibilitySetExtended;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.TerrainShaderManager;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ChunkTask {
    private static TaskDispatcher taskDispatcher;

    public static final boolean bench = false;
    public static AtomicInteger totalBuildTime = new AtomicInteger(0);
    public static AtomicInteger buildCount = new AtomicInteger(0);

    public static BuildTask createBuildTask(RenderSection renderSection, RenderChunkRegion renderChunkRegion, boolean highPriority) {
        return new BuildTask(renderSection, renderChunkRegion, highPriority);
    }

    protected AtomicBoolean cancelled = new AtomicBoolean(false);
    protected final RenderSection renderSection;
    public boolean highPriority = false;

    ChunkTask(RenderSection renderSection) {
        this.renderSection = renderSection;
    }

    public abstract String name();

    public abstract CompletableFuture<Result> doTask(ThreadBuilderPack builderPack);

    public void cancel() {
        this.cancelled.set(true);
    }

    public static void setTaskDispatcher(TaskDispatcher dispatcher) {
        taskDispatcher = dispatcher;
    }

    public static class BuildTask extends ChunkTask {
        @Nullable
        protected RenderChunkRegion region;

        //debug
        private float buildTime;
        private boolean submitted = false;

        public BuildTask(RenderSection renderSection, RenderChunkRegion renderChunkRegion, boolean highPriority) {
            super(renderSection);
            this.region = renderChunkRegion;
            this.highPriority = highPriority;
        }

        public String name() {
            return "rend_chk_rebuild";
        }

        public CompletableFuture<Result> doTask(ThreadBuilderPack chunkBufferBuilderPack) {
            //debug
            this.submitted = true;
            long startTime = System.nanoTime();

            if (this.cancelled.get()) {
                return CompletableFuture.completedFuture(Result.CANCELLED);
            } else if (!this.renderSection.hasXYNeighbours()) {
                this.region = null;
                this.renderSection.setDirty(false);
                this.cancelled.set(true);
                return CompletableFuture.completedFuture(Result.CANCELLED);
            } else if (this.cancelled.get()) {
                return CompletableFuture.completedFuture(Result.CANCELLED);
            } else {
                Vec3 vec3 = WorldRenderer.getCameraPos();
                float f = (float)vec3.x;
                float g = (float)vec3.y;
                float h = (float)vec3.z;
                CompileResults compileResults = this.compile(f, g, h, chunkBufferBuilderPack);

                this.renderSection.updateGlobalBlockEntities(compileResults.globalBlockEntities);

                if (this.cancelled.get()) {
                    compileResults.renderedLayers.values().forEach(UploadBuffer::release);
                    return CompletableFuture.completedFuture(Result.CANCELLED);
                } else {
                    CompiledSection compiledChunk = new CompiledSection();
                    compiledChunk.visibilitySet = compileResults.visibilitySet;
                    compiledChunk.renderableBlockEntities.addAll(compileResults.blockEntities);
                    compiledChunk.transparencyState = compileResults.transparencyState;

                    if(!compileResults.renderedLayers.isEmpty())
                        compiledChunk.isCompletelyEmpty = false;

                    taskDispatcher.scheduleSectionUpdate(renderSection, compileResults.renderedLayers);
                    compiledChunk.renderTypes.addAll(compileResults.renderedLayers.keySet());

                    this.renderSection.setCompiledSection(compiledChunk);
                    this.renderSection.setVisibility(((VisibilitySetExtended)compiledChunk.visibilitySet).getVisibility());
                    this.renderSection.setCompletelyEmpty(compiledChunk.isCompletelyEmpty);

                    this.buildTime = (System.nanoTime() - startTime) * 0.000001f;

                    if(bench) {
                        totalBuildTime.addAndGet((int)buildTime);
                        buildCount.addAndGet(1);
                    }

                    return CompletableFuture.completedFuture(Result.SUCCESSFUL);
                }
            }
        }

        private CompileResults compile(float camX, float camY, float camZ, ThreadBuilderPack chunkBufferBuilderPack) {
            CompileResults compileResults = new CompileResults();

            BlockPos startBlockPos = new BlockPos(renderSection.xOffset(), renderSection.yOffset(), renderSection.zOffset()).immutable();

            BlockPos endBlockPos = startBlockPos.offset(15, 15, 15);
            VisGraph visGraph = new VisGraph();
            RenderChunkRegion renderChunkRegion = this.region;
            this.region = null;
            PoseStack poseStack = new PoseStack();
            if (renderChunkRegion != null) {
                ModelBlockRenderer.enableCaching();
                Set<RenderType> set = new ReferenceArraySet<>(RenderType.chunkBufferLayers().size());
                RandomSource randomSource = RandomSource.create();
                BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();

                for(BlockPos blockPos : BlockPos.betweenClosed(startBlockPos, endBlockPos)) {
                    BlockState blockState = renderChunkRegion.getBlockState(blockPos);
                    if (blockState.isSolidRender(renderChunkRegion, blockPos)) {
                        visGraph.setOpaque(blockPos);
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity blockEntity = renderChunkRegion.getBlockEntity(blockPos);
                        if (blockEntity != null) {
                            this.handleBlockEntity(compileResults, blockEntity);
                        }
                    }

//                    BlockState blockState2 = renderChunkRegion.getBlockState(blockPos);
                    FluidState fluidState = blockState.getFluidState();
                    RenderType renderType;
                    TerrainBufferBuilder bufferBuilder;
                    if (!fluidState.isEmpty()) {
                        renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);

                        //Force compact RenderType
                        renderType = compactRenderTypes(renderType);

                        bufferBuilder = chunkBufferBuilderPack.builder(renderType);
                        if (set.add(renderType)) {
                            bufferBuilder.begin(VertexFormat.Mode.QUADS, TerrainShaderManager.TERRAIN_VERTEX_FORMAT);
                        }

                        bufferBuilder.setBlockAttributes(fluidState.createLegacyBlock());

                        blockRenderDispatcher.renderLiquid(blockPos, renderChunkRegion, bufferBuilder, blockState, fluidState);
                    }

                    if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                        renderType = ItemBlockRenderTypes.getChunkRenderType(blockState);

                        //Force compact RenderType
                        renderType = compactRenderTypes(renderType);

                        bufferBuilder = chunkBufferBuilderPack.builder(renderType);
                        if (set.add(renderType)) {
                            bufferBuilder.begin(VertexFormat.Mode.QUADS, TerrainShaderManager.TERRAIN_VERTEX_FORMAT);
                        }

                        bufferBuilder.setBlockAttributes(blockState);

                        poseStack.pushPose();
                        poseStack.translate(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
                        blockRenderDispatcher.renderBatched(blockState, blockPos, renderChunkRegion, poseStack, bufferBuilder, true, randomSource);
                        poseStack.popPose();
                    }
                }

                if (set.contains(RenderType.translucent())) {
                    TerrainBufferBuilder bufferBuilder2 = chunkBufferBuilderPack.builder(RenderType.translucent());
                    if (!bufferBuilder2.isCurrentBatchEmpty()) {
                        bufferBuilder2.setQuadSortOrigin(camX - (float)startBlockPos.getX(), camY - (float)startBlockPos.getY(), camZ - (float)startBlockPos.getZ());
                        compileResults.transparencyState = bufferBuilder2.getSortState();
                    }
                }

                for(RenderType renderType2 : set) {
                    TerrainBufferBuilder.RenderedBuffer renderedBuffer = chunkBufferBuilderPack.builder(renderType2).endOrDiscardIfEmpty();
                    if (renderedBuffer != null) {
                        UploadBuffer uploadBuffer = new UploadBuffer(renderedBuffer);
                        compileResults.renderedLayers.put(TerrainRenderType.get(renderType2.name), uploadBuffer);
                    }

                    if(renderedBuffer != null)
                        renderedBuffer.release();
                }

                ModelBlockRenderer.clearCache();
            }

            compileResults.visibilitySet = visGraph.resolve();
            return compileResults;
        }

        private RenderType compactRenderTypes(RenderType renderType) {

            if(Initializer.CONFIG.uniqueOpaqueLayer) {
                if (renderType != RenderType.translucent()) {
                    renderType = renderType == RenderType.tripwire() ? RenderType.translucent() : RenderType.cutoutMipped();
                }
            }
            else {
                if (renderType != RenderType.translucent() && renderType != RenderType.cutout()) {
                    renderType = renderType == RenderType.tripwire() ? RenderType.translucent() : RenderType.cutoutMipped();
                }
            }

            return renderType;
        }

        private <E extends BlockEntity> void handleBlockEntity(CompileResults compileResults, E blockEntity) {
            BlockEntityRenderer<E> blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (blockEntityRenderer != null) {
                compileResults.blockEntities.add(blockEntity);
                if (blockEntityRenderer.shouldRenderOffScreen(blockEntity)) {
                    compileResults.globalBlockEntities.add(blockEntity);
                }
            }

        }

        private static final class CompileResults {
            public final List<BlockEntity> globalBlockEntities = new ArrayList<>();
            public final List<BlockEntity> blockEntities = new ArrayList<>();
            public final EnumMap<TerrainRenderType, UploadBuffer> renderedLayers = new EnumMap<>(TerrainRenderType.class);
            public VisibilitySet visibilitySet = new VisibilitySet();
            @org.jetbrains.annotations.Nullable
            public TerrainBufferBuilder.SortState transparencyState;

            CompileResults() {
            }
        }
    }

    public static class SortTransparencyTask extends ChunkTask {
        CompiledSection compiledSection;

        public SortTransparencyTask(RenderSection renderSection) {
            super(renderSection);
            this.compiledSection = renderSection.getCompiledSection();
        }

        public String name() {
            return "rend_chk_sort";
        }

        public CompletableFuture<Result> doTask(ThreadBuilderPack builderPack) {
            if (this.cancelled.get()) {
                return CompletableFuture.completedFuture(Result.CANCELLED);
            } else if (!renderSection.hasXYNeighbours()) {
                this.cancelled.set(true);
                return CompletableFuture.completedFuture(Result.CANCELLED);
            } else {
                Vec3 vec3 = WorldRenderer.getCameraPos();
                float f = (float)vec3.x;
                float f1 = (float)vec3.y;
                float f2 = (float)vec3.z;
                TerrainBufferBuilder.SortState transparencyState = this.compiledSection.transparencyState;
                if (transparencyState != null && this.compiledSection.renderTypes.contains(TerrainRenderType.TRANSLUCENT)) {
                    TerrainBufferBuilder bufferbuilder = builderPack.builder(RenderType.translucent());
                    bufferbuilder.begin(VertexFormat.Mode.QUADS, TerrainShaderManager.TERRAIN_VERTEX_FORMAT);
                    bufferbuilder.restoreSortState(transparencyState);
//                    bufferbuilder.setQuadSortOrigin(f - (float) this.renderSection.origin.getX(), f1 - (float) renderSection.origin.getY(), f2 - (float) renderSection.origin.getZ());
                    bufferbuilder.setQuadSortOrigin(f - (float) this.renderSection.xOffset(), f1 - (float) renderSection.yOffset(), f2 - (float) renderSection.zOffset());
                    this.compiledSection.transparencyState = bufferbuilder.getSortState();
                    TerrainBufferBuilder.RenderedBuffer renderedBuffer = bufferbuilder.end();
                    if (this.cancelled.get()) {
                        return CompletableFuture.completedFuture(Result.CANCELLED);
                    } else {

                        UploadBuffer uploadBuffer = new UploadBuffer(renderedBuffer);
                        taskDispatcher.scheduleUploadChunkLayer(renderSection, TerrainRenderType.TRANSLUCENT, uploadBuffer);
                        renderedBuffer.release();
                        return CompletableFuture.completedFuture(Result.SUCCESSFUL);

                    }
                } else {
                    return CompletableFuture.completedFuture(Result.CANCELLED);
                }
            }
        }
    }
    
    public enum Result {
        CANCELLED,
        SUCCESSFUL;
    }
}
