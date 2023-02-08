package net.vulkanmod.render.chunk;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.*;
import net.vulkanmod.render.chunk.util.ChunkQueue;
import net.vulkanmod.render.chunk.util.Util;
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
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Pipeline;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import javax.annotation.Nullable;
import java.util.*;

import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.*;

public class WorldRenderer {
//    public static WorldRenderer INSTANCE;

    public static final Minecraft minecraft;
    private static final long maxGPUMemLimit = Vulkan.memoryProperties.memoryHeaps(0).size();

    public static ClientLevel level;
    public static int lastViewDistance;
    private static final RenderBuffers renderBuffers;

    public static Vec3 cameraPos;
    private static int lastCameraSectionX;
    private static int lastCameraSectionY;
    private static int lastCameraSectionZ;
    private static float lastCameraX;
    private static float lastCameraY;
    private static float lastCameraZ;
    private static float lastCamRotX;
    private static float lastCamRotY;

    public static ChunkGrid chunkGrid;

    private static boolean needsUpdate;
    private static final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();

    public static final TaskDispatcher taskDispatcher;
    private static final ChunkQueue chunkQueue = new ChunkQueue();
    private static int lastFrame = 0;
    private static final ObjectArrayList<QueueChunkInfo> sectionsInFrustum = new ObjectArrayList<>(10000);

    private static double xTransparentOld;
    private static double yTransparentOld;
    private static double zTransparentOld;

//    public ObjectArrayList<RenderSection> solidChunks = new ObjectArrayList<>();
//    public ObjectArrayList<RenderSection> cutoutChunks = new ObjectArrayList<>();
//    public ObjectArrayList<RenderSection> cutoutMippedChunks = new ObjectArrayList<>();
//    public ObjectArrayList<RenderSection> tripwireChunks = new ObjectArrayList<>();
    public static ObjectArrayList<RenderSection> translucentChunks = new ObjectArrayList<>(1024);

    public static double originX;
    public static double originZ;
    public static double curPosX;
    private static double prevCamX;
    public static double curPosZ;
    private static double prevCamZ;
    private static boolean needsReset=true;
    private static int prev;
    private static boolean hasDirty=true;
    private static boolean needsUpdate2=true;
    ;

    static  {
        minecraft = Minecraft.getInstance();
//        levelRenderer = levelRenderer;
        renderBuffers = minecraft.renderBuffers();
        taskDispatcher = new TaskDispatcher(net.minecraft.Util.backgroundExecutor(), renderBuffers.fixedBufferPack());
        ChunkTask.setTaskDispatcher(taskDispatcher);
    }
    //TODO: fake Constructor
    public static void init() {
//        if(INSTANCE != null) throw new RuntimeException("WorldRenderer re-initialization");
//        renderBuffers = renderBuffers_;
    }

