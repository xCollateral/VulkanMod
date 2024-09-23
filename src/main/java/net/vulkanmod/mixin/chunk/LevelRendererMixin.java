package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Final
    private RenderBuffers renderBuffers;

    @Shadow @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    private WorldRenderer worldRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.worldRenderer = WorldRenderer.init(this.renderBuffers);
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        this.worldRenderer.setLevel(clientLevel);
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onAllChanged(CallbackInfo ci) {
        this.worldRenderer.allChanged();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void renderBlockEntities(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        PoseStack poseStack = new PoseStack();

        this.worldRenderer.renderBlockEntities(poseStack, pos.x(), pos.y(), pos.z(), this.destructionProgress, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        this.worldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);
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
    private void renderSectionLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projectionMatrix) {
        this.worldRenderer.renderSectionLayer(renderType, camX, camY, camZ, modelView, projectionMatrix);
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
        return !this.worldRenderer.graphNeedsUpdate() && this.worldRenderer.getTaskDispatcher().isIdle();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public int countRenderedSections() {
        return this.worldRenderer.getVisibleSectionsCount();
    }

    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getDepthFar()F"))
    private float getRenderDistanceZFar(GameRenderer instance) {
        return instance.getRenderDistance() * 4F;
    }

}
