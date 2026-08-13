package net.vulkanmod.render.chunk;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.render.chunk.buffer.AreaBuffer;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.buffer.DrawParametersBuffer;
import net.vulkanmod.render.chunk.build.RenderRegion;
import net.vulkanmod.render.chunk.build.RenderRegionBuilder;
import net.vulkanmod.render.chunk.build.task.TaskDispatcher;
import net.vulkanmod.render.chunk.build.task.BuildTask;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.build.task.CompiledSection;
import net.vulkanmod.render.chunk.build.task.SortTransparencyTask;
import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.chunk.graph.GraphDirections;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.joml.Vector3d;

import java.util.Collection;
import java.util.Set;

public class RenderSection {
    private ChunkArea chunkArea;
    public byte frustumIndex;
    public short lastFrame = -1;
    private short lastFrame2 = -1;
    public short inAreaIndex;

    public byte adjDirs;
    public RenderSection
            adjDown, adjUp,
            adjNorth, adjSouth,
            adjWest, adjEast;

    private final CompileStatus compileStatus = new CompileStatus();

    private boolean dirty = true;
    private boolean playerChanged;
    private boolean completelyEmpty = true;
    private boolean containsBlockEntities = false;

    public long visibility;

    public int xOffset, yOffset, zOffset;

    // Graph-info
    public byte mainDir;
    public byte directions;
    public byte sourceDirs;
    public byte steps;
    public byte directionChanges;