    public static void setupRenderer(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        Profiler p = Profiler.getProfiler("chunks");
        p.start();

//        WorldRenderer.frustum = frustum.offsetToFullyIncludeCameraCube(8);
        cameraPos = camera.getPosition();
        if (minecraft.options.getEffectiveRenderDistance() != lastViewDistance) {
            allChanged(minecraft.options.getEffectiveRenderDistance()*minecraft.options.getEffectiveRenderDistance()*24*Config.baseAlignSize);
        }
        if(needsReset) //Move WorldOrigin to the correct position on (Initial) World Load
        {
            needsReset=false;
            prevCamX=cameraPos.x;
            prevCamZ=cameraPos.z;
        }

        level.getProfiler().push("camera");
        float cameraX = (float)cameraPos.x();
        float cameraY = (float)cameraPos.y();
        float cameraZ = (float)cameraPos.z();
        int sectionX = SectionPos.posToSectionCoord(cameraX);
        int sectionY = SectionPos.posToSectionCoord(cameraY);
        int sectionZ = SectionPos.posToSectionCoord(cameraZ);

        if (lastCameraSectionX != sectionX || lastCameraSectionY != sectionY || lastCameraSectionZ != sectionZ) {

            lastCameraSectionX = sectionX;
            lastCameraSectionY = sectionY;
            lastCameraSectionZ = sectionZ;
            chunkGrid.repositionCamera(cameraX, cameraZ);
        }

        double entityDistanceScaling = minecraft.options.entityDistanceScaling().get();
        Entity.setViewScale(Mth.clamp((double) minecraft.options.getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * entityDistanceScaling);

//        this.chunkRenderDispatcher.setCamera(cameraPos);
        level.getProfiler().popPush("cull");
        minecraft.getProfiler().popPush("culling");
        BlockPos blockpos = camera.getBlockPosition();

        minecraft.getProfiler().popPush("update");

        boolean flag = minecraft.smartCull;
        if (spectator && level.getBlockState(blockpos).isSolidRender(level, blockpos)) {
            flag = false;
        }

        float d_xRot = Math.abs(camera.getXRot() - lastCamRotX);
        float d_yRot = Math.abs(camera.getYRot() - lastCamRotY);
        needsUpdate |= d_xRot > 2.0f || d_yRot > 2.0f;

        needsUpdate |= cameraX != lastCameraX || cameraY != lastCameraY || cameraZ != lastCameraZ;

        if (!isCapturedFrustum) {

            if (needsUpdate) {
                chunkGrid.updateFrustumVisibility((((FrustumMixed)(frustum)).customFrustum()).offsetToFullyIncludeCameraCube(8));
                lastCameraX = cameraX;
                lastCameraY = cameraY;
                lastCameraZ = cameraZ;
                lastCamRotX = camera.getXRot();
                lastCamRotY = camera.getYRot();

                p.push("frustum");

                minecraft.getProfiler().push("partial_update");
                Queue<QueueChunkInfo> queue = Queues.newArrayDeque();

                chunkQueue.clear();
                initializeQueueForFullUpdate(camera);

                updateRenderChunks();

                needsUpdate = false;
                minecraft.getProfiler().pop();
            }

            p.pushMilestone("update");
//            p.round();

        }

        minecraft.getProfiler().pop();
    }

    private static void initializeQueueForFullUpdate(Camera camera) {
        int i = 16;
        Vec3 vec3 = camera.getPosition();
        BlockPos blockpos = camera.getBlockPosition();
        RenderSection renderSection = chunkGrid.getRenderChunkAt(blockpos);
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
            QueueChunkInfo chunkInfo = new QueueChunkInfo(renderSection, null, 0);
            chunkQueue.add(chunkInfo);
        }

    }

