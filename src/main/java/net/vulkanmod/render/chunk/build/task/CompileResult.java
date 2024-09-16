package net.vulkanmod.render.chunk.build.task;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.interfaces.VisibilitySetExtended;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.vertex.QuadSorter;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class CompileResult {
    public final RenderSection renderSection;
    public final boolean fullUpdate;

    final List<BlockEntity> globalBlockEntities = new ArrayList<>();
    final List<BlockEntity> blockEntities = new ArrayList<>();
    public final EnumMap<TerrainRenderType, UploadBuffer> renderedLayers = new EnumMap<>(TerrainRenderType.class);

    VisibilitySet visibilitySet;
    QuadSorter.SortState transparencyState;
    CompiledSection compiledSection;

    CompileResult(RenderSection renderSection, boolean fullUpdate) {
        this.renderSection = renderSection;
        this.fullUpdate = fullUpdate;
    }

    public void updateSection() {
        this.renderSection.updateGlobalBlockEntities(globalBlockEntities);
        this.renderSection.setCompiledSection(compiledSection);
        this.renderSection.setVisibility(((VisibilitySetExtended)visibilitySet).getVisibility());
        this.renderSection.setCompletelyEmpty(compiledSection.isCompletelyEmpty);
        this.renderSection.setContainsBlockEntities(!blockEntities.isEmpty());
    }
}
