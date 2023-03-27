package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.SkyBoxVBO;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.util.VBOUtil;
import net.vulkanmod.vulkan.Drawer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Shadow private static BufferBuilder.RenderedBuffer buildSkyDisc(BufferBuilder bufferBuilder, float f) { return null; }

    @Shadow protected abstract BufferBuilder.RenderedBuffer drawStars(BufferBuilder bufferBuilder);

    private WorldRenderer worldRenderer;

    private static final SkyBoxVBO starBuffer = new SkyBoxVBO();
    private static final SkyBoxVBO skyBuffer = new SkyBoxVBO();
    private static final SkyBoxVBO darkBuffer = new SkyBoxVBO();

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
    private void setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        this.worldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void compileChunks(Camera camera) {
        this.worldRenderer.compileChunks(camera);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean isChunkCompiled(BlockPos blockPos) {
        return this.worldRenderer.isChunkCompiled(blockPos);
    }

    @Redirect(method = "renderLevel", at=@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))
    private void clear(int i, boolean bl)
    {

    }

    @Inject(method = "renderLevel", at=@At(value="INVOKE", ordinal =0, target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V"))
    private void renderChunkLayer(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci)
    {
        Vec3 vec3 = camera.getPosition();
        double d = vec3.x();
        double e = vec3.y();
        double g = vec3.z();
        VBOUtil.updateCamTranslation(poseStack, d, e, g, matrix4f);
    }
    /**
     * @author
     * @reason
     */
    @Overwrite
    private void renderChunkLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        this.worldRenderer.renderChunkLayer(renderType, camX, camY, camZ, projectionMatrix);
    }

    //Avoid NullPointer when calling VertexBuffer.bind()
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;bind()V"))
    private void bindSkyBoxVBO(VertexBuffer instance)
    {

    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void createDarkSky() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();


        BufferBuilder.RenderedBuffer renderedBuffer = buildSkyDisc(bufferBuilder, -16.0F);

        darkBuffer.upload_(renderedBuffer);
        VertexBuffer.unbind();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void createLightSky() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        BufferBuilder.RenderedBuffer renderedBuffer = buildSkyDisc(bufferBuilder, 16.0F);

        skyBuffer.upload_(renderedBuffer);
        VertexBuffer.unbind();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void createStars() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);


        BufferBuilder.RenderedBuffer renderedBuffer = this.drawStars(bufferBuilder);

        starBuffer.upload_(renderedBuffer);
        VertexBuffer.unbind();
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

    @Redirect(method = "renderSky", at=@At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"))
    private void drawWithShaderSkyBuffer(VertexBuffer instance, Matrix4f matrix4f, Matrix4f matrix4f2, ShaderInstance shaderInstance)
    {
        skyBuffer._drawWithShader(matrix4f, matrix4f2, shaderInstance);
    }

    @Redirect(method = "renderSky", at=@At(value = "INVOKE", ordinal = 1, target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"))
    private void drawWithShaderStarBuffer(VertexBuffer instance, Matrix4f matrix4f, Matrix4f matrix4f2, ShaderInstance shaderInstance)
    {
        starBuffer._drawWithShader(matrix4f, matrix4f2, shaderInstance);
    }

    @Redirect(method = "renderSky", at=@At(value = "INVOKE", ordinal = 2, target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"))
    private void drawWithShaderDarkBuffer(VertexBuffer instance, Matrix4f matrix4f, Matrix4f matrix4f2, ShaderInstance shaderInstance)
    {
        darkBuffer._drawWithShader(matrix4f, matrix4f2, shaderInstance);
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
    public String getChunkStatistics() {
        return this.worldRenderer.getChunkStatistics();
    }
}