    private static void updateRenderChunks() {
//        Profiler p = Profiler.getProfiler("chunk setup");
//        p.start();

        int maxDirectionsChanges = Initializer.CONFIG.advCulling;

        //stats
//        int mainLoop = 0;
//        int added = 0;

        sectionsInFrustum.clear();

//        this.solidChunks.clear();
//        this.cutoutChunks.clear();
//        this.cutoutMippedChunks.clear();
//        this.tripwireChunks.clear();
        translucentChunks.clear();


        lastFrame++;
//        p.push("pre-loop");
        while(chunkQueue.hasNext()) {
            QueueChunkInfo renderChunkInfo = chunkQueue.poll();
            RenderSection renderChunk = renderChunkInfo.chunk;

            sectionsInFrustum.add(renderChunkInfo);

            translucentChunks.add(renderChunk);


//            mainLoop++;

            if(renderChunkInfo.directionChanges > maxDirectionsChanges)
                continue;

            //debug
//            BlockPos pos1 = renderChunk.getOrigin();
//            if(pos1.getX() == (-70 << 4) && pos1.getY() == (4 << 4) && pos1.getZ() == (-149 << 4))
//                System.nanoTime();

//         Direction oppositeDir = null;
//         if (renderChunkInfo.hasSourceDirections()) oppositeDir = renderChunkInfo.mainDir.getOpposite();

            for(Direction direction : Util.DIRECTIONS) {
                RenderSection relativeChunk = renderChunk.getNeighbour(direction);

                if (relativeChunk != null && !renderChunkInfo.hasDirection(direction.getOpposite())) {

                    if (renderChunkInfo.hasMainDirection()) {

                        RenderSection.CompiledSection compiledSection = renderChunk.getCompiledSection();

//                  for(int j = 0; j < DIRECTIONS.length; ++j) {
//                     if (renderChunkInfo.hasSourceDirection(j) && compiledSection.facesCanSeeEachother(DIRECTIONS[j].getOpposite(), direction)) {
//                        flag1 = true;
//                        break;
//                     }
//                  }

//                        if(compiledSection.hasNoRenderableLayers()) continue;

                        if (!compiledSection.canSeeThrough(renderChunkInfo.mainDir.getOpposite(), direction)) {
                            continue;
                        }
                    }

                    if (relativeChunk.setLastFrame(lastFrame)) {

                        boolean b = renderChunkInfo.mainDir != direction && !renderChunk.compiledSection.hasNoRenderableLayers();
                        int d = b ? renderChunkInfo.directionChanges + 1 : renderChunkInfo.directionChanges;

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
                        chunkQueue.add(chunkInfo);

                        int d = (renderChunkInfo.sourceDirs & (1 << direction.ordinal())) == 0 && !renderChunk.compiledSection.hasNoRenderableLayers()
                                ? renderChunkInfo.step > 4
                                ? renderChunkInfo.directionChanges + 1 : 0 : renderChunkInfo.directionChanges;
//                        if (renderChunkInfo.mainDir != direction && !renderChunk.compiledSection.hasNoRenderableLayers())
//                        if (renderChunkInfo.mainDir != null && renderChunkInfo.mainDir.ordinal() != direction.ordinal() && !renderChunk.compiledSection.hasNoRenderableLayers())
                        //                            d = renderChunkInfo.directionChanges + 1;

                        chunkInfo.setDirectionChanges(d);
                        relativeChunk.queueInfo = chunkInfo;
                    }

                }
            }
        }

//        p.round();
//        mainLoop++;
    }

    public static void compileChunks(Camera camera) {
        if(TaskDispatcher.resetting) return;

        minecraft.getProfiler().push("populate_chunks_to_compile");
        RenderRegionCache renderregioncache = new RenderRegionCache();
//        BlockPos cameraPos = camera.getBlockPosition();
//        List<RenderSection> list = Lists.newArrayList();


        RHandler.uniqueVBOs.clear();
        RHandler.translucentVBOs.clear();
        hasDirty=false;

        //TODO later: find a better way
        for(QueueChunkInfo chunkInfo : sectionsInFrustum) {
            RenderSection renderSection = chunkInfo.chunk;
            if (renderSection.isDirty()) {
                hasDirty=true;
                if (((LevelChunk) (level.getChunk((renderSection.getOrigin())))).isClientLightReady()) {
//                RHandler.dirtyVBOs.add(renderSection.vbo);

                    renderSection.rebuildChunkAsync(taskDispatcher, renderregioncache);
                    renderSection.setNotDirty();
                }
            }
            //based on the 1.18.2 applyfrustum Function: Hence why performance is likely bad
            //Could use GPU-based culling in tandem with draw-indirect, which would require a Compute Shader, which apparently isn't particularly hard or difficult to do
            if (!renderSection.vbo.preInitialised) {
                if(!renderSection.vbo.translucentAlphaBlending) {
                    RHandler.uniqueVBOs.add(renderSection.vbo);
                }
                else RHandler.translucentVBOs.add(renderSection.vbo);
            }
        }

        minecraft.getProfiler().popPush("upload");
        if(!TaskDispatcher.resetting) taskDispatcher.uploadAllPendingUploads();
//        CompletableFuture.runAsync(() -> this.taskDispatcher.uploadAllPendingUploads());
        minecraft.getProfiler().popPush("schedule_async_compile");

        //debug
        /*Profiler p = null;
        if(!list.isEmpty()) {
            p = Profiler.getProfiler("compileChunks");
            p.start();
        }*/


        minecraft.getProfiler().pop();

       /* if(!list.isEmpty()) {
            p.round();
        }*/

    }

