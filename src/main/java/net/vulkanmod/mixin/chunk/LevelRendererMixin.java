package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.config.Config;
import net.vulkanmod.render.SkyBoxVBO;
import net.vulkanmod.render.chunk.TaskDispatcher;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

@Mixin(value = LevelRenderer.class, priority = 999)
public abstract class LevelRendererMixin {


    @Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
    private static final SkyBoxVBO darkBuffer = new SkyBoxVBO();

    @Shadow
    private static BufferBuilder.RenderedBuffer buildSkyDisc(BufferBuilder bufferBuilder, float f) { return null; }

    private static final SkyBoxVBO skyBuffer = new SkyBoxVBO();
    private static final SkyBoxVBO starBuffer = new SkyBoxVBO();

    /** @author /*** @reason */

    @Overwrite BufferBuilder.RenderedBuffer drawStars(BufferBuilder bufferBuilder) {return null;}

    @Shadow protected abstract boolean doesMobEffectBlockSky(Camera camera);

    @Shadow protected abstract void renderEndSky(PoseStack poseStack);

    @Shadow @Final private static ResourceLocation SUN_LOCATION;
    @Shadow @Final private static ResourceLocation MOON_LOCATION;

    @Shadow protected abstract void deinitTransparency();

//    private WorldRenderer worldRenderer;

    @Shadow private @Nullable ClientLevel level;
    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow private @Nullable Frustum capturedFrustum;
    @Shadow @Final private Vector3d frustumPos;
    @Shadow private Frustum cullingFrustum;
    @Shadow private boolean captureFrustum;

    @Shadow protected abstract void captureFrustum(Matrix4f matrix4f, Matrix4f matrix4f2, double d, double e, double f, Frustum frustum);

    @Shadow private int renderedEntities;
    @Shadow private int culledEntities;
    @Shadow private @Nullable RenderTarget itemEntityTarget;
    @Shadow private @Nullable RenderTarget weatherTarget;

    @Shadow protected abstract boolean shouldShowEntityOutlines();

    @Shadow private @Nullable RenderTarget entityTarget;
    @Shadow @Final private RenderBuffers renderBuffers;

    @Shadow protected abstract void checkPoseStack(PoseStack poseStack);

    @Shadow @Final private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
    @Shadow @Final private Set<BlockEntity> globalBlockEntities;
    @Shadow private @Nullable PostChain entityEffect;

