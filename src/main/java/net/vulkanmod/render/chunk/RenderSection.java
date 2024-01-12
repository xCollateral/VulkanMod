package net.vulkanmod.render.chunk;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.render.chunk.build.ChunkTask;
import net.vulkanmod.render.chunk.build.CompiledSection;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.vertex.TerrainRenderType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RenderSection {
    static final Map<RenderSection, Set<BlockEntity>> globalBlockEntitiesMap = new Reference2ReferenceOpenHashMap<>();

    private ChunkArea chunkArea;
    private final RenderSection[] neighbours = new RenderSection[6];
    public byte frustumIndex;
    private short lastFrame = -1;
    private short lastFrame2 = -1;

    private final CompileStatus compileStatus = new CompileStatus();

    private boolean dirty = true;
    private boolean playerChanged;

    private boolean completelyEmpty = true;
    private long visibility;

    int xOffset, yOffset, zOffset;

//    private final DrawBuffers.DrawParameters[] drawParametersArray =
//            Arrays.stream(TerrainRenderType.VALUES)
//                    .map(terrainRenderType -> new DrawBuffers.DrawParameters(terrainRenderType == TerrainRenderType.TRANSLUCENT))
//                    .toArray(DrawBuffers.DrawParameters[]::new);
    private final DrawBuffers.DrawParameters[] drawParametersArray;

    //Graph-info
    public Direction mainDir;
    public byte directions;
    public byte step;
    public byte directionChanges;
    byte sourceDirs;


    public RenderSection(int index, int x, int y, int z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;

        this.drawParametersArray = new DrawBuffers.DrawParameters[TerrainRenderType.VALUES.length];
        for(int i = 0; i < this.drawParametersArray.length; ++i) {
            this.drawParametersArray[i] = new DrawBuffers.DrawParameters(TerrainRenderType.VALUES[i] == TerrainRenderType.TRANSLUCENT);
        }
    }

    public void setOrigin(int x, int y, int z) {
        this.reset();

        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;

    }

    public RenderSection setGraphInfo(@Nullable Direction from, byte step) {
        mainDir = from;

        sourceDirs = (byte) (from != null ? 1 << from.ordinal() : 0);

        this.step = step;
        this.directions = 0;
        this.directionChanges = 0;
        return this;
    }

    public void addDir(Direction direction) {
        if(sourceDirs == 0)
            return;
        sourceDirs |= 1 << direction.ordinal();
    }

    public void setDirections(byte p_109855_, Direction p_109856_) {
        this.directions = (byte)(this.directions | p_109855_ | 1 << p_109856_.ordinal());
    }

    void setDirectionChanges(byte i) {
        this.directionChanges = i;
    }

    public boolean hasDirection(Direction p_109860_) {
        return (this.directions & 1 << p_109860_.ordinal()) > 0;
    }

    public boolean hasMainDirection() {
        return this.sourceDirs != 0;
    }

    public void resortTransparency(TaskDispatcher taskDispatcher) {

        if (this.compileStatus.sortTask != null) {
            this.compileStatus.sortTask.cancel();
        }

        if (this.getCompiledSection().renderTypes.contains(TerrainRenderType.TRANSLUCENT)) {
            this.compileStatus.sortTask = new ChunkTask.SortTransparencyTask(this);
            taskDispatcher.schedule(this.compileStatus.sortTask);
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
        BlockPos blockpos = new BlockPos(this.xOffset, this.yOffset, this.zOffset).immutable();
        RenderChunkRegion renderchunkregion = renderRegionCache.createRegion(WorldRenderer.getLevel(), blockpos.offset(-1, -1, -1), blockpos.offset(16, 16, 16), 1);
        boolean flag1 = this.compileStatus.compiledSection == CompiledSection.UNCOMPILED;

        this.compileStatus.rebuildTask = ChunkTask.createBuildTask(this, renderchunkregion, !flag1);
        return this.compileStatus.rebuildTask;
    }

    protected boolean cancelTasks() {
        boolean flag = false;
        if (this.compileStatus.rebuildTask != null) {
            this.compileStatus.rebuildTask.cancel();
            this.compileStatus.rebuildTask = null;
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

    public boolean isDirty() { return this.dirty; }

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

    public DrawBuffers.DrawParameters getDrawParameters(TerrainRenderType renderType) {
        return drawParametersArray[renderType.ordinal()];
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

    public boolean visibilityBetween(Direction dir1, Direction dir2) {
        return (this.visibility & (1L << ((dir1.ordinal() << 3) + dir2.ordinal()))) != 0;
    }

    public boolean isCompletelyEmpty() {
        return this.completelyEmpty;
    }

    public boolean hasXYNeighbours() {
        return true; //checks if chunks exist in level
    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> fullSet) {
        Set<BlockEntity> set = Sets.newHashSet(fullSet);
        Set<BlockEntity> set1;
        Set<BlockEntity> sectionSet;
        synchronized(globalBlockEntitiesMap) {
            sectionSet = globalBlockEntitiesMap.computeIfAbsent(this,
                    (section) -> new HashSet<>());
        }

        if(sectionSet.size() != fullSet.size() || !sectionSet.containsAll(fullSet)) {
            set1 = Sets.newHashSet(sectionSet);
            set.removeAll(sectionSet);
            set1.removeAll(fullSet);

            sectionSet.clear();
            sectionSet.addAll(fullSet);

            Minecraft.getInstance().levelRenderer.updateGlobalBlockEntities(set1, set);
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
        for(TerrainRenderType r : this.getCompiledSection().renderTypes) {
            getDrawParameters(r).reset(this.chunkArea, r);
        }
    }

    public void setDirty(boolean playerChanged) {
        this.playerChanged = playerChanged || this.dirty && this.playerChanged;
        this.dirty = true;
        WorldRenderer.getInstance().setNeedsUpdate();
    }

    public void setCompiledSection(CompiledSection compiledSection) {
        this.compileStatus.compiledSection = compiledSection;
    }

    public boolean setLastFrame(short i) {
        boolean res = i == this.lastFrame ;
        if(!res)
            this.lastFrame = i;
        return res;
    }

    public boolean setLastFrame2(short i) {
        boolean res = i == this.lastFrame2 ;
        if(!res)
            this.lastFrame2 = i;
        return res;
    }

    public short getLastFrame() {
        return this.lastFrame;
    }

    static class CompileStatus {
        CompiledSection compiledSection = CompiledSection.UNCOMPILED;
        ChunkTask.BuildTask rebuildTask;
        ChunkTask.SortTransparencyTask sortTask;
    }
}