    public static boolean isChunkCompiled(BlockPos blockPos) {
        RenderSection renderSection = chunkGrid.getRenderChunkAt(blockPos);
        return renderSection != null && renderSection.compiledSection != RenderSection.CompiledSection.UNCOMPILED;
    }

    public static void allChanged(int size ) {
        TaskDispatcher.resetting=true;
        resetOrigin();
        lastViewDistance = minecraft.options.getEffectiveRenderDistance();

        resetAllBuffers(size);

        if (level != null) {
//            this.graphicsChanged();
            level.clearTintCaches();
//            if (this.taskDispatcher == null) {
//                this.taskDispatcher = new TaskDispatcher(Util.backgroundExecutor(), this.renderBuffers.fixedBufferPack());
//            } else {
//                this.taskDispatcher.setLevel(this.level);
//            }

            needsUpdate = true;
//            generateClouds = true;
//            recentlyCompiledChunks.clear();
//            ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());

            if (chunkGrid != null) {
                chunkGrid.releaseAllBuffers();
            }
            taskDispatcher.clearBatchQueue();
            synchronized(globalBlockEntities) {
                globalBlockEntities.clear();
            }

            chunkGrid = new ChunkGrid(level, minecraft.options.getEffectiveRenderDistance());

            sectionsInFrustum.clear();
            Entity entity = minecraft.getCameraEntity();
            if (entity != null) {
                chunkGrid.repositionCamera(entity.getX(), entity.getZ());
            }

        }
//        TaskDispatcher.resetting=false;
    }

    public static void resetAllBuffers(int size) {
        TaskDispatcher.resetting=true;
        Drawer.skipRendering=true;
        RHandler.uniqueVBOs.clear();
        RHandler.translucentVBOs.clear();
//        nvkWaitForFences(Vulkan.getDevice(), Drawer.inFlightFences.capacity(), Drawer.inFlightFences.address0(), 1, -1);
        vkDeviceWaitIdle(Vulkan.getDevice()); //Use a heavier Wait to avoid potential crashes
//        if(size> maxGPUMemLimit) size= (int) maxGPUMemLimit;
        RHandler.virtualBuffer.reset(size);
        RHandler.virtualBufferIdx.reset(size/8);
        TaskDispatcher.resetting=false;
        Drawer.skipRendering=false;

    }

    private static void resetOrigin() {
        originX=0;
        originZ=0;
    }

