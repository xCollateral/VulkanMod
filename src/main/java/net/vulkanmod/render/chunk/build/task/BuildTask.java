package net.vulkanmod.render.chunk.build.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.LiquidRenderer;
import net.vulkanmod.render.chunk.build.RenderRegion;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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

    public Result runTask(BuilderResources builderResources) {
        long startTime = System.nanoTime();

        if (this.cancelled.get()) {
            return Result.CANCELLED;
        }

        Vec3 vec3 = WorldRenderer.getCameraPos();
        float x = (float) vec3.x;
        float y = (float) vec3.y;
        float z = (float) vec3.z;
        CompileResult compileResult = this.compile(x, y, z, builderResources);

        CompiledSection compiledSection = new CompiledSection();
        compiledSection.blockEntities.addAll(compileResult.blockEntities);
        compiledSection.transparencyState = compileResult.transparencyState;
        compiledSection.isCompletelyEmpty = compileResult.renderedLayers.isEmpty();
        compileResult.compiledSection = compiledSection;

        if (this.cancelled.get()) {
            compileResult.renderedLayers.values().forEach(UploadBuffer::release);
            return Result.CANCELLED;
        }

        taskDispatcher.scheduleSectionUpdate(compileResult);

        float buildTime = (System.nanoTime() - startTime) * 0.000001f;
        if (BENCH) {
            builderResources.updateBuildStats((int) buildTime);
        }

        return Result.SUCCESSFUL;
    }

    private CompileResult compile(float camX, float camY, float camZ, BuilderResources builderResources) {
        CompileResult compileResult = new CompileResult(this.section, true);

        BlockPos startBlockPos = new BlockPos(section.xOffset(), section.yOffset(), section.zOffset()).immutable();
        VisGraph visGraph = new VisGraph();

        if (this.region == null) {
            compileResult.visibilitySet = visGraph.resolve();
            return compileResult;
        }

        Vector3f pos = new Vector3f();
        ThreadBuilderPack bufferBuilders = builderResources.builderPack;
        setupBufferBuilders(bufferBuilders);

        this.region.loadBlockStates();
        this.region.initTintCache(builderResources.tintCache);

        builderResources.update(this.region, this.section);

        BlockRenderer blockRenderer = builderResources.blockRenderer;

        LiquidRenderer liquidRenderer = builderResources.liquidRenderer;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        for (int y = 0; y < 16; ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    blockPos.set(section.xOffset() + x, section.yOffset() + y, section.zOffset() + z);

                    BlockState blockState = this.region.getBlockState(blockPos);
                    if (blockState.isSolidRender(this.region, blockPos)) {
                        visGraph.setOpaque(blockPos);
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity blockEntity = this.region.getBlockEntity(blockPos);
                        if (blockEntity != null) {
                            this.handleBlockEntity(compileResult, blockEntity);
                        }
                    }

                    FluidState fluidState = blockState.getFluidState();
                    TerrainRenderType renderType;
                    TerrainBufferBuilder bufferBuilder;
                    if (!fluidState.isEmpty()) {
                        renderType = TerrainRenderType.get(ItemBlockRenderTypes.getRenderLayer(fluidState));

                        bufferBuilder = getBufferBuilder(bufferBuilders, renderType);
                        bufferBuilder.setBlockAttributes(blockState);

                        liquidRenderer.renderLiquid(blockState, fluidState, blockPos, bufferBuilder);
                    }

                    if (blockState.getRenderShape() == RenderShape.MODEL) {
                        renderType = TerrainRenderType.get(ItemBlockRenderTypes.getChunkRenderType(blockState));

                        bufferBuilder = getBufferBuilder(bufferBuilders, renderType);
                        bufferBuilder.setBlockAttributes(blockState);

                        pos.set(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
                        blockRenderer.renderBlock(blockState, blockPos, pos, bufferBuilder);
                    }
                }
            }
        }

        TerrainBufferBuilder translucentBufferBuilder = bufferBuilders.builder(TerrainRenderType.TRANSLUCENT);
        if (!translucentBufferBuilder.isCurrentBatchEmpty()) {
            translucentBufferBuilder.setupQuadSortingPoints();
            translucentBufferBuilder.setupQuadSorting(camX - (float) startBlockPos.getX(), camY - (float) startBlockPos.getY(), camZ - (float) startBlockPos.getZ());
            compileResult.transparencyState = translucentBufferBuilder.getSortState();
        }

        for (TerrainRenderType renderType : TerrainRenderType.VALUES) {
            TerrainBufferBuilder.RenderedBuffer renderedBuffer = bufferBuilders.builder(renderType).end();
            if (renderedBuffer != null) {
                UploadBuffer uploadBuffer = new UploadBuffer(renderedBuffer);
                compileResult.renderedLayers.put(renderType, uploadBuffer);
                renderedBuffer.release();
            }
        }

        compileResult.visibilitySet = visGraph.resolve();
        this.region = null;
        return compileResult;
    }

    private void setupBufferBuilders(ThreadBuilderPack builderPack) {
        for (TerrainRenderType renderType : TerrainRenderType.VALUES) {
            TerrainBufferBuilder bufferBuilder = builderPack.builder(renderType);
            bufferBuilder.begin();
        }
    }

    private TerrainBufferBuilder getBufferBuilder(ThreadBuilderPack bufferBuilders, TerrainRenderType renderType) {
        renderType = compactRenderTypes(renderType);
        return bufferBuilders.builder(renderType);
    }

    private TerrainRenderType compactRenderTypes(TerrainRenderType renderType) {
        if (Initializer.CONFIG.uniqueOpaqueLayer) {
            renderType = switch (renderType) {
                case SOLID, CUTOUT, CUTOUT_MIPPED -> TerrainRenderType.CUTOUT_MIPPED;
                case TRANSLUCENT, TRIPWIRE -> TerrainRenderType.TRANSLUCENT;
            };
        } else {
            renderType = switch (renderType) {
                case SOLID, CUTOUT_MIPPED -> TerrainRenderType.CUTOUT_MIPPED;
                case CUTOUT -> TerrainRenderType.CUTOUT;
                case TRANSLUCENT, TRIPWIRE -> TerrainRenderType.TRANSLUCENT;
            };
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
