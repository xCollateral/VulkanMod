package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.vulkanmod.config.VideoResolution;
import net.vulkanmod.render.texture.SpriteUtil;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.profiling.Profiler2;
import net.vulkanmod.render.texture.SpriteUtil;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.Optional;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow @Final public ParticleEngine particleEngine;
    @Shadow @Final public GameRenderer gameRenderer;
    @Shadow @Final private ReloadableResourceManager resourceManager;
    @Shadow @Final private FontManager fontManager;
    @Shadow @Final private ModelManager modelManager;
    @Shadow @Final private SoundManager soundManager;
    @Shadow @Final private TextureManager textureManager;
    @Shadow @Final private VirtualScreen virtualScreen;
    @Shadow @Final private Window window;
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final public LevelRenderer levelRenderer;
    @Shadow @Final private MobEffectTextureManager mobEffectTextures;
    @Shadow @Final private PaintingTextureManager paintingTextures;
    @Shadow public boolean noRender;
    @Shadow @Final public Options options;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void forceGraphicsMode(GameConfig gameConfig, CallbackInfo ci) {
        var graphicsModeOption = this.options.graphicsMode();

        if(graphicsModeOption.get() == GraphicsStatus.FABULOUS) {
            Initializer.LOGGER.error("Fabulous graphics mode not supported, forcing Fancy");
            graphicsModeOption.set(GraphicsStatus.FANCY);
        }
    }

    @Shadow @Final private VanillaPackResources vanillaPackResources;

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))
    private void beginRender(int i, boolean bl) {
        Renderer renderer = Renderer.getInstance();
        renderer.beginFrame();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V", shift = At.Shift.BEFORE))
    private void submitRender(boolean tick, CallbackInfo ci) {
        Renderer renderer = Renderer.getInstance();
        Profiler2 p = Profiler2.getMainProfiler();
        p.push("submitRender");
        renderer.endFrame();
        p.pop();
    }

    @Redirect(method="<init>", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/Window;setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V"))
    private void bypassWaylandIcon(Window instance, PackResources packResources, IconSet iconSet) throws IOException {
        if(!VideoResolution.isWayLand())
        {
            this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().isStable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
        }
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V"))
    private void redirectMainTarget1(RenderTarget instance, boolean bl) {
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;unbindWrite()V"))
    private void redirectMainTarget2(RenderTarget instance) {
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V"))
    private void removeBlit(RenderTarget instance, int i, int j) {
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"))
    private void removeThreadYield() {
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void limitWhenMinimized(CallbackInfoReturnable<Integer> cir) {
        if(this.noRender) cir.setReturnValue(10);
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;getInstance()Ljava/util/Optional;"))
    private Optional<TimerQuery> removeTimer() {
        return Optional.empty();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"),
    locals = LocalCapture.CAPTURE_FAILHARD)
    private void redirectResourceTick(boolean bl, CallbackInfo ci, long l, Runnable runnable, int i, int j) {
        int n = Math.min(10, i) - 1;
        SpriteUtil.setDoUpload(j == n);
    }

    @Inject(method = "runTick", at = @At(value = "HEAD"))
    private void resetBuffers(boolean bl, CallbackInfo ci) {
        Renderer.getInstance().resetBuffers();
    }


    @Inject(method = "close", at = @At(value = "HEAD"))
    public void close(CallbackInfo ci) {
        Vulkan.waitIdle();

    }
    @Inject(method = "close", at = @At(value = "RETURN"))
    public void close2(CallbackInfo ci) {

        Vulkan.cleanUp();

        Util.shutdownExecutors();

    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;emergencySave()V"))
    private void skipEmergencySave(Minecraft instance) {

    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"))
    public void onResolutionChanged(CallbackInfo ci) {
        Renderer.scheduleSwapChainUpdate();
    }

}