    public static void setLevel(@Nullable ClientLevel level_) {
        lastCameraX = Float.MIN_VALUE;
        lastCameraY = Float.MIN_VALUE;
        lastCameraZ = Float.MIN_VALUE;
        lastCameraSectionX = Integer.MIN_VALUE;
        lastCameraSectionY = Integer.MIN_VALUE;
        lastCameraSectionZ = Integer.MIN_VALUE;
//        this.entityRenderDispatcher.setLevel(level);
        level = level_;TaskDispatcher.resetting=true;
        if (level != null) {

            if(chunkGrid == null) {
                allChanged(0);
            }
        } else {
            if (chunkGrid != null) {
                chunkGrid.releaseAllBuffers();
                chunkGrid = null;
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
            sectionsInFrustum.clear();
        }
//        TaskDispatcher.resetting=false;

    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> p_109763_, Collection<BlockEntity> p_109764_) {
        synchronized(globalBlockEntities) {
            globalBlockEntities.removeAll(p_109763_);
            globalBlockEntities.addAll(p_109764_);
        }
    }

    public static void renderChunkLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projection) {


//        //debug
//        Profiler p = Profiler.getProfiler("chunks");
//        RenderType solid = RenderType.solid();
//        RenderType cutout = RenderType.cutout();
//        RenderType cutoutMipped = RenderType.cutoutMipped();
//        RenderType translucent = RenderType.translucent();
//        RenderType tripwire = RenderType.tripwire();
//
//        String layerName;
//        if (solid.equals(renderType)) {
//            layerName = "solid";
//        } else if (cutout.equals(renderType)) {
//            layerName = "cutout";
//        } else if (cutoutMipped.equals(renderType)) {
//            layerName = "cutoutMipped";
//        } else if (tripwire.equals(renderType)) {
//            layerName = "tripwire";
//        } else if (translucent.equals(renderType)) {
//            layerName = "translucent";
//        } else layerName = "unk";
//
//        p.pushMilestone("layer " + layerName);

        RHandler.camX=camX;
        RHandler.camY=camY;
        RHandler.camZ=camZ;
        RenderSystem.assertOnRenderThread();
        renderType.setupRenderState();
        translucentSort(renderType, camX, camY, camZ); //may be better to place translucent textures ina separate renderpass and submit it together with the vbos
        if (Config.drawIndirect)
            if (RHandler.uniqueVBOs.size() != prev || needsUpdate2) {
                needsUpdate2=false;
                prev = RHandler.uniqueVBOs.size();
                RHandler.drawCommands.clear();

                for (int i = 0; i < RHandler.uniqueVBOs.size(); i++) {

                    RHandler.drawCommands.put(RHandler.uniqueVBOs.get(i).indirectCommand);

                }
                for(int x =RHandler.translucentVBOs.size()-1; x>=0;x--)
                {
                    RHandler.drawCommands.put(RHandler.translucentVBOs.get(x).indirectCommand);
                }
                RHandler.AllocIndirectCmds();
            }
        minecraft.getProfiler().push("filterempty");
        minecraft.getProfiler().popPush(() -> {
            return "render_" + renderType;
        });
        boolean flag = renderType != RenderType.translucent();

//        if (RenderType.translucent().equals(renderType)) {
//        }
//        else {
//            sections = ObjectArrayList.of();
//        }

//        ObjectListIterator<WorldRenderer.QueueChunkInfo> iterator = this.sectionsInFrustum.listIterator(flag ? 0 : this.sectionsInFrustum.size());
//        ObjectListIterator<RenderSection> iterator = sections.listIterator(flag ? 0 : sections.size());

        VertexFormat vertexformat = renderType.format();

        Matrix4f pose = poseStack.last().pose();


        originX+= (prevCamX - camX);;
        originZ+= (prevCamZ - camZ);;
        {

            pose.m03 += pose.m00* (originX) + pose.m01* -camY +pose.m02 * originZ;
            pose.m13 += pose.m10* (originX) + pose.m11* -camY +pose.m12 * originZ;
            pose.m23 += pose.m20* (originX) + pose.m21* -camY +pose.m22 * originZ;
            pose.m33 += 0;
        }
        prevCamX=camX;
        prevCamZ=camZ;
        VRenderSystem.applyMVP(pose, projection);


//        Drawer drawer = Drawer.getInstance();
        ShaderInstance rendertypeCutoutMippedShader = Config.noFog ? GameRenderer.getRendertypeCutoutMippedShader() : GameRenderer.getRendertypeTripwireShader();
        Pipeline pipeline = ((ShaderMixed) rendertypeCutoutMippedShader).getPipeline();
        Drawer.bindPipeline(pipeline);

        Drawer.uploadAndBindUBOs(pipeline);

//        Supplier<Boolean> checker = flag ? iterator::hasNext : iterator::hasPrevious;
//        Supplier<RenderSection> getter = flag ? iterator::next : iterator::previous;

        try(MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = Drawer.commandBuffers.get(Drawer.getCurrentFrame());

            long vertexBuffers = stack.npointer(RHandler.virtualBuffer.bufferPointerSuperSet);
            long offsets = stack.npointer(0);
//            Profiler.Push("bindVertex");
            VK10.nvkCmdBindVertexBuffers(commandBuffer, 0, 1, vertexBuffers, offsets);
            vkCmdBindIndexBuffer(commandBuffer, RHandler.virtualBufferIdx.bufferPointerSuperSet, 0, VK_INDEX_TYPE_UINT16);
        }
//        RHandler.uniqueVBOs.unstableSort(null);

//        RHandler.translucentVBOs.clear();
//        int t=0;
//        int size = Math.max(RHandler.uniqueVBOs.size() - 1, 0);
//        for (int i = 0; i < RHandler.uniqueVBOs.size(); i++) {
//            VBO vbos = RHandler.uniqueVBOs.get(i);
//            if (vbos.translucent) {
////                translucentVBOs.add(vbos);
//                RHandler.uniqueVBOs.remove(vbos);
//                RHandler.translucentVBOs.add(vbos);
//            }
//        }
//        RHandler.uniqueVBOs.addAll( Math.max(RHandler.uniqueVBOs.size() - 1, 0), RHandler.translucentVBOs);
        if(!Config.drawIndirect) {
            for (int i = 0; i < RHandler.uniqueVBOs.size(); i++) {

                Drawer.drawIndexedBindless(RHandler.uniqueVBOs.get(i).indirectCommand);
            }
            for (int i = RHandler.translucentVBOs.size() - 1; i >= 0; i--) {

                Drawer.drawIndexedBindless(RHandler.translucentVBOs.get(i).indirectCommand);
            }
        }
        else Drawer.drawIndexedBindlessIndirect();


//
        //Need to reset push constant in case the pipeline will still be used for rendering
//        VRenderSystem.setChunkOffset(0, 0, 0);
//        drawer.pushConstants(pipeline);

        minecraft.getProfiler().pop();
        renderType.clearRenderState();

        VRenderSystem.copyMVP(RenderSystem.getModelViewMatrix());

    }

