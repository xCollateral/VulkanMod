package net.vulkanmod.render.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
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

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkTask {
    private static TaskDispatcher taskDispatcher;

    protected AtomicBoolean cancelled = new AtomicBoolean(false);
    protected final RenderSection renderSection;
    public boolean highPriority = false;

    ChunkTask(RenderSection renderSection) {
        this.renderSection = renderSection;
    }

    public String name() {
        return "generic_chk_task";
    }

    public CompletableFuture<Result> doTask(ChunkBufferBuilderPack builderPack) {
        return null;
    }

    public Result doTaskD(ChunkBufferBuilderPack builderPack) {
        return null;
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    public static void setTaskDispatcher(TaskDispatcher dispatcher) {
        taskDispatcher = dispatcher;
    }

    public static class BuildTask extends ChunkTask {
        @Nullable
        protected RenderChunkRegion region;
//        private CompileResults compileResults;

        //debug
        private boolean submitted = false;

        private Reference2ObjectArrayMap<RenderType, BufferBuilder.RenderedBuffer> buffers = new Reference2ObjectArrayMap<>();

        public BuildTask(RenderSection renderSection, RenderChunkRegion renderChunkRegion, boolean highPriority) {
            super(renderSection);
            this.region = renderChunkRegion;
            this.highPriority = highPriority;
        }

        public String name() {
            return "rend_chk_rebuild";
        }

        public CompletableFuture<Result> doTask(ChunkBufferBuilderPack builderPack) {
//            if (this.cancelled) {
//                return CompletableFuture.completedFuture(Result.CANCELLED);
//            } else if (!this.renderSection.hasXYNeighbours()) {
//                this.region = null;
//                this.renderSection.setDirty(false);
//                this.cancelled = true;
//                return CompletableFuture.completedFuture(Result.CANCELLED);
//            }
//            else {
//
//                Vec3 vec3 = WorldRenderer.getCameraPos();
//                float f = (float)vec3.x;
//                float f1 = (float)vec3.y;
//                float f2 = (float)vec3.z;
//                RenderSection.CompiledSection compiledSection = new RenderSection.CompiledSection();
//                Set<BlockEntity> set = this.compile(f, f1, f2, compiledSection, builderPack);
//
//                this.renderSection.updateGlobalBlockEntities(set);
//                if (this.cancelled) {
//                    this.buffers.values().forEach(BufferBuilder.RenderedBuffer::release);
//                    return CompletableFuture.completedFuture(Result.CANCELLED);
//                } else {
//                    List<CompletableFuture<Void>> list = Lists.newArrayList();
//                    this.buffers.forEach((renderType, renderedBuffer) -> {
//                        list.add(taskDispatcher.scheduleUploadChunkLayer(renderedBuffer, this.renderSection.getBuffer(renderType)));
//                    });
//                    return Util.sequenceFailFast(list).handle((voidList, throwable) -> {
//                        if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException)) {
//                            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering chunk");
//                            Minecraft.getInstance().delayCrash(crashreport);
//                        }
//
//                        if (this.cancelled) {
//                            return Result.CANCELLED;
//                        } else {
//                            this.renderSection.setCompiled(compiledSection);
//                            this.renderSection.initialCompilationCancelCount.set(0);
////                            ChunkRenderDispatcher.this.renderer.addRecentlycompiledSection(ChunkRenderDispatcher.RenderChunk.this);
//                            return Result.SUCCESSFUL;
//                        }
//                    });
//                }
//            }

            return this.doTaskV2(builderPack);
        }

        public CompletableFuture<Result> doTaskV2(ChunkBufferBuilderPack chunkBufferBuilderPack) {
            //debug
            this.submitted = true;

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
                    compileResults.renderedLayers.values().forEach(BufferBuilder.RenderedBuffer::release);
                    return CompletableFuture.completedFuture(Result.CANCELLED);
                } else {
                    RenderSection.CompiledSection compiledChunk = new RenderSection.CompiledSection();
                    compiledChunk.visibilitySet = compileResults.visibilitySet;
                    compiledChunk.renderableBlockEntities.addAll(compileResults.blockEntities);
                    compiledChunk.transparencyState = compileResults.transparencyState;
                    List<CompletableFuture<Void>> list = Lists.newArrayList();
                    if(!compileResults.renderedLayers.isEmpty()) compiledChunk.isCompletelyEmpty = false;
                    compileResults.renderedLayers.forEach((renderType, renderedBuffer) -> {
                        list.add(taskDispatcher.scheduleUploadChunkLayer(renderedBuffer, this.renderSection.getBuffer(renderType)));
                        compiledChunk.renderTypes.add(renderType);
                    });
                    return Util.sequenceFailFast(list).handle((listx, throwable) -> {
                        if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException)) {
                            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwable, "Rendering chunk"));
                        }

                        if (this.cancelled.get()) {
                            return Result.CANCELLED;
                        } else {
                            this.renderSection.compiledSection = (compiledChunk);
                            this.renderSection.initialCompilationCancelCount.set(0);
//                            ChunkRenderDispatcher.this.renderer.addRecentlyCompiledChunk(ChunkRenderDispatcher.RenderChunk.this);
                            return Result.SUCCESSFUL;
                        }
                    });
                }
            }
        }

        private Set<BlockEntity> compile(float x, float y, float z, RenderSection.CompiledSection compiledSection, ChunkBufferBuilderPack builderPack) {
            int i = 1;
            BlockPos blockpos = this.renderSection.origin.immutable();
            BlockPos blockpos1 = blockpos.offset(15, 15, 15);
            VisGraph visgraph = new VisGraph();
            Set<BlockEntity> set = Sets.newHashSet();
            RenderChunkRegion renderchunkregion = this.region;
//            this.region = null;
            PoseStack posestack = new PoseStack();
            if (renderchunkregion != null) {
                ModelBlockRenderer.enableCaching();
                RandomSource random = RandomSource.create();
                BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();

                for(BlockPos blockpos2 : BlockPos.betweenClosed(blockpos, blockpos1)) {
                    BlockState blockstate = renderchunkregion.getBlockState(blockpos2);
                    if (blockstate.isSolidRender(renderchunkregion, blockpos2)) {
                        visgraph.setOpaque(blockpos2);
                    }

                    if (blockstate.hasBlockEntity()) {
                        BlockEntity blockentity = renderchunkregion.getBlockEntity(blockpos2);
                        if (blockentity != null) {
                            this.handleBlockEntity(compiledSection, set, blockentity);
                        }
                    }

                    BlockState blockstate1 = renderchunkregion.getBlockState(blockpos2);
                    FluidState fluidstate = blockstate1.getFluidState();
                    if (!fluidstate.isEmpty()) {
                        RenderType rendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);
                        BufferBuilder bufferbuilder = builderPack.builder(rendertype);
                        if (compiledSection.renderTypes.add(rendertype)) {
                            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        }

                        blockrenderdispatcher.renderLiquid(blockpos2, renderchunkregion, bufferbuilder, blockstate1, fluidstate);
                    }

                    if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                        RenderType rendertype1 = ItemBlockRenderTypes.getChunkRenderType(blockstate);
                        BufferBuilder bufferbuilder2 = builderPack.builder(rendertype1);
                        if (compiledSection.renderTypes.add(rendertype1)) {
                            bufferbuilder2.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        }

                        posestack.pushPose();
                        posestack.translate((double)(blockpos2.getX() & 15), (double)(blockpos2.getY() & 15), (double)(blockpos2.getZ() & 15));
                        blockrenderdispatcher.renderBatched(blockstate, blockpos2, renderchunkregion, posestack, bufferbuilder2, true, random);

                        posestack.popPose();
                    }
                }

                if (compiledSection.renderTypes.contains(RenderType.translucent())) {
                    BufferBuilder bufferbuilder1 = builderPack.builder(RenderType.translucent());
                    bufferbuilder1.setQuadSortOrigin(x - (float)blockpos.getX(), y - (float)blockpos.getY(), z - (float)blockpos.getZ());
                    compiledSection.transparencyState = bufferbuilder1.getSortState();
                }

