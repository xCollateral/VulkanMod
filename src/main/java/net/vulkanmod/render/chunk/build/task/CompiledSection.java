package net.vulkanmod.render.chunk.build.task;

import com.google.common.collect.Lists;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CompiledSection {
    public static final CompiledSection UNCOMPILED = new CompiledSection();

    public final Set<TerrainRenderType> renderTypes = EnumSet.noneOf(TerrainRenderType.class);
    boolean isCompletelyEmpty = false;
    final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();

    @Nullable
    TerrainBufferBuilder.SortState transparencyState;

    public boolean hasNoRenderableLayers() {
        return this.isCompletelyEmpty;
    }

    public boolean isEmpty(TerrainRenderType p_112759_) {
        return !this.renderTypes.contains(p_112759_);
    }

    public List<BlockEntity> getRenderableBlockEntities() {
        return this.renderableBlockEntities;
    }
}