    public RenderSection(int index, int x, int y, int z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public void setOrigin(int x, int y, int z) {
        this.reset();

        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public void addDir(int direction) {
        sourceDirs |= (byte) (1 << direction);
    }

    public void setDirections(byte dirs, int direction) {
        this.directions = (byte) (this.directions | dirs | 1 << direction);
    }

    void setDirectionChanges(byte i) {
        this.directionChanges = i;
    }

    public boolean hasDirection(byte dir) {
        return (this.directions & (1 << dir)) > 0;
    }

    public boolean hasMainDirection() {
        return this.sourceDirs != 0;
    }

    public void setAdjacent(RenderSection adjacent, int direction) {

        switch (direction) {
            case GraphDirections.DOWN -> {
                this.adjDown = adjacent;
                adjacent.adjUp = this;
                addAdjDir(adjacent, direction);
            }

            case GraphDirections.UP -> {
                this.adjUp = adjacent;
                adjacent.adjDown = this;
                addAdjDir(adjacent, direction);
            }

            case GraphDirections.NORTH -> {
                this.adjNorth = adjacent;
                adjacent.adjSouth = this;
                addAdjDir(adjacent, direction);
            }

            case GraphDirections.SOUTH -> {
                this.adjSouth = adjacent;
                adjacent.adjNorth = this;
                addAdjDir(adjacent, direction);
            }

            case GraphDirections.WEST -> {
                this.adjWest = adjacent;
                adjacent.adjEast = this;
                addAdjDir(adjacent, direction);
            }

            case GraphDirections.EAST -> {
                this.adjEast = adjacent;
                adjacent.adjWest = this;
                addAdjDir(adjacent, direction);
            }
        }

    }

    public void resetAdjacent(int direction) {

        RenderSection adjacent;
        switch (direction) {
            case GraphDirections.DOWN -> {
                adjacent = this.adjDown;

                if (adjacent != null) {
                    adjacent.adjUp = null;
                    removeAdjDir(adjacent, direction);
                }
            }

            case GraphDirections.UP -> {
                adjacent = this.adjUp;

                if (adjacent != null) {
                    adjacent.adjDown = null;
                    removeAdjDir(adjacent, direction);
                }
            }

            case GraphDirections.NORTH -> {
                adjacent = this.adjNorth;

                if (adjacent != null) {
                    adjacent.adjSouth = null;
                    removeAdjDir(adjacent, direction);
                }
            }

            case GraphDirections.SOUTH -> {
                adjacent = this.adjSouth;

                if (adjacent != null) {
                    adjacent.adjNorth = null;
                    removeAdjDir(adjacent, direction);
                }
            }

            case GraphDirections.WEST -> {
                adjacent = this.adjWest;

                if (adjacent != null) {
                    adjacent.adjEast = null;
                    removeAdjDir(adjacent, direction);
                }
            }

            case GraphDirections.EAST -> {
                adjacent = this.adjEast;

                if (adjacent != null) {
                    adjacent.adjWest = null;
                    removeAdjDir(adjacent, direction);
                }
            }
        }

    }

    private void addAdjDir(RenderSection adjacent, int direction) {
        this.adjDirs |= (byte) (1 << direction);
        adjacent.adjDirs |= (byte) (1 << Util.getOppositeDirIdx((byte) direction));
    }

    private void removeAdjDir(RenderSection adjacent, int direction) {
        this.adjDirs &= (byte) ~(1 << direction);
        adjacent.adjDirs &= (byte) ~(1 << Util.getOppositeDirIdx((byte) direction));
    }

    public boolean resortTransparency(TaskDispatcher taskDispatcher, Vector3d cameraPos) {
        CompiledSection compiledSection = this.getCompiledSection();

        if (this.compileStatus.sortTask != null) {
            this.compileStatus.sortTask.cancel();
        }

        if (!compiledSection.hasTransparencyState()) {
            return false;
        } else {
            this.compileStatus.sortTask = ChunkTask.createSortTask(this, cameraPos);
            taskDispatcher.schedule(this.compileStatus.sortTask);
            return true;
        }
    }

    public boolean rebuildChunkAsync(TaskDispatcher dispatcher, RenderRegionBuilder renderRegionCache, Vector3d cameraPos) {
        BuildTask chunkCompileTask = this.createCompileTask(renderRegionCache, cameraPos);

        if (chunkCompileTask == null)
            return false;

        dispatcher.schedule(chunkCompileTask);
        return true;
    }

    // TODO: sync rebuild
    public void rebuildChunkSync(TaskDispatcher dispatcher, RenderRegionBuilder renderRegionCache) {
    }

    public BuildTask createCompileTask(RenderRegionBuilder renderRegionCache, Vector3d cameraPos) {
        boolean flag = this.cancelTasks();

        Level level = WorldRenderer.getLevel();
        int secX = xOffset >> 4;
        int secZ = zOffset >> 4;
        int secY = yOffset >> 4;

        if (!ChunkStatusMap.INSTANCE.chunkRenderReady(secX, secZ))
            return null;

        RenderRegion renderRegion = renderRegionCache.createRegion(level, secX, secY, secZ);

        boolean flag1 = this.compileStatus.compiledSection == CompiledSection.UNCOMPILED;
        this.compileStatus.buildTask = ChunkTask.createBuildTask(this, renderRegion, cameraPos, !flag1 || flag);
        return this.compileStatus.buildTask;
    }

    protected boolean cancelTasks() {
        boolean flag = false;
        if (this.compileStatus.buildTask != null) {
            this.compileStatus.buildTask.cancel();
            this.compileStatus.buildTask = null;
            flag = true;
        }

        if (this.compileStatus.sortTask != null) {
            this.compileStatus.sortTask.cancel();
            this.compileStatus.sortTask = null;
        }

        return flag;
    }

    public void setNotDirty() {
        this.dirty = false;
        this.playerChanged = false;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isDirtyFromPlayer() {
        return this.dirty && this.playerChanged;
    }

    public int xOffset() {
        return xOffset;
    }

    public int yOffset() {
        return yOffset;
    }

    public int zOffset() {
        return zOffset;
    }

    public void resetDrawParameters(TerrainRenderType renderType) {
        for (int i = 0; i < QuadFacing.COUNT; ++i) {
            DrawBuffers drawBuffers = this.chunkArea.getDrawBuffers();
            long ptr = DrawParametersBuffer.getParamsPtr(drawBuffers.getDrawParamsPtr(), this.inAreaIndex, renderType.ordinal(), i);

            AreaBuffer areaBuffer = drawBuffers.getAreaBuffer(renderType);
            int vertexOffset = DrawParametersBuffer.getVertexOffset(ptr);
            if (areaBuffer != null && vertexOffset != -1) {
                areaBuffer.freeSegment(vertexOffset);
            }

            DrawParametersBuffer.resetParameters(ptr);
        }
    }

    public void setChunkArea(ChunkArea chunkArea) {
        this.chunkArea = chunkArea;

        this.frustumIndex = chunkArea.getFrustumIndex(xOffset, yOffset, zOffset);
    }

    public ChunkArea getChunkArea() {
        return this.chunkArea;
    }

    public CompiledSection getCompiledSection() {
        return compileStatus.compiledSection;
    }

    public boolean isCompiled() {
        return this.compileStatus.compiledSection != CompiledSection.UNCOMPILED;
    }

    public void setVisibility(long visibility) {
        this.visibility = visibility;
    }

    public void setCompletelyEmpty(boolean b) {
        this.completelyEmpty = b;
    }

    public void setContainsBlockEntities(boolean b) {
        this.containsBlockEntities = b;
    }

    public byte getDirections() {
        return directions;
    }

    public byte getVisibilityDirs() {
        return (byte) (this.visibility >> (Util.getOppositeDirIdx(this.mainDir) << 3));
    }

    public long getVisibility() {
        return visibility;
    }

    public boolean isCompletelyEmpty() {
        return this.completelyEmpty;
    }

    public boolean containsBlockEntities() {
        return this.containsBlockEntities;
    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> fullSet) {
        Set<BlockEntity> sectionSet = compileStatus.globalBlockEntities;

        if (sectionSet.size() != fullSet.size() || !sectionSet.containsAll(fullSet)) {
            Set<BlockEntity> toRemove = Sets.newHashSet(sectionSet);
            Set<BlockEntity> toAdd = Sets.newHashSet(fullSet);
            toAdd.removeAll(sectionSet);
            toRemove.removeAll(fullSet);

            sectionSet.clear();
            sectionSet.addAll(fullSet);

            // TODO
//            Minecraft.getInstance().levelRenderer.updateGlobalBlockEntities(toRemove, toAdd);
        }
    }

    private void reset() {
        this.cancelTasks();
        this.compileStatus.compiledSection = CompiledSection.UNCOMPILED;
        this.dirty = true;
        this.visibility = 0;
        this.completelyEmpty = true;

        this.resetDrawParameters();
    }

    private void resetDrawParameters() {
        if (this.chunkArea == null)
            return;

        long basePtr = this.chunkArea.getDrawBuffers().getDrawParamsPtr();
        for (TerrainRenderType renderType : TerrainRenderType.VALUES) {
            for (QuadFacing facing : QuadFacing.VALUES) {
                long ptr = DrawParametersBuffer.getParamsPtr(basePtr, this.inAreaIndex, renderType.ordinal(), facing.ordinal());
                DrawParametersBuffer.resetParameters(ptr);
            }
        }
    }

    public void setDirty(boolean playerChanged) {
        this.playerChanged = playerChanged || this.dirty && this.playerChanged;
        this.dirty = true;
        WorldRenderer.getInstance().scheduleGraphUpdate();
    }

    public void setCompiledSection(CompiledSection compiledSection) {
        this.compileStatus.compiledSection = compiledSection;
    }

    public boolean setLastFrame(short i) {
        boolean alreadySet = i == this.lastFrame;
        if (!alreadySet)
            this.lastFrame = i;
        return alreadySet;
    }

    public boolean setLastFrame2(short i) {
        boolean alreadySet = i == this.lastFrame2;
        if (!alreadySet)
            this.lastFrame2 = i;
        return alreadySet;
    }

    public short getLastFrame() {
        return this.lastFrame;
    }

    public short getLastFrame2() {
        return this.lastFrame2;
    }

    static class CompileStatus {
        CompiledSection compiledSection = CompiledSection.UNCOMPILED;
        Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();
        BuildTask buildTask;
        SortTransparencyTask sortTask;
    }
}
