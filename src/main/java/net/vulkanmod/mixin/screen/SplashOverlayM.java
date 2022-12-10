package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public abstract class SplashOverlayM {

    @Shadow @Final private Minecraft minecraft;

    @Shadow @Final private boolean fadeIn;
    @Shadow @Final private static ResourceLocation MOJANG_STUDIOS_LOGO_LOCATION;
    @Shadow @Final private static int LOGO_BACKGROUND_COLOR;
    @Shadow @Final private static int LOGO_BACKGROUND_COLOR_DARK;
    @Shadow @Final private static IntSupplier BRAND_BACKGROUND;
    @Shadow @Final private static int LOGO_SCALE;
    @Shadow @Final private static float LOGO_QUARTER_FLOAT;
    @Shadow @Final private static int LOGO_QUARTER;
    @Shadow @Final private static int LOGO_HALF;
    @Shadow @Final private static float LOGO_OVERLAP;
    @Shadow @Final private static float SMOOTHING;
    @Shadow @Final public static long FADE_OUT_TIME;
    @Shadow @Final public static long FADE_IN_TIME;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;

    @Shadow
    public static void registerTextures(Minecraft minecraft) {
    }

    @Shadow protected abstract void drawProgressBar(PoseStack poseStack, int i, int j, int k, int l, float f);

    @Shadow public abstract boolean isPauseScreen();

//    /**
//     * @author
//     */
//    @Overwrite
//    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
//        float h;
//        int k;
//        float g;
//        int i = this.client.getWindow().getScaledWidth();
//        int j = this.client.getWindow().getScaledHeight();
//        long l = Util.getMeasuringTimeMs();
//        if (this.reloading && this.reloadStartTime == -1L) {
//            this.reloadStartTime = l;
//        }
//        float f = this.reloadCompleteTime > -1L ? (float)(l - this.reloadCompleteTime) / 1000.0f : -1.0f;
//        float f2 = g = this.reloadStartTime > -1L ? (float)(l - this.reloadStartTime) / 500.0f : -1.0f;
//        if (f >= 1.0f) {
//            if (this.client.currentScreen != null) {
//                this.client.currentScreen.render(matrices, 0, 0, delta);
//            }
//            k = Math.ceil((1.0f - Math.clamp(f - 1.0f, 0.0f, 1.0f)) * 255.0f);
//            SplashOverlay.fill(matrices, 0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), k));
//            h = 1.0f - MathHelper.clamp(f - 1.0f, 0.0f, 1.0f);
//        } else if (this.reloading) {
//            if (this.client.currentScreen != null && g < 1.0f) {
//                this.client.currentScreen.render(matrices, mouseX, mouseY, delta);
//            }
//            k = MathHelper.ceil(MathHelper.clamp((double)g, 0.15, 1.0) * 255.0);
//            SplashOverlay.fill(matrices, 0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), k));
//            h = MathHelper.clamp(g, 0.0f, 1.0f);
//        } else {
//            k = BRAND_ARGB.getAsInt();
//            float m = (float)(k >> 16 & 0xFF) / 255.0f;
//            float n = (float)(k >> 8 & 0xFF) / 255.0f;
//            float o = (float)(k & 0xFF) / 255.0f;
////            GlStateManager._clearColor(m, n, o, 1.0f);
////            GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
//
//            //Vulkan
//            RenderSystem.clearColor(m, n, o, 1.0f);
//            RenderSystem.clear(16384, MinecraftClient.IS_SYSTEM_MAC);
//            h = 1.0f;
//        }
//        k = (int)((double)this.client.getWindow().getScaledWidth() * 0.5);
//        int m = (int)((double)this.client.getWindow().getScaledHeight() * 0.5);
//        double n = Math.min((double)this.client.getWindow().getScaledWidth() * 0.75, (double)this.client.getWindow().getScaledHeight()) * 0.25;
//        int p = (int)(n * 0.5);
//        double d = n * 4.0;
//        int q = (int)(d * 0.5);
//        RenderSystem.setShaderTexture(0, LOGO);
//        RenderSystem.enableBlend();
////        RenderSystem.blendEquation(32774);
////        RenderSystem.blendFunc(770, 1);
//        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, h);
//        SplashOverlay.drawTexture(matrices, k - q, m - p, q, (int)n, -0.0625f, 0.0f, 120, 60, 120, 120);
//        SplashOverlay.drawTexture(matrices, k, m - p, q, (int)n, 0.0625f, 60.0f, 120, 60, 120, 120);
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.disableBlend();
//        int r = (int)((double)this.client.getWindow().getScaledHeight() * 0.8325);
//        float s = this.reload.getProgress();
//        this.progress = MathHelper.clamp(this.progress * 0.95f + s * 0.050000012f, 0.0f, 1.0f);
//        if (f < 1.0f) {
//            this.renderProgressBar(matrices, i / 2 - q, r - 5, i / 2 + q, r + 5, 1.0f - MathHelper.clamp(f, 0.0f, 1.0f));
//        }
//        if (f >= 2.0f) {
//            this.client.setOverlay(null);
//        }
//        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || g >= 2.0f)) {
//            try {
//                this.reload.throwException();
//                this.exceptionHandler.accept(Optional.empty());
//            }
//            catch (Throwable throwable) {
//                this.exceptionHandler.accept(Optional.of(throwable));
//            }
//            this.reloadCompleteTime = Util.getMeasuringTimeMs();
//            if (this.client.currentScreen != null) {
//                this.client.currentScreen.init(this.client, this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
//            }
//        }
//    }

    /**
     * @author
     */
    @Overwrite
    public void render(PoseStack poseStack, int i, int j, float f) {
        float o;
        int n;
        float h;
        int k = this.minecraft.getWindow().getGuiScaledWidth();
        int l = this.minecraft.getWindow().getGuiScaledHeight();
        long m = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = m;
        }
        float g = this.fadeOutStart > -1L ? (float)(m - this.fadeOutStart) / 1000.0f : -1.0f;
        float f2 = h = this.fadeInStart > -1L ? (float)(m - this.fadeInStart) / 500.0f : -1.0f;
        if (g >= 1.0f) {
            if (this.minecraft.screen != null) {
                this.minecraft.screen.render(poseStack, 0, 0, f);
            }
            n = Mth.ceil((1.0f - Mth.clamp(g - 1.0f, 0.0f, 1.0f)) * 255.0f);
            LoadingOverlay.fill(poseStack, 0, 0, k, l, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = 1.0f - Mth.clamp(g - 1.0f, 0.0f, 1.0f);
        } else if (this.fadeIn) {
            if (this.minecraft.screen != null && h < 1.0f) {
                this.minecraft.screen.render(poseStack, i, j, f);
            }
            n = Mth.ceil(Mth.clamp((double)h, 0.15, 1.0) * 255.0);
            LoadingOverlay.fill(poseStack, 0, 0, k, l, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = Mth.clamp(h, 0.0f, 1.0f);
        } else {
            n = BRAND_BACKGROUND.getAsInt();
            float p = (float)(n >> 16 & 0xFF) / 255.0f;
            float q = (float)(n >> 8 & 0xFF) / 255.0f;
            float r = (float)(n & 0xFF) / 255.0f;
//            GlStateManager._clearColor(p, q, r, 1.0f);
//            GlStateManager._clear(16384, Minecraft.ON_OSX);
            o = 1.0f;

            //Vulkan
            RenderSystem.clearColor(p, q, r, 1.0f);
            RenderSystem.clear(16384, Minecraft.ON_OSX);
        }
        n = (int)((double)this.minecraft.getWindow().getGuiScaledWidth() * 0.5);
        int s = (int)((double)this.minecraft.getWindow().getGuiScaledHeight() * 0.5);
        double d = Math.min((double)this.minecraft.getWindow().getGuiScaledWidth() * 0.75, (double)this.minecraft.getWindow().getGuiScaledHeight()) * 0.25;
        int t = (int)(d * 0.5);
        double e = d * 4.0;
        int u = (int)(e * 0.5);
        RenderSystem.setShaderTexture(0, MOJANG_STUDIOS_LOGO_LOCATION);
        RenderSystem.enableBlend();
//        RenderSystem.blendEquation(32774);
//        RenderSystem.blendFunc(770, 1);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, o);
        LoadingOverlay.blit(poseStack, n - u, s - t, u, (int)d, -0.0625f, 0.0f, 120, 60, 120, 120);
        LoadingOverlay.blit(poseStack, n, s - t, u, (int)d, 0.0625f, 60.0f, 120, 60, 120, 120);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        int v = (int)((double)this.minecraft.getWindow().getGuiScaledHeight() * 0.8325);
        float w = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95f + w * 0.050000012f, 0.0f, 1.0f);
        if (g < 1.0f) {
            this.drawProgressBar(poseStack, k / 2 - u, v - 5, k / 2 + u, v + 5, 1.0f - Mth.clamp(g, 0.0f, 1.0f));
        }
        if (g >= 2.0f) {
            this.minecraft.setOverlay(null);
        }
        if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || h >= 2.0f)) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            }
            catch (Throwable throwable) {
                this.onFinish.accept(Optional.of(throwable));
            }
            this.fadeOutStart = Util.getMillis();
            if (this.minecraft.screen != null) {
                this.minecraft.screen.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
            }
        }
    }

    private static int replaceAlpha(int i, int j) {
        return i & 0xFFFFFF | j << 24;
    }
}
