package net.vulkanmod.render.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.Profiler;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.chunk.util.ChunkQueue;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.VRenderSystem;
import com.mojang.math.Matrix4f;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;

public class WorldRenderer {
    private static WorldRenderer INSTANCE;

    private Minecraft minecraft;

    private ClientLevel level;
    private int lastViewDistance;
    private RenderBuffers renderBuffers;

    private Vec3 cameraPos;
    private int lastCameraSectionX;
    private int lastCameraSectionY;
    private int lastCameraSectionZ;
    private float lastCameraX;
    private float lastCameraY;
    private float lastCameraZ;
    private float lastCamRotX;
    private float lastCamRotY;

    private ChunkGrid chunkGrid;

    private boolean needsUpdate;
    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();

    private TaskDispatcher taskDispatcher;
    private final ChunkQueue chunkQueue = new ChunkQueue();
    private int lastFrame = 0;
    private final ObjectArrayList<QueueChunkInfo> sectionsInFrustum = new ObjectArrayList<>(10000);

    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;

    public ObjectArrayList<RenderSection> solidChunks = new ObjectArrayList<>();
    public ObjectArrayList<RenderSection> cutoutChunks = new ObjectArrayList<>();
    public ObjectArrayList<RenderSection> cutoutMippedChunks = new ObjectArrayList<>();
    public ObjectArrayList<RenderSection> tripwireChunks = new ObjectArrayList<>();
    public ObjectArrayList<RenderSection> translucentChunks = new ObjectArrayList<>();

    private Frustum frustum;

    RenderRegionCache renderRegionCache;

    private WorldRenderer(RenderBuffers renderBuffers) {
        this.minecraft = Minecraft.getInstance();
//        this.levelRenderer = levelRenderer;
        this.renderBuffers = renderBuffers;
        this.taskDispatcher = new TaskDispatcher(net.minecraft.Util.backgroundExecutor(), this.renderBuffers.fixedBufferPack());
        ChunkTask.setTaskDispatcher(this.taskDispatcher);
    }

    public static WorldRenderer init(RenderBuffers renderBuffers) {
        if(INSTANCE != null) throw new RuntimeException("WorldRenderer re-initialization");
        return INSTANCE = new WorldRenderer(renderBuffers);
    }

    public static WorldRenderer getInstance() {
        return INSTANCE;
    }

    public static ClientLevel getLevel() {
        return INSTANCE.level;
    }

    public static Vec3 getCameraPos() {
        return INSTANCE.cameraPos;
    }

    public void setupRenderer(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        Profiler p = Profiler.getProfiler("chunks");
        p.start();

        this.frustum = frustum.offsetToFullyIncludeCameraCube(8);
        this.cameraPos = camera.getPosition();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }

        this.level.getProfiler().push("camera");
        float cameraX = (float)cameraPos.x();
        float cameraY = (float)cameraPos.y();
        float cameraZ = (float)cameraPos.z();
        int sectionX = SectionPos.posToSectionCoord(cameraX);
        int sectionY = SectionPos.posToSectionCoord(cameraY);
        int sectionZ = SectionPos.posToSectionCoord(cameraZ);

//        Profiler p2 = Profiler.getProfiler("camera");

        if (this.lastCameraSectionX != sectionX || this.lastCameraSectionY != sectionY || this.lastCameraSectionZ != sectionZ) {

//            p2.start();
            this.lastCameraSectionX = sectionX;
            this.lastCameraSectionY = sectionY;
            this.lastCameraSectionZ = sectionZ;
            this.chunkGrid.repositionCamera(cameraX, cameraZ);
//            p2.pushMilestone("end-reposition");
        }

        double entityDistanceScaling = this.minecraft.options.entityDistanceScaling().get();
        Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * entityDistanceScaling);

