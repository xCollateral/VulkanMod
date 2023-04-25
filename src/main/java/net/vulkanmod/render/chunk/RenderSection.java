package net.vulkanmod.render.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.chunk.util.VBOUtil;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RenderSection {
    private final int index;

    private final RenderSection[] neighbours = new RenderSection[6];
    private ChunkAreaManager.Tree chunkAreaTree;
    private ChunkArea chunkArea;
    private int lastFrame = -1;

    public CompiledSection compiledSection = CompiledSection.UNCOMPILED;

    final AtomicInteger initialCompilationCancelCount = new AtomicInteger(0);
    @Nullable
    private ChunkTask.BuildTask lastRebuildTask;
    @Nullable
    private ChunkTask.SortTransparencyTask lastResortTransparencyTask;
    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
    private final Map<VBOUtil.RenderTypes, VBO> buffers=new EnumMap<>(VBOUtil.RenderTypes.class);
    private AABB bb;
    private boolean dirty = true;
    private boolean lightReady = false;
    private boolean init = true;

    final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
    private final BlockPos.MutableBlockPos[] relativeOrigins = Util.make(new BlockPos.MutableBlockPos[6], (p_112831_) -> {
        for(int i = 0; i < p_112831_.length; ++i) {
            p_112831_[i] = new BlockPos.MutableBlockPos();
        }

    });
    private boolean playerChanged;

    public WorldRenderer.QueueChunkInfo queueInfo;

    public RenderSection(int index, int x, int y, int z) {
        this.index = index;
        this.origin.set(x, y, z);

        for (VBOUtil.RenderTypes renderType : VBOUtil.RenderTypes.values()) {
            buffers.put(renderType, new VBO(this.index, renderType, x, y, z));
        }

        //TODO later: find something better

    }

    public void setOrigin(int x, int y, int z) {
        this.reset();
        this.origin.set(x, y, z);
        this.bb = new AABB(x, y, z, x + 16, y + 16, z + 16);
        for(VBO vbo : buffers.values())
        {
            vbo.updateOrigin(x, y, z);
        }
        for(Direction direction : Direction.values()) {
            this.relativeOrigins[direction.ordinal()].set(this.origin).move(direction, 16);
        }

        if(this.init) {
            this.lightReady = WorldRenderer.getLevel().getChunk(x >> 4, z >> 4).isClientLightReady();
            this.init = false;
        }

    }

    public boolean resortTransparency(TaskDispatcher taskDispatcher) {
        CompiledSection compiledSection1 = this.getCompiledSection();
        if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
        }

        if (!compiledSection1.renderTypes.contains(VBOUtil.RenderTypes.TRANSLUCENT)) {
            return false;
        } else {
            this.lastResortTransparencyTask = new ChunkTask.SortTransparencyTask(this);
            taskDispatcher.schedule(this.lastResortTransparencyTask);
            return true;
        }
    }

    public void rebuildChunkAsync(TaskDispatcher dispatcher, RenderRegionCache renderRegionCache) {
        ChunkTask.BuildTask chunkCompileTask = this.createCompileTask(renderRegionCache);
        dispatcher.schedule(chunkCompileTask);
    }

    public void rebuildChunkSync(TaskDispatcher dispatcher, RenderRegionCache renderRegionCache) {
        ChunkTask.BuildTask chunkCompileTask = this.createCompileTask(renderRegionCache);
        chunkCompileTask.doTask(dispatcher.fixedBuffers);
    }

    public ChunkTask.BuildTask createCompileTask(RenderRegionCache renderRegionCache) {
        boolean flag = this.cancelTasks();
        BlockPos blockpos = this.origin.immutable();
//         int i = 1;
        RenderChunkRegion renderchunkregion = renderRegionCache.createRegion(WorldRenderer.getLevel(), blockpos.offset(-1, -1, -1), blockpos.offset(16, 16, 16), 1);
        boolean flag1 = this.compiledSection == CompiledSection.UNCOMPILED;
        if (flag1 && flag) {
            this.initialCompilationCancelCount.incrementAndGet();
        }

        //debug
//        int xSec = this.origin.getX() >> 4;
//        int zSec = this.origin.getZ() >> 4;
//        if(renderchunkregion != null &&
//                (xSec < renderchunkregion.centerX - 1 || xSec > renderchunkregion.centerX + 1 || zSec < renderchunkregion.centerZ - 1 || zSec > renderchunkregion.centerZ + 1))
//        {
//            System.nanoTime();
//        }

        this.lastRebuildTask = new ChunkTask.BuildTask(this, renderchunkregion, !flag1 || this.initialCompilationCancelCount.get() > 2);
//        this.lastRebuildTask.debugCompile();
        return this.lastRebuildTask;
    }

    protected boolean cancelTasks() {
        boolean flag = false;
        if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
            flag = true;
        }

        if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
        }

        return flag;
    }

    public void setNotDirty() {
        this.dirty = false;
        this.playerChanged = false;
    }

    public boolean isDirty() { return this.dirty; }

    public boolean isDirtyFromPlayer() {
        return this.dirty && this.playerChanged;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public VBO getBuffer(VBOUtil.RenderTypes renderType) {
        return buffers.get(renderType);
    }

    public void setNeighbour(int index, @Nullable RenderSection chunk) {
        this.neighbours[index] = chunk;
    }

    public RenderSection getNeighbour(Direction dir) {
        return this.neighbours[dir.ordinal()];
    }

    public RenderSection getNeighbour(int i) {
        return this.neighbours[i];
    }

    public void setChunkArea(ChunkArea chunkArea) {
        this.chunkArea = chunkArea;
    }

    public ChunkArea getChunkArea() {
        return this.chunkArea;
    }

    public CompiledSection getCompiledSection() {
        return compiledSection;
    }

    //debug
    public AABB getAabb() {
        return this.bb;
    }

    public boolean hasXYNeighbours() {
        return true; //checks if chunks exist in level
    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> toAdd) {
        Set<BlockEntity> set = Sets.newHashSet(toAdd);
        Set<BlockEntity> set1;
        synchronized(this.globalBlockEntities) {
            set1 = Sets.newHashSet(this.globalBlockEntities);
            set.removeAll(this.globalBlockEntities);
            set1.removeAll(toAdd);
            this.globalBlockEntities.clear();
            this.globalBlockEntities.addAll(toAdd);
        }

        Minecraft.getInstance().levelRenderer.updateGlobalBlockEntities(set1, set);
    }

    private void reset() {
        this.cancelTasks();
        this.compiledSection = CompiledSection.UNCOMPILED;
        this.dirty = true;

    }

    public void setDirty(boolean playerChanged) {
        this.playerChanged = playerChanged || this.dirty && this.playerChanged;
        this.dirty = true;
        WorldRenderer.getInstance().setNeedsUpdate();
    }

    public void setLightReady(boolean b) {
        this.lightReady = b;
    }

    public boolean isLightReady() {
        return this.lightReady;
    }

    public void setCompiledSection(CompiledSection compiledSection) {
        this.compiledSection = compiledSection;
    }

    public boolean setLastFrame(int i) {
        boolean res = i == this.lastFrame ;
        if(!res) this.lastFrame = i;
        return res;
    }

    public void releaseBuffers() {
        this.reset();
        this.buffers.values().forEach(VBO::close);
    }

    public static class CompiledSection {
        public static final CompiledSection UNCOMPILED = new CompiledSection() {
            public boolean canSeeThrough(Direction dir1, Direction dir2) {
                return false;
            }
        };
        final Set<VBOUtil.RenderTypes> renderTypes = EnumSet.noneOf(VBOUtil.RenderTypes.class);
        boolean isCompletelyEmpty = true;
        final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
        VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        BufferBuilder.SortState transparencyState;

        public boolean hasNoRenderableLayers() {
            return this.isCompletelyEmpty;
        }

        public boolean isEmpty(VBOUtil.RenderTypes p_112759_) {
            return !this.renderTypes.contains(p_112759_);
        }

        public List<BlockEntity> getRenderableBlockEntities() {
            return this.renderableBlockEntities;
        }

        public boolean canSeeThrough(Direction dir1, Direction dir2) {
            return this.visibilitySet.visibilityBetween(dir1, dir2);
        }
    }

    static class Status {
        static final Status UNCOMPILED = new Status();
    }
}
