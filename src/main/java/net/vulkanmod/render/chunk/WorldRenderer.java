package net.vulkanmod.render.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import net.vulkanmod.config.Config;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.Profiler;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.chunk.util.ChunkQueue;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.chunk.util.VBOUtil;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static net.vulkanmod.render.chunk.util.VBOUtil.*;
import static net.vulkanmod.vulkan.Drawer.pBuffers;
import static net.vulkanmod.vulkan.Drawer.pOffsets;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;

public class WorldRenderer {
    private static WorldRenderer INSTANCE;

    private final Minecraft minecraft;

    private ClientLevel level;
    private int lastViewDistance;
    private final RenderBuffers renderBuffers;

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

    private final TaskDispatcher taskDispatcher;
    private final ChunkQueue chunkQueue = new ChunkQueue();
    private int lastFrame = 0;
    private final ObjectArrayList<QueueChunkInfo> sectionsInFrustum = new ObjectArrayList<>(10000);

    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;

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

                if(flag)
                    this.updateRenderChunks();
                else
                    this.updateRenderChunksSpectator();

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
            boolean flag = blockpos.getY() > this.level.getMinBuildHeight();
            int j = flag ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
            int k = Mth.floor(vec3.x / 16.0D) * 16;
            int l = Mth.floor(vec3.z / 16.0D) * 16;
            List<WorldRenderer.QueueChunkInfo> list = Lists.newArrayList();

            for(int i1 = -this.lastViewDistance; i1 <= this.lastViewDistance; ++i1) {
                for(int j1 = -this.lastViewDistance; j1 <= this.lastViewDistance; ++j1) {

                    RenderSection renderSection1 = this.chunkGrid.getRenderSectionAt(new BlockPos(k + SectionPos.sectionToBlockCoord(i1, 8), j, l + SectionPos.sectionToBlockCoord(j1, 8)));
                    if (renderSection1 != null) {
                        list.add(new QueueChunkInfo(renderSection1, null, 0));

                    }
                }
            }

            //Maybe not needed
//            list.sort(Comparator.comparingDouble((p_194358_) -> {
//                return blockpos.distSqr(p_194358_.chunk.getOrigin().offset(8, 8, 8));
//            }));

            for (QueueChunkInfo chunkInfo : list) {
                this.chunkQueue.add(chunkInfo);
            }

        } else {
            QueueChunkInfo chunkInfo = new QueueChunkInfo(renderSection, null, 0);
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

        cutoutChunks.clear();
        translucentChunks.clear();

        this.lastFrame++;

//        p.push("pre-loop");
        while(this.chunkQueue.hasNext()) {
            QueueChunkInfo renderChunkInfo = this.chunkQueue.poll();
            RenderSection renderSection = renderChunkInfo.chunk;

            this.sectionsInFrustum.add(renderChunkInfo);

            this.scheduleUpdate(renderSection);

            renderSection.compiledSection.renderTypes.stream().filter(renderType ->
                    !renderSection.getBuffer(renderType).preInitalised).forEach(renderType -> {
                        final VBO buffer = renderSection.getBuffer(renderType);
                        switch (buffer.type) {
                            case CUTOUT -> cutoutChunks.add(buffer);
                            case TRANSLUCENT -> translucentChunks.add(buffer);
                        }
            });

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
                        boolean flag1 = compiledSection.canSeeThrough(renderChunkInfo.mainDir.getOpposite(), direction);

//                  for(int j = 0; j < DIRECTIONS.length; ++j) {
//                     if (renderChunkInfo.hasSourceDirection(j) && compiledSection.facesCanSeeEachother(DIRECTIONS[j].getOpposite(), direction)) {
//                        flag1 = true;
//                        break;
//                     }
//                  }

//                        if(compiledSection.hasNoRenderableLayers()) continue;

                        if (!flag1) {
                            continue;
                        }
                    }

                    this.addNode(renderChunkInfo, relativeChunk, direction);

                }
            }
        }