//        this.chunkRenderDispatcher.setCamera(cameraPos);
        this.level.getProfiler().popPush("cull");
        this.minecraft.getProfiler().popPush("culling");
        BlockPos blockpos = camera.getBlockPosition();

        this.minecraft.getProfiler().popPush("update");

        boolean flag = this.minecraft.smartCull;
        if (spectator && this.level.getBlockState(blockpos).isSolidRender(this.level, blockpos)) {
            flag = false;
        }

        float d_xRot = Math.abs(camera.getXRot() - this.lastCamRotX);
        float d_yRot = Math.abs(camera.getYRot() - this.lastCamRotY);
        this.needsUpdate |= d_xRot > 2.0f || d_yRot > 2.0f;

        this.needsUpdate |= cameraX != this.lastCameraX || cameraY != this.lastCameraY || cameraZ != this.lastCameraZ;

        if (!isCapturedFrustum) {

            if (this.needsUpdate) {
                this.chunkGrid.updateFrustumVisibility((((FrustumMixed)(frustum)).customFrustum()).offsetToFullyIncludeCameraCube(8));
                this.lastCameraX = cameraX;
                this.lastCameraY = cameraY;
                this.lastCameraZ = cameraZ;
                this.lastCamRotX = camera.getXRot();
                this.lastCamRotY = camera.getYRot();

                p.push("frustum");

                this.minecraft.getProfiler().push("partial_update");
//                Queue<QueueChunkInfo> queue = Queues.newArrayDeque();

                this.chunkQueue.clear();
                this.initializeQueueForFullUpdate(camera);

                this.renderRegionCache = new RenderRegionCache();

                this.updateRenderChunks();

                this.needsUpdate = false;
                this.minecraft.getProfiler().pop();
            }

            p.pushMilestone("update");
//            p.round();

        }

        this.minecraft.getProfiler().pop();
    }

    private void initializeQueueForFullUpdate(Camera camera) {
        int i = 16;
        Vec3 vec3 = camera.getPosition();
        BlockPos blockpos = camera.getBlockPosition();
        RenderSection renderSection = this.chunkGrid.getRenderSectionAt(blockpos);
        if (renderSection == null) {
//            boolean flag = blockpos.getY() > this.level.getMinBuildHeight();
//            int j = flag ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
//            int k = Mth.floor(vec3.x / 16.0D) * 16;
//            int l = Mth.floor(vec3.z / 16.0D) * 16;
//            List<WorldRenderer.QueueChunkInfo> list = Lists.newArrayList();
//
//            for(int i1 = -this.lastViewDistance; i1 <= this.lastViewDistance; ++i1) {
//                for(int j1 = -this.lastViewDistance; j1 <= this.lastViewDistance; ++j1) {
//                    ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk1 = this.viewArea.getRenderChunkAt(new BlockPos(k + SectionPos.sectionToBlockCoord(i1, 8), j, l + SectionPos.sectionToBlockCoord(j1, 8)));
//                    if (chunkrenderdispatcher$renderchunk1 != null) {
//                        list.add(new WorldRenderer.QueueChunkInfo(chunkrenderdispatcher$renderchunk1, (Direction)null, 0));
//                    }
//                }
//            }
//
//            list.sort(Comparator.comparingDouble((p_194358_) -> {
//                return blockpos.distSqr(p_194358_.chunk.getOrigin().offset(8, 8, 8));
//            }));
//            //TODO
//            chunkInfoQueue.addAll(list);
        } else {
            QueueChunkInfo chunkInfo = new QueueChunkInfo(renderSection, (Direction)null, 0);
            this.chunkQueue.add(chunkInfo);
        }

    }

    private void updateRenderChunks() {
//        Profiler p = Profiler.getProfiler("chunk setup");
//        p.start();

        int maxDirectionsChanges = Initializer.CONFIG.advCulling;

        //stats
//        int mainLoop = 0;
//        int added = 0;

        this.sectionsInFrustum.clear();

        this.solidChunks.clear();
        this.cutoutChunks.clear();
        this.cutoutMippedChunks.clear();
        this.tripwireChunks.clear();
        this.translucentChunks.clear();

        this.lastFrame++;

//        p.push("pre-loop");
        while(this.chunkQueue.hasNext()) {
            QueueChunkInfo renderChunkInfo = this.chunkQueue.poll();
            RenderSection renderSection = renderChunkInfo.chunk;

            this.sectionsInFrustum.add(renderChunkInfo);

            this.scheduleUpdate(renderSection);

            renderSection.compiledSection.renderTypes.forEach(
                    renderType -> {
                        if (RenderType.solid().equals(renderType)) {
                            solidChunks.add(renderSection);
                        }
                        else if (RenderType.cutout().equals(renderType)) {
                            cutoutChunks.add(renderSection);
                        }
                        else if (RenderType.cutoutMipped().equals(renderType)) {
                            cutoutMippedChunks.add(renderSection);
                        }
                        else if (RenderType.translucent().equals(renderType)) {
                            translucentChunks.add(renderSection);
                        }
                        else if (RenderType.tripwire().equals(renderType)) {
                            tripwireChunks.add(renderSection);
                        }
                    }
            );

//            mainLoop++;

            if(renderChunkInfo.directionChanges > maxDirectionsChanges)
                continue;

            //debug
//            BlockPos pos1 = renderSection.getOrigin();
//            if(pos1.getX() == (-70 << 4) && pos1.getY() == (4 << 4) && pos1.getZ() == (-149 << 4))
//                System.nanoTime();

//         Direction oppositeDir = null;
//         if (renderChunkInfo.hasSourceDirections()) oppositeDir = renderChunkInfo.mainDir.getOpposite();

            for(Direction direction : Util.DIRECTIONS) {
                RenderSection relativeChunk = renderSection.getNeighbour(direction);

                if (relativeChunk != null && !renderChunkInfo.hasDirection(direction.getOpposite())) {

                    if (renderChunkInfo.hasMainDirection()) {

                        RenderSection.CompiledSection compiledSection = renderSection.getCompiledSection();
                        boolean flag1 = false;

//                  for(int j = 0; j < DIRECTIONS.length; ++j) {
//                     if (renderChunkInfo.hasSourceDirection(j) && compiledSection.facesCanSeeEachother(DIRECTIONS[j].getOpposite(), direction)) {
//                        flag1 = true;
//                        break;
//                     }
//                  }

//                        if(compiledSection.hasNoRenderableLayers()) continue;

                        if (compiledSection.canSeeThrough(renderChunkInfo.mainDir.getOpposite(), direction)) {
                            flag1 = true;
                        }

                        if (!flag1) {
                            continue;
                        }
                    }

                    if (relativeChunk.setLastFrame(this.lastFrame)) {

                        int d;
                        if (renderChunkInfo.mainDir != direction && !renderSection.compiledSection.hasNoRenderableLayers())
                            d = renderChunkInfo.directionChanges + 1;
                        else d = renderChunkInfo.directionChanges;

                        QueueChunkInfo chunkInfo = relativeChunk.queueInfo;
                        if(chunkInfo != null) {
                            chunkInfo.addDir(direction);

                            if(d < chunkInfo.directionChanges) chunkInfo.setDirectionChanges(d);
                        }

                    }
                    else if (relativeChunk.getChunkArea().inFrustum() < 0) {
                        //TODO later: check frustum on intersections

                        QueueChunkInfo chunkInfo = new QueueChunkInfo(relativeChunk, direction, renderChunkInfo.step + 1);
                        chunkInfo.setDirections(renderChunkInfo.directions, direction);
                        this.chunkQueue.add(chunkInfo);

                        int d;
//                        if (renderChunkInfo.mainDir != direction && !renderSection.compiledSection.hasNoRenderableLayers())
//                        if (renderChunkInfo.mainDir != null && renderChunkInfo.mainDir.ordinal() != direction.ordinal() && !renderSection.compiledSection.hasNoRenderableLayers())
                        if ((renderChunkInfo.sourceDirs & (1 << direction.ordinal())) == 0 && !renderSection.compiledSection.hasNoRenderableLayers())
                        {
                            if(renderChunkInfo.step > 4) {
                                d = renderChunkInfo.directionChanges + 1;
                            }
                            else {
                                d = 0;
                            }
//                            d = renderChunkInfo.directionChanges + 1;
                        }

                        else d = renderChunkInfo.directionChanges;

                        chunkInfo.setDirectionChanges(d);
                        relativeChunk.queueInfo = chunkInfo;
                    }

                }
            }
        }

//        p.round();
//        mainLoop++;
    }

    public void scheduleUpdate(RenderSection section) {
        if(!section.isDirty() || !section.isLightReady()) return;

        section.rebuildChunkAsync(this.taskDispatcher, this.renderRegionCache);
        section.setNotDirty();

    }

    public void compileChunks(Camera camera) {
        this.minecraft.getProfiler().push("populate_chunks_to_compile");
        RenderRegionCache renderregioncache = new RenderRegionCache();
        BlockPos cameraPos = camera.getBlockPosition();
        List<RenderSection> list = Lists.newArrayList();

        this.minecraft.getProfiler().popPush("upload");
        this.taskDispatcher.uploadAllPendingUploads();
//        CompletableFuture.runAsync(() -> this.taskDispatcher.uploadAllPendingUploads());
        this.minecraft.getProfiler().popPush("schedule_async_compile");

//        //debug
//        Profiler p = null;
//        if(!list.isEmpty()) {
//            p = Profiler.getProfiler("compileChunks");
//            p.start();
//        }


//        for(RenderSection renderSection : list) {
//            renderSection.rebuildChunkAsync(this.taskDispatcher, renderregioncache);
////            renderSection.rebuildChunkSync(this.taskDispatcher, renderregioncache);
//            renderSection.setNotDirty();
//        }

//        if(!list.isEmpty()) {
//            p.round();
//        }
        this.minecraft.getProfiler().pop();
    }

    public boolean isChunkCompiled(BlockPos blockPos) {
        RenderSection renderSection = this.chunkGrid.getRenderSectionAt(blockPos);
        return renderSection != null && renderSection.compiledSection != RenderSection.CompiledSection.UNCOMPILED;
    }

    public void allChanged() {
        if (this.level != null) {
//            this.graphicsChanged();
            this.level.clearTintCaches();
//            if (this.taskDispatcher == null) {
//                this.taskDispatcher = new TaskDispatcher(Util.backgroundExecutor(), this.renderBuffers.fixedBufferPack());
//            } else {
//                this.taskDispatcher.setLevel(this.level);
//            }

            this.needsUpdate = true;
//            this.generateClouds = true;
//            this.recentlyCompiledChunks.clear();
//            ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
            if (this.chunkGrid != null) {
                this.chunkGrid.releaseAllBuffers();
            }

            this.taskDispatcher.clearBatchQueue();
            synchronized(this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }

            this.chunkGrid = new ChunkGrid(this.level, this.minecraft.options.getEffectiveRenderDistance());

            this.sectionsInFrustum.clear();
            Entity entity = this.minecraft.getCameraEntity();
            if (entity != null) {
                this.chunkGrid.repositionCamera(entity.getX(), entity.getZ());
            }

        }
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.lastCameraX = Float.MIN_VALUE;
        this.lastCameraY = Float.MIN_VALUE;
        this.lastCameraZ = Float.MIN_VALUE;
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
//        this.entityRenderDispatcher.setLevel(level);
        this.level = level;
        if (level != null) {
            if(this.chunkGrid == null) {
                this.allChanged();
            }
        } else {
            if (this.chunkGrid != null) {
                this.chunkGrid.releaseAllBuffers();
                this.chunkGrid = null;
            }

//            if (this.chunkRenderDispatcher != null) {
//                this.chunkRenderDispatcher.dispose();
//            }
//            if (this.taskDispatcher != null) {
//                this.taskDispatcher.dispose();
//            }

//            this.chunkRenderDispatcher = null;
//            this.globalBlockEntities.clear();
//            this.renderChunkStorage.set((LevelRenderer.RenderChunkStorage)null);
            this.sectionsInFrustum.clear();
        }

    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> p_109763_, Collection<BlockEntity> p_109764_) {
        synchronized(this.globalBlockEntities) {
            this.globalBlockEntities.removeAll(p_109763_);
            this.globalBlockEntities.addAll(p_109764_);
        }
    }

    public void renderChunkLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projection) {
        //debug
        Profiler p = Profiler.getProfiler("chunks");
        RenderType solid = RenderType.solid();
        RenderType cutout = RenderType.cutout();
        RenderType cutoutMipped = RenderType.cutoutMipped();
        RenderType translucent = RenderType.translucent();
        RenderType tripwire = RenderType.tripwire();

        String layerName;
        if (solid.equals(renderType)) {
            layerName = "solid";
        } else if (cutout.equals(renderType)) {
            layerName = "cutout";
        } else if (cutoutMipped.equals(renderType)) {
            layerName = "cutoutMipped";
        } else if (tripwire.equals(renderType)) {
            layerName = "tripwire";
        } else if (translucent.equals(renderType)) {
            layerName = "translucent";
        } else layerName = "unk";

        p.pushMilestone("layer " + layerName);

        RenderSystem.assertOnRenderThread();
        renderType.setupRenderState();
        if (renderType == RenderType.translucent()) {
            this.minecraft.getProfiler().push("translucent_sort");
            double d0 = camX - this.xTransparentOld;
            double d1 = camY - this.yTransparentOld;
            double d2 = camZ - this.zTransparentOld;
            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D) {
                this.xTransparentOld = camX;
                this.yTransparentOld = camY;
                this.zTransparentOld = camZ;
                int j = 0;

                for(QueueChunkInfo chunkInfo : this.sectionsInFrustum) {
                    if (j < 15 && chunkInfo.chunk.resortTransparency(renderType, this.taskDispatcher)) {
                        ++j;
                    }
                }
            }

            this.minecraft.getProfiler().pop();
        }

        this.minecraft.getProfiler().push("filterempty");
        this.minecraft.getProfiler().popPush(() -> {
            return "render_" + renderType;
        });
        boolean flag = renderType != RenderType.translucent();

        ObjectArrayList<RenderSection> sections;
        if (RenderType.solid().equals(renderType)) {
            sections = this.solidChunks;
        }
        else if (RenderType.cutout().equals(renderType)) {
            sections = this.cutoutChunks;
        }
        else if (RenderType.cutoutMipped().equals(renderType)) {
            sections = this.cutoutMippedChunks;
        }
        else if (RenderType.translucent().equals(renderType)) {
            sections = this.translucentChunks;
        }
        else if (RenderType.tripwire().equals(renderType)) {
            sections = this.tripwireChunks;
        } else {
            sections = ObjectArrayList.of();
        }