    @Shadow protected abstract void renderHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState);

    @Shadow private @Nullable PostChain transparencyChain;
    @Shadow private @Nullable RenderTarget translucentTarget;
    @Shadow private @Nullable RenderTarget particlesTarget;
    @Shadow private @Nullable RenderTarget cloudsTarget;

    @Shadow public abstract void renderClouds(PoseStack poseStack, Matrix4f matrix4f, float f, double d, double e, double g);

    @Shadow protected abstract void renderSnowAndRain(LightTexture lightTexture, float f, double d, double e, double g);

    @Shadow protected abstract void renderWorldBorder(Camera camera);

    @Shadow protected abstract void renderDebug(Camera camera);

    @Shadow protected abstract void renderEntity(Entity entity, double d, double e, double f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        WorldRenderer.init();
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        WorldRenderer.setLevel(clientLevel);
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void allChanged(CallbackInfo ci) {
        WorldRenderer.allChanged(0);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void renderBlockEntities(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        WorldRenderer.renderBlockEntities(poseStack, pos.x(), pos.y(), pos.z(), this.destructionProgress, f);
    }

    static
    {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();


        darkBuffer.upload_(buildSkyDisc(bufferBuilder, -16.0F));


        skyBuffer.upload_(buildSkyDisc(bufferBuilder, 16.0F));

        starBuffer.upload_(drawStars2(bufferBuilder));
//        VertexBuffer.unbind();

    }
    //TODO: Static version of drawStars()
    private static BufferBuilder.RenderedBuffer drawStars2(BufferBuilder bufferBuilder) {
        RandomSource randomSource = RandomSource.create(10842L);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for(int i = 0; i < 1500; ++i) {
            double d = randomSource.nextFloat() * 2.0F - 1.0F;
            double e = randomSource.nextFloat() * 2.0F - 1.0F;
            double f = randomSource.nextFloat() * 2.0F - 1.0F;
            double g = 0.15F + randomSource.nextFloat() * 0.1F;
            double h = d * d + e * e + f * f;
            if (h < 1.0 && h > 0.01) {
                h = 1.0 / Math.sqrt(h);
                d *= h;
                e *= h;
                f *= h;
                double j = d * 100.0;
                double k = e * 100.0;
                double l = f * 100.0;
                double m = Math.atan2(d, f);
                double n = Math.sin(m);
                double o = Math.cos(m);
                double p = Math.atan2(Math.sqrt(d * d + f * f), e);
                double q = Math.sin(p);
                double r = Math.cos(p);
                double s = randomSource.nextDouble() * Math.PI * 2.0;
                double t = Math.sin(s);
                double u = Math.cos(s);

                for(int v = 0; v < 4; ++v) {
                    double w = 0.0;
                    double x = (double)((v & 2) - 1) * g;
                    double y = (double)((v + 1 & 2) - 1) * g;
                    double z = 0.0;
                    double aa = x * u - y * t;
                    double ab = y * u + x * t;
                    double ad = aa * q + 0.0 * r;
                    double ae = 0.0 * q - aa * r;
                    double af = ae * n - ab * o;
                    double ah = ab * n + ae * o;
                    bufferBuilder.vertex(j + af, k + ad, l + ah).endVertex();
                }
            }
        }

        return bufferBuilder.end();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite private void createDarkSky() {}

    /**
     * @author
     * @reason
     */
    @Overwrite private void createLightSky() {}

    /**
     * @author
     * @reason
     */
    @Overwrite private void createStars() {}

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void renderSky(PoseStack poseStack, Matrix4f matrix4f, float f, Camera camera, boolean bl, Runnable runnable) {
        runnable.run();
        if (!bl) {
            FogType fogType = camera.getFluidInCamera();
            if (fogType != FogType.POWDER_SNOW && fogType != FogType.LAVA && !this.doesMobEffectBlockSky(camera)) {
                switch (WorldRenderer.level.effects().skyType()) {
                    case END -> this.renderEndSky(poseStack);
                    case NORMAL -> {
                        RenderSystem.disableTexture();
                        Vec3 vec3 = WorldRenderer.level.getSkyColor(WorldRenderer.minecraft.gameRenderer.getMainCamera().getPosition(), f);
                        float g = (float) vec3.x;
                        float h = (float) vec3.y;
                        float i = (float) vec3.z;
                        FogRenderer.levelFogColor();
                        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                        VRenderSystem.depthMask(false);
                        RenderSystem.setShaderColor(g, h, i, 1.0F);
                        ShaderInstance shaderInstance = RenderSystem.getShader();
                        skyBuffer._drawWithShader(poseStack.last().pose(), matrix4f, shaderInstance);
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        float[] fs = WorldRenderer.level.effects().getSunriseColor(WorldRenderer.level.getTimeOfDay(f), f);
                        float j;
                        float l;
                        float p;
                        float q;
                        float r;
                        if (fs != null) {
                            RenderSystem.setShader(GameRenderer::getPositionColorShader);
                            RenderSystem.disableTexture();
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                            poseStack.pushPose();
                            poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
                            j = Mth.sin(WorldRenderer.level.getSunAngle(f)) < 0.0F ? 180.0F : 0.0F;
                            poseStack.mulPose(Vector3f.ZP.rotationDegrees(j));
                            poseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
                            float k = fs[0];
                            l = fs[1];
                            float m = fs[2];
                            Matrix4f matrix4f2 = poseStack.last().pose();
                            bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                            bufferBuilder.vertex(matrix4f2, 0.0F, 100.0F, 0.0F).color(k, l, m, fs[3]).endVertex();

                            for (int o = 0; o <= 16; ++o) {
                                p = (float) o * 6.2831855F / 16.0F;
                                q = Mth.sin(p);
                                r = Mth.cos(p);
                                bufferBuilder.vertex(matrix4f2, q * 120.0F, r * 120.0F, -r * 40.0F * fs[3]).color(fs[0], fs[1], fs[2], 0.0F).endVertex();
                            }

                            BufferUploader.drawWithShader(bufferBuilder.end());
                            poseStack.popPose();
                        }
                        RenderSystem.enableTexture();
                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                        poseStack.pushPose();
                        j = 1.0F - WorldRenderer.level.getRainLevel(f);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, j);
                        poseStack.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
                        poseStack.mulPose(Vector3f.XP.rotationDegrees(WorldRenderer.level.getTimeOfDay(f) * 360.0F));
                        Matrix4f matrix4f3 = poseStack.last().pose();
                        l = 30.0F;
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);
                        RenderSystem.setShaderTexture(0, SUN_LOCATION);
                        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                        bufferBuilder.vertex(matrix4f3, -l, 100.0F, -l).uv(0.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f3, l, 100.0F, -l).uv(1.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f3, l, 100.0F, l).uv(1.0F, 1.0F).endVertex();
                        bufferBuilder.vertex(matrix4f3, -l, 100.0F, l).uv(0.0F, 1.0F).endVertex();
                        BufferUploader.drawWithShader(bufferBuilder.end());
                        l = 20.0F;
                        RenderSystem.setShaderTexture(0, MOON_LOCATION);
                        int s = WorldRenderer.level.getMoonPhase();
                        int t = s % 4;
                        int n = s / 4 % 2;
                        float u = (float) (t) / 4.0F;
                        p = (float) (n) / 2.0F;
                        q = (float) (t + 1) / 4.0F;
                        r = (float) (n + 1) / 2.0F;
                        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                        bufferBuilder.vertex(matrix4f3, -l, -100.0F, l).uv(q, r).endVertex();
                        bufferBuilder.vertex(matrix4f3, l, -100.0F, l).uv(u, r).endVertex();
                        bufferBuilder.vertex(matrix4f3, l, -100.0F, -l).uv(u, p).endVertex();
                        bufferBuilder.vertex(matrix4f3, -l, -100.0F, -l).uv(q, p).endVertex();
                        BufferUploader.drawWithShader(bufferBuilder.end());
                        RenderSystem.disableTexture();
                        float v = WorldRenderer.level.getStarBrightness(f) * j;
                        if (v > 0.0F) {
                            RenderSystem.setShaderColor(v, v, v, v);
                            FogRenderer.setupNoFog();

                            starBuffer._drawWithShader(poseStack.last().pose(), matrix4f, GameRenderer.getPositionShader());

                            runnable.run();
                        }
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.disableBlend();
                        poseStack.popPose();
                        RenderSystem.disableTexture();
                        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
                        double d = WorldRenderer.minecraft.player.getEyePosition(f).y - WorldRenderer.level.getLevelData().getHorizonHeight(WorldRenderer.level);
                        if (d < 0.0) {
                            poseStack.pushPose();
                            poseStack.translate(0.0, 12.0, 0.0);

                            darkBuffer._drawWithShader(poseStack.last().pose(), matrix4f, shaderInstance);

                            poseStack.popPose();
                        }
                        if (WorldRenderer.level.effects().hasGround()) {
                            RenderSystem.setShaderColor(g * 0.2F + 0.04F, h * 0.2F + 0.04F, i * 0.6F + 0.1F, 1.0F);
                        } else {
                            RenderSystem.setShaderColor(g, h, i, 1.0F);
                        }
                        RenderSystem.enableTexture();
                        VRenderSystem.depthMask(true);
                    }
                }
            }
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        WorldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void compileChunks(Camera camera) {
        if(!TaskDispatcher.resetting)  WorldRenderer.compileChunks(camera);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean isChunkCompiled(BlockPos blockPos) {
        return WorldRenderer.isChunkCompiled(blockPos);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void renderChunkLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
      /*  deinitTransparency();
        WorldRenderer.renderChunkLayer(renderType, poseStack, camX, camY, camZ, projectionMatrix);*/
    }
    @Inject(method = "renderLevel", at=@At(value="RETURN"))
    private void renderChunkLayer(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
//        deinitTransparency();
//        WorldRenderer.renderChunkLayer(RenderType.translucent(), poseStack, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z, matrix4f);
    }

    @Overwrite
    public void renderLevel(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f) {
        RenderSystem.setShaderGameTime(this.level.getGameTime(), f);
        this.blockEntityRenderDispatcher.prepare(this.level, camera, this.minecraft.hitResult);
        this.entityRenderDispatcher.prepare(this.level, camera, this.minecraft.crosshairPickEntity);
        ProfilerFiller profilerFiller = this.level.getProfiler();
        profilerFiller.popPush("light_update_queue");
        this.level.pollLightUpdates();
        profilerFiller.popPush("light_updates");
        boolean bl2 = this.level.isLightUpdateQueueEmpty();
        this.level.getChunkSource().getLightEngine().runUpdates(Integer.MAX_VALUE, bl2, true);
        final Vec3 vec3 = camera.getPosition();
        final double d = vec3.x();
        final double e = vec3.y();
        final double g = vec3.z();
        Matrix4f matrix4f2 = poseStack.last().pose();
        profilerFiller.popPush("culling");
        boolean bl3 = this.capturedFrustum != null;
        Frustum frustum;
        if (bl3) {
            frustum = this.capturedFrustum;
            frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
        } else {
            frustum = this.cullingFrustum;
        }

        this.minecraft.getProfiler().popPush("captureFrustum");
        if (this.captureFrustum) {
            this.captureFrustum(matrix4f2, matrix4f, vec3.x, vec3.y, vec3.z, bl3 ? new Frustum(matrix4f2, matrix4f) : frustum);
            this.captureFrustum = false;
        }

        profilerFiller.popPush("clear");
        FogRenderer.setupColor(camera, f, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), gameRenderer.getDarkenWorldAmount(f));
        FogRenderer.levelFogColor();
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        float h = gameRenderer.getRenderDistance();
        boolean bl4 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d), Mth.floor(e)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        profilerFiller.popPush("sky");
        RenderSystem.setShader(GameRenderer::getPositionShader);
        this.renderSky(poseStack, matrix4f, f, camera, bl4, () -> {
            FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, h, bl4, f);
        });
        if(!Config.noFog) {
            profilerFiller.popPush("fog");
            FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(h, 32.0F), bl4, f);
        }
        profilerFiller.popPush("terrain_setup");
        this.setupRender(camera, frustum, bl3, this.minecraft.player.isSpectator());
        profilerFiller.popPush("compilechunks");
        this.compileChunks(camera);
        profilerFiller.popPush("terrain");
        this.renderChunkLayer(RenderType.solid(), poseStack, d, e, g, matrix4f);
        this.renderChunkLayer(RenderType.cutoutMipped(), poseStack, d, e, g, matrix4f);
        this.renderChunkLayer(RenderType.cutout(), poseStack, d, e, g, matrix4f);
        if (this.level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel(poseStack.last().pose());
        } else {
            Lighting.setupLevel(poseStack.last().pose());
        }

        profilerFiller.popPush("entities");
        this.renderedEntities = 0;
        this.culledEntities = 0;
        if (this.itemEntityTarget != null) {
            this.itemEntityTarget.clear(Minecraft.ON_OSX);
            this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        if (this.weatherTarget != null) {
            this.weatherTarget.clear(Minecraft.ON_OSX);
        }

        if (this.shouldShowEntityOutlines()) {
            this.entityTarget.clear(Minecraft.ON_OSX);
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        boolean bl5 = false;
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
        Iterator<Entity> var26 = this.level.entitiesForRendering().iterator();

        while (true) {
            Entity entity;
            int m;
            do {
                BlockPos blockPos;
                do {
                    do {
                        do {
                            if (!var26.hasNext()) {
                                bufferSource.endLastBatch();
                                this.checkPoseStack(poseStack);
                                bufferSource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
                                bufferSource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
                                bufferSource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                                bufferSource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
                                profilerFiller.popPush("blockentities");
                                ObjectListIterator<LevelRenderer.RenderChunkInfo> var40 = this.renderChunksInFrustum.iterator();

                                while (true) {
                                    List<BlockEntity> list;
                                    do {
                                        if (!var40.hasNext()) {
                                            synchronized (this.globalBlockEntities) {

                                                for (BlockEntity blockEntity2 : this.globalBlockEntities) {
                                                    BlockPos blockPos3 = blockEntity2.getBlockPos();
                                                    poseStack.pushPose();
                                                    poseStack.translate(blockPos3.getX() - d, blockPos3.getY() - e, blockPos3.getZ() - g);
                                                    this.blockEntityRenderDispatcher.render(blockEntity2, f, poseStack, bufferSource);
                                                    poseStack.popPose();
                                                }
                                            }

                                            this.checkPoseStack(poseStack);
                                            bufferSource.endBatch(RenderType.solid());
                                            bufferSource.endBatch(RenderType.endPortal());
                                            bufferSource.endBatch(RenderType.endGateway());
                                            bufferSource.endBatch(Sheets.solidBlockSheet());
                                            bufferSource.endBatch(Sheets.cutoutBlockSheet());
                                            bufferSource.endBatch(Sheets.bedSheet());
                                            bufferSource.endBatch(Sheets.shulkerBoxSheet());
                                            bufferSource.endBatch(Sheets.signSheet());
                                            bufferSource.endBatch(Sheets.chestSheet());
                                            this.renderBuffers.outlineBufferSource().endOutlineBatch();
                                            if (bl5) {
                                                this.entityEffect.process(f);
                                                this.minecraft.getMainRenderTarget().bindWrite(false);
                                            }

                                            profilerFiller.popPush("destroyProgress");

                                            for (Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> sortedSetEntry : this.destructionProgress.long2ObjectEntrySet()) {
                                                blockPos = BlockPos.of(sortedSetEntry.getLongKey());
                                                double o = blockPos.getX() - d;
                                                double p = blockPos.getY() - e;
                                                double q = blockPos.getZ() - g;
                                                if (!(o * o + p * p + q * q > 1024.0)) {
                                                    SortedSet<BlockDestructionProgress> sortedSet2 = sortedSetEntry.getValue();
                                                    if (sortedSet2 != null && !sortedSet2.isEmpty()) {
                                                        int r = sortedSet2.last().getProgress();
                                                        poseStack.pushPose();
                                                        poseStack.translate(blockPos.getX() - d, blockPos.getY() - e, blockPos.getZ() - g);
                                                        PoseStack.Pose pose2 = poseStack.last();
                                                        VertexConsumer vertexConsumer2 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(r)), pose2.pose(), pose2.normal());
                                                        this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(blockPos), blockPos, this.level, poseStack, vertexConsumer2);
                                                        poseStack.popPose();
                                                    }
                                                }
                                            }

                                            this.checkPoseStack(poseStack);
                                            HitResult hitResult = this.minecraft.hitResult;
                                            if (bl && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                                                profilerFiller.popPush("outline");
                                                BlockPos blockPos4 = ((BlockHitResult) hitResult).getBlockPos();
                                                BlockState blockState = this.level.getBlockState(blockPos4);
                                                if (!blockState.isAir() && this.level.getWorldBorder().isWithinBounds(blockPos4)) {
                                                    VertexConsumer vertexConsumer3 = bufferSource.getBuffer(RenderType.lines());
                                                    this.renderHitOutline(poseStack, vertexConsumer3, camera.getEntity(), d, e, g, blockPos4, blockState);
                                                }
                                            }

                                            PoseStack poseStack2 = RenderSystem.getModelViewStack();
                                            poseStack2.pushPose();
                                            poseStack2.mulPoseMatrix(poseStack.last().pose());
                                            RenderSystem.applyModelViewMatrix();
                                            this.minecraft.debugRenderer.render(poseStack, bufferSource, d, e, g);

                                            bufferSource.endBatch(Sheets.translucentCullBlockSheet());
                                            bufferSource.endBatch(Sheets.bannerSheet());
                                            bufferSource.endBatch(Sheets.shieldSheet());
                                            bufferSource.endBatch(RenderType.armorGlint());
                                            bufferSource.endBatch(RenderType.armorEntityGlint());
                                            bufferSource.endBatch(RenderType.glint());
                                            bufferSource.endBatch(RenderType.glintDirect());
                                            bufferSource.endBatch(RenderType.glintTranslucent());
                                            bufferSource.endBatch(RenderType.entityGlint());
                                            bufferSource.endBatch(RenderType.entityGlintDirect());
                                            bufferSource.endBatch(RenderType.waterMask());
                                            this.renderBuffers.crumblingBufferSource().endBatch();
                                            //Lines and partcules must be drawn before and  after the chunkLayer respectively to be properly Visible
                                            //Not sure if transparencyChain is needed as the layers seem to render fine without depth Clearing.Copying
                                            if (this.transparencyChain != null) {
                                                RenderStateShard.WEATHER_TARGET.setupRenderState();
                                                profilerFiller.popPush("weather");
                                                this.renderSnowAndRain(lightTexture, f, d, e, g);
                                                this.renderWorldBorder(camera);
                                                RenderStateShard.WEATHER_TARGET.clearRenderState();
                                                this.transparencyChain.process(f);
                                                this.minecraft.getMainRenderTarget().bindWrite(false);
                                            } else {
                                                RenderSystem.depthMask(false);
                                                profilerFiller.popPush("weather");
                                                this.renderSnowAndRain(lightTexture, f, d, e, g);
                                                this.renderWorldBorder(camera);
                                                RenderSystem.depthMask(true);
                                            }
                                            poseStack2.popPose();
                                            RenderSystem.applyModelViewMatrix();
                                            {
                                                profilerFiller.popPush("translucent");
                                                if (this.translucentTarget != null) {
                                                    this.translucentTarget.clear(Minecraft.ON_OSX);
                                                }


                                                profilerFiller.popPush("string");
//                                                this.renderChunkLayer(RenderType.tripwire(), poseStack, d, e, g, matrix4f);
                                                profilerFiller.popPush("particles");
                                                 deinitTransparency();
                                                 this.minecraft.particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
                                                 WorldRenderer.renderChunkLayer(RenderType.translucent(), poseStack, d, e, g, matrix4f);
                                                bufferSource.endBatch(RenderType.lines());
                                                bufferSource.endBatch();

                                            }

                                            poseStack2.pushPose();
                                            poseStack2.mulPoseMatrix(poseStack.last().pose());
                                            RenderSystem.applyModelViewMatrix();
                                            if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
                                                if (this.transparencyChain != null) {
                                                    this.cloudsTarget.clear(Minecraft.ON_OSX);
                                                    RenderStateShard.CLOUDS_TARGET.setupRenderState();
                                                    profilerFiller.popPush("clouds");
                                                    this.renderClouds(poseStack, matrix4f, f, d, e, g);
                                                    RenderStateShard.CLOUDS_TARGET.clearRenderState();
                                                } else {
                                                    profilerFiller.popPush("clouds");
                                                    RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
                                                    this.renderClouds(poseStack, matrix4f, f, d, e, g);
                                                }
                                            }



                                            this.renderDebug(camera);
                                            RenderSystem.depthMask(true);
                                            RenderSystem.disableBlend();
                                            poseStack2.popPose();
                                            RenderSystem.applyModelViewMatrix();
                                            FogRenderer.setupNoFog();
                                            return;
                                        }

                                        LevelRenderer.RenderChunkInfo renderChunkInfo = var40.next();
                                        list = renderChunkInfo.chunk.getCompiledChunk().getRenderableBlockEntities();
                                    } while (list.isEmpty());

                                    for (BlockEntity o : list) {
                                        BlockPos blockPos2 = o.getBlockPos();
                                        MultiBufferSource multiBufferSource2 = bufferSource;
                                        poseStack.pushPose();
                                        poseStack.translate(blockPos2.getX() - d, blockPos2.getY() - e, blockPos2.getZ() - g);
                                        SortedSet<BlockDestructionProgress> sortedSet = this.destructionProgress.get(blockPos2.asLong());
                                        if (sortedSet != null && !sortedSet.isEmpty()) {
                                            m = sortedSet.last().getProgress();
                                            if (m >= 0) {
                                                PoseStack.Pose pose = poseStack.last();
                                                VertexConsumer vertexConsumer = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(m)), pose.pose(), pose.normal());
                                                multiBufferSource2 = (renderType) -> {
                                                    VertexConsumer vertexConsumer2 = bufferSource.getBuffer(renderType);
                                                    return renderType.affectsCrumbling() ? VertexMultiConsumer.create(vertexConsumer, vertexConsumer2) : vertexConsumer2;
                                                };
                                            }
                                        }

                                        this.blockEntityRenderDispatcher.render(o, f, poseStack, multiBufferSource2);
                                        poseStack.popPose();
                                    }
                                }
                            }

                            entity = var26.next();
                        } while (!this.entityRenderDispatcher.shouldRender(entity, frustum, d, e, g) && !entity.hasIndirectPassenger(this.minecraft.player));

                        blockPos = entity.blockPosition();
                    } while (!this.level.isOutsideBuildHeight(blockPos.getY()) && !this.isChunkCompiled(blockPos));
                } while (entity == camera.getEntity() && !camera.isDetached() && (!(camera.getEntity() instanceof LivingEntity) || !((LivingEntity) camera.getEntity()).isSleeping()));
            } while (entity instanceof LocalPlayer && camera.getEntity() != entity);

            ++this.renderedEntities;
            if (entity.tickCount == 0) {
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
            }

            Object multiBufferSource;
            if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(entity)) {
                bl5 = true;
                OutlineBufferSource outlineBufferSource = this.renderBuffers.outlineBufferSource();
                multiBufferSource = outlineBufferSource;
                int i = entity.getTeamColor();
//                int j = true;
                int k = i >> 16 & 255;
                m = i >> 8 & 255;
                int n = i & 255;
                outlineBufferSource.setColor(k, m, n, 255);
            } else {
                multiBufferSource = bufferSource;
            }

            this.renderEntity(entity, d, e, g, f, poseStack, (MultiBufferSource) multiBufferSource);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void setSectionDirty(int x, int y, int z, boolean flag) {
        WorldRenderer.setSectionDirty(x, y, z, flag);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public String getChunkStatistics() {
        return WorldRenderer.getChunkStatistics();
    }
}
