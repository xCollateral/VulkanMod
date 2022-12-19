package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.SkyBoxVBO;
import net.vulkanmod.render.chunk.TaskDispatcher;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Overwrite BufferBuilder.RenderedBuffer drawStars(BufferBuilder bufferBuilder) {return null;};

    @Shadow protected abstract boolean doesMobEffectBlockSky(Camera camera);

    @Shadow protected abstract void renderEndSky(PoseStack poseStack);

    @Shadow @Final private static ResourceLocation SUN_LOCATION;
    @Shadow @Final private static ResourceLocation MOON_LOCATION;

    @Shadow protected abstract void deinitTransparency();

//    private WorldRenderer worldRenderer;

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
        deinitTransparency();
        WorldRenderer.renderChunkLayer(RenderType.translucent(), poseStack, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z, matrix4f);
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