//                compiledSection.renderTypes.stream().map(builderPack::builder).forEach(BufferBuilder::end);
                compiledSection.renderTypes.stream().forEach(renderType -> {
                    BufferBuilder.RenderedBuffer renderedBuffer = builderPack.builder(renderType).endOrDiscardIfEmpty();
                    if (renderedBuffer != null) {
                        buffers.put(renderType, renderedBuffer);
                    }
                });
                ModelBlockRenderer.clearCache();
            }

            compiledSection.visibilitySet = visgraph.resolve();
            return set;
        }

        private CompileResults compile(float f, float g, float h, ChunkBufferBuilderPack chunkBufferBuilderPack) {
            CompileResults compileResults = new CompileResults();

            BlockPos blockPos = this.renderSection.origin.immutable();
            BlockPos blockPos2 = blockPos.offset(15, 15, 15);
            VisGraph visGraph = new VisGraph();
            RenderChunkRegion renderChunkRegion = this.region;
            this.region = null;
            PoseStack poseStack = new PoseStack();
            if (renderChunkRegion != null) {
                ModelBlockRenderer.enableCaching();
                Set<RenderType> set = new ReferenceArraySet(RenderType.chunkBufferLayers().size());
                RandomSource randomSource = RandomSource.create();
                BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
                Iterator var15 = BlockPos.betweenClosed(blockPos, blockPos2).iterator();

                while(var15.hasNext()) {
                    BlockPos blockPos3 = (BlockPos)var15.next();
                    BlockState blockState = renderChunkRegion.getBlockState(blockPos3);
                    if (blockState.isSolidRender(renderChunkRegion, blockPos3)) {
                        visGraph.setOpaque(blockPos3);
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity blockEntity = renderChunkRegion.getBlockEntity(blockPos3);
                        if (blockEntity != null) {
                            this.handleBlockEntity(compileResults, blockEntity);
                        }
                    }

                    BlockState blockState2 = renderChunkRegion.getBlockState(blockPos3);
                    FluidState fluidState = blockState2.getFluidState();
                    RenderType renderType;
                    BufferBuilder bufferBuilder;
                    if (!fluidState.isEmpty()) {
                        renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
                        bufferBuilder = chunkBufferBuilderPack.builder(renderType);
                        if (set.add(renderType)) {
                            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        }

                        blockRenderDispatcher.renderLiquid(blockPos3, renderChunkRegion, bufferBuilder, blockState2, fluidState);
                    }

                    if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                        renderType = ItemBlockRenderTypes.getChunkRenderType(blockState);
                        bufferBuilder = chunkBufferBuilderPack.builder(renderType);
                        if (set.add(renderType)) {
                            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        }

                        poseStack.pushPose();
                        poseStack.translate((double)(blockPos3.getX() & 15), (double)(blockPos3.getY() & 15), (double)(blockPos3.getZ() & 15));
                        blockRenderDispatcher.renderBatched(blockState, blockPos3, renderChunkRegion, poseStack, bufferBuilder, true, randomSource);
                        poseStack.popPose();
                    }
                }

                if (set.contains(RenderType.translucent())) {
                    BufferBuilder bufferBuilder2 = chunkBufferBuilderPack.builder(RenderType.translucent());
                    if (!bufferBuilder2.isCurrentBatchEmpty()) {
                        bufferBuilder2.setQuadSortOrigin(f - (float)blockPos.getX(), g - (float)blockPos.getY(), h - (float)blockPos.getZ());
                        compileResults.transparencyState = bufferBuilder2.getSortState();
                    }
                }

                var15 = set.iterator();

                while(var15.hasNext()) {
                    RenderType renderType2 = (RenderType)var15.next();
                    BufferBuilder.RenderedBuffer renderedBuffer = chunkBufferBuilderPack.builder(renderType2).endOrDiscardIfEmpty();
                    if (renderedBuffer != null) {
                        compileResults.renderedLayers.put(renderType2, renderedBuffer);
                    }
                }

                ModelBlockRenderer.clearCache();
            }

            compileResults.visibilitySet = visGraph.resolve();
            return compileResults;
        }

        private <E extends BlockEntity> void handleBlockEntity(RenderSection.CompiledSection compiledSection, Set<BlockEntity> set, E entity) {
            BlockEntityRenderer<E> blockentityrenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
            if (blockentityrenderer != null) {
                compiledSection.renderableBlockEntities.add(entity);
                if (blockentityrenderer.shouldRenderOffScreen(entity)) {
                    set.add(entity);
                }
            }

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
            public final List<BlockEntity> globalBlockEntities = new ArrayList();
            public final List<BlockEntity> blockEntities = new ArrayList();
            public final Map<RenderType, BufferBuilder.RenderedBuffer> renderedLayers = new Reference2ObjectArrayMap();
            public VisibilitySet visibilitySet = new VisibilitySet();
            @org.jetbrains.annotations.Nullable
            public BufferBuilder.SortState transparencyState;

            CompileResults() {
            }
        }
    }

    public static class SortTransparencyTask extends ChunkTask {
        RenderSection.CompiledSection compiledSection;

        SortTransparencyTask(RenderSection renderSection) {
            super(renderSection);
            this.compiledSection = renderSection.getCompiledSection();
        }

        public String name() {
            return "rend_chk_sort";
        }

        public CompletableFuture<Result> doTask(ChunkBufferBuilderPack builderPack) {
            if (this.cancelled.get()) {
                return CompletableFuture.completedFuture(Result.CANCELLED);
            } else if (renderSection.hasXYNeighbours()) {
                this.cancelled.set(true);
                return CompletableFuture.completedFuture(Result.CANCELLED);
            } else {
                Vec3 vec3 = WorldRenderer.getCameraPos();
                float f = (float)vec3.x;
                float f1 = (float)vec3.y;
                float f2 = (float)vec3.z;
                BufferBuilder.SortState bufferbuilder$sortstate = this.compiledSection.transparencyState;
                if (bufferbuilder$sortstate != null && this.compiledSection.renderTypes.contains(RenderType.translucent())) {
                    BufferBuilder bufferbuilder = builderPack.builder(RenderType.translucent());
                    bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                    bufferbuilder.restoreSortState(bufferbuilder$sortstate);
                    bufferbuilder.setQuadSortOrigin(f - (float) this.renderSection.origin.getX(), f1 - (float) renderSection.origin.getY(), f2 - (float) renderSection.origin.getZ());
                    this.compiledSection.transparencyState = bufferbuilder.getSortState();
                    BufferBuilder.RenderedBuffer renderedBuffer = bufferbuilder.end();
                    if (this.cancelled.get()) {
                        return CompletableFuture.completedFuture(Result.CANCELLED);
                    } else {
                        CompletableFuture<Result> completablefuture = taskDispatcher.scheduleUploadChunkLayer(renderedBuffer, renderSection.getBuffer(RenderType.translucent())).thenApply((p_112898_) -> {
                            return Result.CANCELLED;
                        });
                        return completablefuture.handle((p_199960_, p_199961_) -> {
                            if (p_199961_ != null && !(p_199961_ instanceof CancellationException) && !(p_199961_ instanceof InterruptedException)) {
                                CrashReport crashreport = CrashReport.forThrowable(p_199961_, "Rendering chunk");
                                Minecraft.getInstance().delayCrash(crashreport);
                            }

                            return this.cancelled.get() ? Result.CANCELLED : Result.SUCCESSFUL;
                        });
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
