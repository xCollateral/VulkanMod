package net.vulkanmod.render.chunk.build.task;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.concurrent.CompletableFuture;

public class SortTransparencyTask extends ChunkTask {

    public SortTransparencyTask(RenderSection renderSection) {
        super(renderSection);
    }

    public String name() {
        return "rend_chk_sort";
    }

    public CompletableFuture<Result> doTask(BuilderResources context) {
        ThreadBuilderPack builderPack = context.builderPack;

        if (this.cancelled.get()) {
            return CompletableFuture.completedFuture(Result.CANCELLED);
        }

        Vec3 vec3 = WorldRenderer.getCameraPos();
        float x = (float)vec3.x;
        float y = (float)vec3.y;
        float z = (float)vec3.z;

        CompiledSection compiledSection = renderSection.getCompiledSection();
        TerrainBufferBuilder.SortState transparencyState = compiledSection.transparencyState;
        TerrainBufferBuilder bufferBuilder = builderPack.builder(TerrainRenderType.TRANSLUCENT);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, PipelineManager.TERRAIN_VERTEX_FORMAT);
        bufferBuilder.restoreSortState(transparencyState);

        bufferBuilder.setQuadSortOrigin(x - (float) this.renderSection.xOffset(), y - (float) renderSection.yOffset(), z - (float) renderSection.zOffset());
        compiledSection.transparencyState = bufferBuilder.getSortState();
        TerrainBufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();

        if (this.cancelled.get()) {
            return CompletableFuture.completedFuture(Result.CANCELLED);
        } else {
            UploadBuffer uploadBuffer = new UploadBuffer(renderedBuffer);
            taskDispatcher.scheduleUploadChunkLayer(renderSection, TerrainRenderType.get(RenderType.translucent()), uploadBuffer);
            renderedBuffer.release();
            return CompletableFuture.completedFuture(Result.SUCCESSFUL);
        }
    }
}
