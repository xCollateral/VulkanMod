package net.vulkanmod.render.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.vulkanmod.render.VBO;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RenderSection {
    final int index;

    private final RenderSection[] neighbours = new RenderSection[6];
    public final VBO vbo;
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
    /*private final Map<RenderType, VertexBuffer> buffers =
            //TODO later: find something better
            new Reference2ReferenceArrayMap<>(RenderType.chunkBufferLayers().stream().collect(Collectors.toMap((renderType) -> {
        return renderType;
    }, (renderType) -> {
        return new VertexBuffer();
    })));*/
    private AABB bb;
    private boolean dirty = true;
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
        vbo = new VBO(index, x, y, z);
    }

    public void setOrigin(int x, int y, int z) {
        this.reset();

        this.origin.set(x, y, z);
        this.bb = new AABB(x, y, z, x + 16, y + 16, z + 16);
        vbo.updateOrigin(x,y,z);
        this.relativeOrigins[Direction.DOWN.ordinal()].set(this.origin).move(Direction.DOWN, 16);
        this.relativeOrigins[Direction.UP.ordinal()].set(this.origin).move(Direction.UP, 16);
        this.relativeOrigins[Direction.NORTH.ordinal()].set(this.origin).move(Direction.NORTH, 16);
        this.relativeOrigins[Direction.SOUTH.ordinal()].set(this.origin).move(Direction.SOUTH, 16);
        this.relativeOrigins[Direction.WEST.ordinal()].set(this.origin).move(Direction.WEST, 16);
        this.relativeOrigins[Direction.EAST.ordinal()].set(this.origin).move(Direction.EAST, 16);

    }

    public boolean resortTransparency(RenderType renderType, TaskDispatcher taskDispatcher) {
        CompiledSection compiledSection1 = this.getCompiledSection();
        if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
        }

        if (!vbo.translucent || compiledSection1.renderTypes != (renderType)) {
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
        RenderChunkRegion renderchunkregion = renderRegionCache.createRegion(WorldRenderer.level, blockpos.offset(-1, -1, -1), blockpos.offset(16, 16, 16), 1);
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

    public BlockPos getOrigin() {
        return this.origin;
    }

//    public VertexBuffer getBuffer(RenderType renderType) {
//        return buffers.get(renderType);
//    }

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
//        vbo.close();
    }

    public void setDirty(boolean playerChanged) {
        this.playerChanged = playerChanged || this.dirty && this.playerChanged;
        this.dirty = true;
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
//        this.buffers.values().forEach(VertexBuffer::close);

    }

    public static class CompiledSection {
        public static final CompiledSection UNCOMPILED = new CompiledSection() {
            public boolean canSeeThrough(Direction dir1, Direction dir2) {
                return false;
            }
        };
        final RenderType renderTypes = RenderType.translucent();
        boolean isCompletelyEmpty = true;
        final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
        VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        BufferBuilder.SortState transparencyState;

        public CompiledSection() {}
        public CompiledSection(ChunkTask.BuildTask.CompileResults compileResults) {
            this.visibilitySet = compileResults.visibilitySet;
            this.renderableBlockEntities.addAll(compileResults.blockEntities);
            this.transparencyState = compileResults.transparencyState;
        }

        public boolean hasNoRenderableLayers() {
            return this.isCompletelyEmpty;
        }

//        public boolean isEmpty(RenderType p_112759_) {
//            return !this.renderTypes.contains(p_112759_);
//        }

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
