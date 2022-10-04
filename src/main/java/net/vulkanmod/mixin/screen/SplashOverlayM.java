package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayM {

    @Shadow @Final private static Identifier LOGO;
    @Shadow @Final private static int MOJANG_RED;
    @Shadow @Final private static int MONOCHROME_BLACK;
    @Shadow @Final private static IntSupplier BRAND_ARGB;
    @Shadow @Final private static int field_32251;
    @Shadow @Final private static float field_32252;
    @Shadow @Final private static int field_32253;
    @Shadow @Final private static int field_32254;
    @Shadow @Final private static float field_32255;
    @Shadow @Final private static float field_32256;
    @Shadow @Final public static long RELOAD_COMPLETE_FADE_DURATION;
    @Shadow @Final public static long RELOAD_START_FADE_DURATION;
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    @Shadow
    private static int withAlpha(int color, int alpha) {
        return 0;
    }

    @Shadow protected abstract void renderProgressBar(MatrixStack matrices, int minX, int minY, int maxX, int maxY, float opacity);

    /**
     * @author
     */
    @Overwrite
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float h;
        int k;
        float g;
        int i = this.client.getWindow().getScaledWidth();
        int j = this.client.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = l;
        }
        float f = this.reloadCompleteTime > -1L ? (float)(l - this.reloadCompleteTime) / 1000.0f : -1.0f;
        float f2 = g = this.reloadStartTime > -1L ? (float)(l - this.reloadStartTime) / 500.0f : -1.0f;
        if (f >= 1.0f) {
            if (this.client.currentScreen != null) {
                this.client.currentScreen.render(matrices, 0, 0, delta);
            }
            k = MathHelper.ceil((1.0f - MathHelper.clamp(f - 1.0f, 0.0f, 1.0f)) * 255.0f);
            SplashOverlay.fill(matrices, 0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), k));
            h = 1.0f - MathHelper.clamp(f - 1.0f, 0.0f, 1.0f);
        } else if (this.reloading) {
            if (this.client.currentScreen != null && g < 1.0f) {
                this.client.currentScreen.render(matrices, mouseX, mouseY, delta);
            }
            k = MathHelper.ceil(MathHelper.clamp((double)g, 0.15, 1.0) * 255.0);
            SplashOverlay.fill(matrices, 0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), k));
            h = MathHelper.clamp(g, 0.0f, 1.0f);
        } else {
            k = BRAND_ARGB.getAsInt();
            float m = (float)(k >> 16 & 0xFF) / 255.0f;
            float n = (float)(k >> 8 & 0xFF) / 255.0f;
            float o = (float)(k & 0xFF) / 255.0f;
//            GlStateManager._clearColor(m, n, o, 1.0f);
//            GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);

            //Vulkan
            RenderSystem.clearColor(m, n, o, 1.0f);
            RenderSystem.clear(16384, MinecraftClient.IS_SYSTEM_MAC);
            h = 1.0f;
        }
        k = (int)((double)this.client.getWindow().getScaledWidth() * 0.5);
        int m = (int)((double)this.client.getWindow().getScaledHeight() * 0.5);
        double n = Math.min((double)this.client.getWindow().getScaledWidth() * 0.75, (double)this.client.getWindow().getScaledHeight()) * 0.25;
        int p = (int)(n * 0.5);
        double d = n * 4.0;
        int q = (int)(d * 0.5);
        RenderSystem.setShaderTexture(0, LOGO);
        RenderSystem.enableBlend();
//        RenderSystem.blendEquation(32774);
//        RenderSystem.blendFunc(770, 1);
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, h);
        SplashOverlay.drawTexture(matrices, k - q, m - p, q, (int)n, -0.0625f, 0.0f, 120, 60, 120, 120);
        SplashOverlay.drawTexture(matrices, k, m - p, q, (int)n, 0.0625f, 60.0f, 120, 60, 120, 120);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        int r = (int)((double)this.client.getWindow().getScaledHeight() * 0.8325);
        float s = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95f + s * 0.050000012f, 0.0f, 1.0f);
        if (f < 1.0f) {
            this.renderProgressBar(matrices, i / 2 - q, r - 5, i / 2 + q, r + 5, 1.0f - MathHelper.clamp(f, 0.0f, 1.0f));
        }
        if (f >= 2.0f) {
            this.client.setOverlay(null);
        }
        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || g >= 2.0f)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            }
            catch (Throwable throwable) {
                this.exceptionHandler.accept(Optional.of(throwable));
            }
            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(this.client, this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
            }
        }
    }
}
