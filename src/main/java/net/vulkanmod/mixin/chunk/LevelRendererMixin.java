package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.profiling.Profiler2;
import net.vulkanmod.vulkan.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Final private RenderBuffers renderBuffers;

    @Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
    @Shadow private @Nullable ClientLevel level;

    @Shadow public abstract void graphicsChanged();

    @Shadow private int lastViewDistance;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Set<BlockEntity> globalBlockEntities;
    @Shadow private boolean generateClouds;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;



    @Shadow public abstract void needsUpdate();

    @Shadow public abstract void renderLevel(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f);

    @Unique
    private WorldRenderer worldRenderer;

    @Unique
    private Object2ReferenceOpenHashMap<Class<? extends Entity>, ObjectArrayList<Pair<Entity, MultiBufferSource>>> entitiesMap = new Object2ReferenceOpenHashMap<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.worldRenderer = WorldRenderer.init(this.renderBuffers);
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        this.worldRenderer.setLevel(clientLevel);
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void allChanged(CallbackInfo ci) {
        this.worldRenderer.allChanged();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void renderBlockEntities(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        this.worldRenderer.renderBlockEntities(poseStack, pos.x(), pos.y(), pos.z(), this.destructionProgress, f);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean shouldShowEntityOutlines() {
        return false; //Temp Fix: Disable Glowing due to flickering artifacts with Post effect detection
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        this.worldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);

        entitiesMap.clear();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void compileSections(Camera camera) {
        this.worldRenderer.compileSections(camera);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean isSectionCompiled(BlockPos blockPos) {
        return this.worldRenderer.isSectionCompiled(blockPos);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void renderSectionLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        this.worldRenderer.renderSectionLayer(renderType, poseStack, camX, camY, camZ, projectionMatrix);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void onChunkLoaded(ChunkPos chunkPos) {
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void allChanged() {
        if (this.level != null) {
            this.graphicsChanged();
            this.level.clearTintCaches();

//            this.needsFullRenderChunkUpdate = true;
            this.generateClouds = true;
//            this.recentlyCompiledChunks.clear();
            ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
//         if (this.viewArea != null) {
//            this.viewArea.releaseAllBuffers();
//         }
//
//         this.chunkRenderDispatcher.blockUntilClear();
            synchronized(this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }

        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void setSectionDirty(int x, int y, int z, boolean flag) {
        this.worldRenderer.setSectionDirty(x, y, z, flag);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public String getSectionStatistics() {
        return this.worldRenderer.getChunkStatistics();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean hasRenderedAllSections() {
        return this.worldRenderer.needsUpdate();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public int countRenderedSections() {
        return this.worldRenderer.getVisibleSectionsCount();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"))
    private void pushProfiler2(PoseStack poseStack, Matrix4f matrix4f, float f, double d, double e, double g, CallbackInfo ci) {
        Profiler2 profiler = Profiler2.getMainProfiler();
        profiler.push("clouds");
    }

    @Inject(method = "renderClouds", at = @At("RETURN"))
    private void popProfiler2(PoseStack poseStack, Matrix4f matrix4f, float f, double d, double e, double g, CallbackInfo ci) {
        Profiler2 profiler = Profiler2.getMainProfiler();
        profiler.pop();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"
            , shift = At.Shift.BEFORE))
    private void pushProfiler3(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        Profiler2 profiler = Profiler2.getMainProfiler();
        profiler.push("particles");
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"
            , shift = At.Shift.AFTER))
    private void popProfiler3(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        Profiler2 profiler = Profiler2.getMainProfiler();
        profiler.pop();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void renderEntity(Entity entity, double d, double e, double f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource) {
        if(!Initializer.CONFIG.entityCulling) {
            double h = Mth.lerp((double)g, entity.xOld, entity.getX());
            double i = Mth.lerp((double)g, entity.yOld, entity.getY());
            double j = Mth.lerp((double)g, entity.zOld, entity.getZ());
            float k = Mth.lerp(g, entity.yRotO, entity.getYRot());
            this.entityRenderDispatcher.render(entity, h - d, i - e, j - f, k, g, poseStack, multiBufferSource, this.entityRenderDispatcher.getPackedLightCoords(entity, g));
            return;
        }

        var entityClass = entity.getClass();
        var list = this.entitiesMap.get(entityClass);

        if(list == null) {
            list = new ObjectArrayList<>();
            this.entitiesMap.put(entityClass, list);
        }

        list.add(new Pair<>(entity, multiBufferSource));

    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V",
            shift = At.Shift.AFTER, ordinal = 0)
    )
    private void renderEntities(PoseStack poseStack, float partialTicks, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        if(!Initializer.CONFIG.entityCulling)
            return;

        Vec3 cameraPos = WorldRenderer.getCameraPos();

        for(var list : this.entitiesMap.values()) {
            for(var pair : list) {
                Entity entity = pair.first;
                MultiBufferSource multiBufferSource = pair.second;

                double h = Mth.lerp(partialTicks, entity.xOld, entity.getX());
                double i = Mth.lerp(partialTicks, entity.yOld, entity.getY());
                double j = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
                float k = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
                this.entityRenderDispatcher.render(entity, h - cameraPos.x, i - cameraPos.y, j - cameraPos.z, k, partialTicks, poseStack, multiBufferSource, this.entityRenderDispatcher.getPackedLightCoords(entity, partialTicks));
            }
        }
    }

//    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
//    private Iterable<Entity> replaceIterator(ClientLevel instance) {
//
//        return () -> new Iterator<Entity>() {
//            @Override
//            public boolean hasNext() {
//                return false;
//            }
//
//            @Override
//            public Entity next() {
//                return null;
//            }
//        };
//    }
//
//    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
//            target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;",
//            shift = At.Shift.AFTER),
//            locals = LocalCapture.CAPTURE_FAILHARD
//    )
//    private void renderEntities(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
//        for(Entity entity : this.level.entitiesForRendering()) {
//            if (this.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) {
//                BlockPos blockpos = entity.blockPosition();
//                if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isChunkCompiled(blockpos)) && (entity != p_109604_.getEntity() || p_109604_.isDetached() || p_109604_.getEntity() instanceof LivingEntity && ((LivingEntity)p_109604_.getEntity()).isSleeping()) && (!(entity instanceof LocalPlayer) || p_109604_.getEntity() == entity)) {
//                    ++this.renderedEntities;
//                    if (entity.tickCount == 0) {
//                        entity.xOld = entity.getX();
//                        entity.yOld = entity.getY();
//                        entity.zOld = entity.getZ();
//                    }
//
//                    MultiBufferSource multibuffersource;
//                    if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(entity)) {
//                        flag3 = true;
//                        OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
//                        multibuffersource = outlinebuffersource;
//                        int i = entity.getTeamColor();
//                        int j = 255;
//                        int k = i >> 16 & 255;
//                        int l = i >> 8 & 255;
//                        int i1 = i & 255;
//                        outlinebuffersource.setColor(k, l, i1, 255);
//                    } else {
//                        multibuffersource = bu;
//                    }
//
//                    this.renderEntity(entity, d0, d1, d2, p_109601_, p_109600_, multibuffersource);
//                }
//            }
//        }
//    }

}