    private static void translucentSort(RenderType renderType, double camX, double camY, double camZ) {
       {
            minecraft.getProfiler().push("translucent_sort");
            double d0 = camX - xTransparentOld;
            double d1 = camY - yTransparentOld;
            double d2 = camZ - zTransparentOld;
            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D) {
                xTransparentOld = camX;
                yTransparentOld = camY;
                zTransparentOld = camZ;
                int j = 0;

                for(QueueChunkInfo chunkInfo : sectionsInFrustum) {
                    if (j < 15 && chunkInfo.chunk.resortTransparency(renderType, taskDispatcher)) {
                        ++j;
                    }
                }
            }

            minecraft.getProfiler().pop();
        }
    }

    public static void renderBlockEntities(PoseStack poseStack, double camX, double camY, double camZ,
                                    Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress, float gameTime) {
        MultiBufferSource bufferSource = renderBuffers.bufferSource();

        for(QueueChunkInfo info : sectionsInFrustum) {
            List<BlockEntity> list = info.chunk.getCompiledSection().getRenderableBlockEntities();
            if (!list.isEmpty()) {
                for(BlockEntity blockentity1 : list) {
                    BlockPos blockpos4 = blockentity1.getBlockPos();
                    MultiBufferSource multibuffersource1 = bufferSource;
                    poseStack.pushPose();
                    poseStack.translate(blockpos4.getX() - camX, blockpos4.getY() - camY, blockpos4.getZ() - camZ);
                    SortedSet<BlockDestructionProgress> sortedset = destructionProgress.get(blockpos4.asLong());
                    if (sortedset != null && !sortedset.isEmpty()) {
                        int j1 = sortedset.last().getProgress();
                        if (j1 >= 0) {
                            PoseStack.Pose posestack$pose1 = poseStack.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(j1)), posestack$pose1.pose(), posestack$pose1.normal());
                            multibuffersource1 = (p_194349_) -> {
                                VertexConsumer vertexconsumer3 = bufferSource.getBuffer(p_194349_);
                                return p_194349_.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                            };
                        }
                    }

                    minecraft.getBlockEntityRenderDispatcher().render(blockentity1, gameTime, poseStack, multibuffersource1);
                    poseStack.popPose();
                }
            }
        }
    }

    public static void setNeedsUpdate() {
        needsUpdate = true;
        needsUpdate2 = true;
    }

    public static void setSectionDirty(int x, int y, int z, boolean flag) {
        chunkGrid.setDirty(x, y, z, flag);
    }

    public static String getChunkStatistics() {
        int i = chunkGrid.chunks.length;
        int j = sectionsInFrustum.size();
        return String.format("Chunks: %d/%d D: %d, %s", j, i, lastViewDistance, taskDispatcher == null ? "null" : taskDispatcher.getStats());
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