//        p.round();
//        mainLoop++;
    }

    private void updateRenderChunksSpectator() {

        int maxDirectionsChanges = Initializer.CONFIG.advCulling;

        this.sectionsInFrustum.clear();

        VBOUtil.cutoutChunks.clear();
        VBOUtil.translucentChunks.clear();

        this.lastFrame++;

        while(this.chunkQueue.hasNext()) {
            QueueChunkInfo renderChunkInfo = this.chunkQueue.poll();
            RenderSection renderSection = renderChunkInfo.chunk;

            this.sectionsInFrustum.add(renderChunkInfo);

            this.scheduleUpdate(renderSection);

            renderSection.compiledSection.renderTypes.stream().filter(renderType ->
                    !renderSection.getBuffer(renderType).preInitalised).forEach(renderType -> {
                final VBO buffer = renderSection.getBuffer(renderType);
                switch (buffer.type) {
                    case CUTOUT -> cutoutChunks.add(buffer);
                    case TRANSLUCENT -> translucentChunks.add(buffer);
                }
            });

            if(renderChunkInfo.directionChanges > maxDirectionsChanges)
                continue;

            for(Direction direction : Util.DIRECTIONS) {
                RenderSection relativeChunk = renderSection.getNeighbour(direction);

                if (relativeChunk != null && !renderChunkInfo.hasDirection(direction.getOpposite())) {

                    this.addNode(renderChunkInfo, relativeChunk, direction);

                }
            }
        }

    }

    private void addNode(QueueChunkInfo renderChunkInfo, RenderSection relativeChunk, Direction direction) {
        if (relativeChunk.setLastFrame(this.lastFrame)) {

            int d;
            if (renderChunkInfo.mainDir != direction && !renderChunkInfo.chunk.compiledSection.hasNoRenderableLayers())
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
            if ((renderChunkInfo.sourceDirs & (1 << direction.ordinal())) == 0 && !renderChunkInfo.chunk.compiledSection.hasNoRenderableLayers())
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



    public void scheduleUpdate(RenderSection section) {
        if(!section.isDirty() || !section.isLightReady()) return;

        //TODO sync rebuilds
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
        resetOrigin();
        resetAllBuffers();
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

    public static void resetAllBuffers() {

        Drawer.skipRendering=true;
        cutoutChunks.clear();
        translucentChunks.clear();
//        nvkWaitForFences(Vulkan.getDevice(), Drawer.inFlightFences.capacity(), Drawer.inFlightFences.address0(), 1, -1);
        vkDeviceWaitIdle(Vulkan.getDevice()); //Use a heavier Wait to avoid potential crashes
//        if(size> maxGPUMemLimit) size= (int) maxGPUMemLimit;

//        virtualBufferIdx.reset();
        virtualBufferVtx.reset();
        virtualBufferVtx2.reset();

        Drawer.skipRendering=false;

    }

    private void resetOrigin() {
        originX=0;
        originZ=0;
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

    public void renderChunkLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f projection) {
        //debug
        Profiler p = Profiler.getProfiler("chunks");
        final RenderTypes layer = getLayer(renderType);

        if((layer == RenderTypes.CUTOUT ? cutoutChunks : translucentChunks).isEmpty()) return;
        p.pushMilestone("layer " + layer.name);

        RenderSystem.assertOnRenderThread();
        renderType.setupRenderState();


        this.minecraft.getProfiler().push("filterempty");
        this.minecraft.getProfiler().popPush(() -> {
            return "render_" + renderType;
        });



//        ObjectListIterator<WorldRenderer.QueueChunkInfo> iterator = this.sectionsInFrustum.listIterator(flag ? 0 : this.sectionsInFrustum.size());


        //Use seperate matrix to avoid Incorrect translations propagating to Particles/Lines Layer

        VRenderSystem.applyMVP(translationOffset, projection);


        Drawer drawer = Drawer.getInstance();
        Pipeline pipeline = ((ShaderMixed)(layer.shader)).getPipeline();
        drawer.bindPipeline(pipeline);

        drawer.uploadAndBindUBOs(pipeline);


        final boolean b = layer != RenderTypes.TRANSLUCENT;
        VkCommandBuffer commandBuffer = Drawer.commandBuffers.get(Drawer.getCurrentFrame());
        vkCmdBindIndexBuffer(commandBuffer, Drawer.getInstance().getQuadsIndexBuffer().getIndexBuffer().getId(), 0, VK_INDEX_TYPE_UINT16);

        VUtil.UNSAFE.putLong(pBuffers, b ? virtualBufferVtx.bufferPointerSuperSet:virtualBufferVtx2.bufferPointerSuperSet);
//ToDO:Share Layouts
        if(Config.Bindless)
        {
           VUtil.UNSAFE.putLong(pOffsets, 0);
           nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);
        }

        if(b)
        {
            for(final VBO vbo : cutoutChunks)
            {
                vbo.draw();
            }
        }

        else for (int i = translucentChunks.size() - 1; i >= 0; i--) {
            translucentChunks.get(i).draw();
        }


//        p1.end();

        //Need to reset push constant in case the pipeline will still be used for rendering
//        VRenderSystem.setChunkOffset(0, 0, 0);
//        drawer.pushConstants(pipeline);

        this.minecraft.getProfiler().pop();
        renderType.clearRenderState();
        VRenderSystem.applyModelViewMatrix(RenderSystem.getModelViewMatrix());
        VRenderSystem.copyMVP();


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