//        ObjectListIterator<WorldRenderer.QueueChunkInfo> iterator = this.sectionsInFrustum.listIterator(flag ? 0 : this.sectionsInFrustum.size());
        ObjectListIterator<RenderSection> iterator = sections.listIterator(flag ? 0 : sections.size());

        VertexFormat vertexformat = renderType.format();

        VRenderSystem.applyMVP(poseStack.last().pose(), projection);


        Drawer drawer = Drawer.getInstance();
        Pipeline pipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
        drawer.bindPipeline(pipeline);

        drawer.uploadAndBindUBOs(pipeline);

        Supplier<Boolean> checker = flag ? iterator::hasNext : iterator::hasPrevious;
        Supplier<RenderSection> getter = flag ? iterator::next : iterator::previous;

//        Profiler p1 = Profiler.getProfiler("drawCmds");
//        p1.start();
//        Profiler.setCurrentProfiler(p1);

        while (checker.get()) {
            RenderSection renderSection = getter.get();

            VBO vertexbuffer = renderSection.getBuffer(renderType);
            BlockPos blockpos = renderSection.getOrigin();

//            p1.push("start");
//            p1.push("set_push-const");
            VRenderSystem.setChunkOffset((float) ((double) blockpos.getX() - camX), (float) ((double) blockpos.getY() - camY), (float) ((double) blockpos.getZ() - camZ));
//            p1.push("push_const");
            drawer.pushConstants(pipeline);
////
            vertexbuffer.drawChunkLayer();
//            Profiler.Push("end");

//            flag1 = true;
        }

