package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

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
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.vulkanmod.vulkan.Drawer;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    public void test(GameConfig gameConfig, CallbackInfo ci) {

    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))
    private void beginRender(int i, boolean bl) {
        Drawer drawer = Drawer.getInstance();
        drawer.initiateRenderPass();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V", shift = At.Shift.BEFORE))
    private void submitRender(boolean tick, CallbackInfo ci) {
        Drawer drawer = Drawer.getInstance();
        drawer.endRenderPass();
        drawer.submitDraw();
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void limitWhenMinimized(CallbackInfoReturnable<Integer> cir) {
        if(this.noRender) cir.setReturnValue(10);
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;getInstance()Ljava/util/Optional;"))
    private Optional<TimerQuery> removeTimer() {
        return Optional.empty();
    }

    /**
     * @author
     */
    @Overwrite
    public void close() {
        //TODO: clear all Vulkan resources
        try {
            this.modelManager.close();
            this.fontManager.close();
            this.gameRenderer.close();
            this.levelRenderer.close();
            this.soundManager.destroy();
            this.particleEngine.close();
            this.mobEffectTextures.close();
            this.paintingTextures.close();
            //this.textureManager.close();
            this.resourceManager.close();

            Vulkan.cleanUp();

            Util.shutdownExecutors();
        }
        catch (Throwable throwable) {
            LOGGER.error("Shutdown failure!", throwable);
            throw throwable;
        }
        finally {
            this.virtualScreen.close();
            this.window.close();
        }
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;emergencySave()V"))
    private void skipEmergencySave(Minecraft instance) {

    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"))
    public void onResolutionChanged(CallbackInfo ci) {
        Drawer.shouldRecreate = true;
    }

}