//        p1.end();

        //Need to reset push constant in case the pipeline will still be used for rendering
        VRenderSystem.setChunkOffset(0, 0, 0);
        drawer.pushConstants(pipeline);

        this.minecraft.getProfiler().pop();
        renderType.clearRenderState();

        VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

    }

    public void renderBlockEntities(PoseStack poseStack, double camX, double camY, double camZ,
                                    Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress, float gameTime) {
        MultiBufferSource bufferSource = this.renderBuffers.bufferSource();

        for(QueueChunkInfo info : this.sectionsInFrustum) {
            List<BlockEntity> list = info.chunk.getCompiledSection().getRenderableBlockEntities();
            if (!list.isEmpty()) {
                for(BlockEntity blockentity1 : list) {
                    BlockPos blockpos4 = blockentity1.getBlockPos();
                    MultiBufferSource multibuffersource1 = bufferSource;
                    poseStack.pushPose();
                    poseStack.translate((double)blockpos4.getX() - camX, (double)blockpos4.getY() - camY, (double)blockpos4.getZ() - camZ);
                    SortedSet<BlockDestructionProgress> sortedset = destructionProgress.get(blockpos4.asLong());
                    if (sortedset != null && !sortedset.isEmpty()) {
                        int j1 = sortedset.last().getProgress();
                        if (j1 >= 0) {
                            PoseStack.Pose posestack$pose1 = poseStack.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(j1)), posestack$pose1.pose(), posestack$pose1.normal());
                            multibuffersource1 = (p_194349_) -> {
                                VertexConsumer vertexconsumer3 = bufferSource.getBuffer(p_194349_);
                                return p_194349_.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                            };
                        }
                    }

                    this.minecraft.getBlockEntityRenderDispatcher().render(blockentity1, gameTime, poseStack, multibuffersource1);
                    poseStack.popPose();
                }
            }
        }
    }

    public void setNeedsUpdate() {
        this.needsUpdate = true;
    }

    public void setSectionDirty(int x, int y, int z, boolean flag) {
        this.chunkGrid.setDirty(x, y, z, flag);
    }

    public void setSectionsLightReady(int x, int z) {
        List<RenderSection> list = this.chunkGrid.getRenderSectionsAt(x, z);
        list.forEach(section -> section.setLightReady(true));
        this.needsUpdate = true;
    }

    public String getChunkStatistics() {
        int i = this.chunkGrid.chunks.length;
        int j = this.sectionsInFrustum.size();
        return String.format("Chunks: %d/%d D: %d, %s", j, i, this.lastViewDistance, this.taskDispatcher == null ? "null" : this.taskDispatcher.getStats());
    }

    public static class QueueChunkInfo {
        final RenderSection chunk;
        byte directions;
        final int step;
        int directionChanges;

        public final Direction mainDir;

        byte sourceDirs;

        QueueChunkInfo(RenderSection renderSection, @Nullable Direction from, int step) {
            this.chunk = renderSection;
            mainDir = from;

            sourceDirs = (byte) (from != null ? 1 << from.ordinal() : 0);

            this.step = step;
        }

        public void addDir(Direction direction) {
            sourceDirs |= 1 << direction.ordinal();
        }

        public void setDirections(byte p_109855_, Direction p_109856_) {
            this.directions = (byte)(this.directions | p_109855_ | 1 << p_109856_.ordinal());
        }

        void setDirectionChanges(int i) {
            this.directionChanges = i;
        }

        public boolean hasDirection(Direction p_109860_) {
            return (this.directions & 1 << p_109860_.ordinal()) > 0;
        }

        public boolean hasMainDirection() {
            return this.sourceDirs != 0;
        }

    }
}
