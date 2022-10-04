package net.vulkanmod.mixin.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.PaintingManager;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.WindowProvider;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.util.Util;
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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Final public ParticleManager particleManager;
    @Shadow @Final public GameRenderer gameRenderer;
    @Shadow @Final private ReloadableResourceManagerImpl resourceManager;
    @Shadow @Final private FontManager fontManager;
    @Shadow @Final private BakedModelManager bakedModelManager;
    @Shadow @Final private PaintingManager paintingManager;

    @Shadow @Final public WorldRenderer worldRenderer;

    @Shadow @Final private SoundManager soundManager;

    @Shadow @Final private ResourcePackManager resourcePackManager;

    @Shadow @Final private StatusEffectSpriteManager statusEffectSpriteManager;

    @Shadow @Final private TextureManager textureManager;

    @Shadow @Final private WindowProvider windowProvider;

    @Shadow @Final private Window window;

    @Shadow @Final private static Logger LOGGER;

    @Shadow public boolean skipGameRender;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void test(RunArgs args, CallbackInfo ci) {

    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", shift = At.Shift.BEFORE))
    private void beginRender(boolean tick, CallbackInfo ci) {
        Drawer drawer = Drawer.getInstance();
        drawer.initiateRenderPass();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;swapBuffers()V", shift = At.Shift.BEFORE))
    private void submitRender(boolean tick, CallbackInfo ci) {
        Drawer drawer = Drawer.getInstance();
        drawer.endRenderPass();
        drawer.submitDraw();
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void limitWhenMinimized(CallbackInfoReturnable<Integer> cir) {
        if(this.skipGameRender) cir.setReturnValue(10);
    }

    /**
     * @author
     */
    @Overwrite
    public void close() {
        //TODO: clear all Vulkan resources
        try {
            this.bakedModelManager.close();
            this.fontManager.close();
            this.gameRenderer.close();
            this.worldRenderer.close();
            this.soundManager.close();
            this.resourcePackManager.close();
            this.particleManager.clearAtlas();
            this.statusEffectSpriteManager.close();
            this.paintingManager.close();
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
            this.windowProvider.close();
            this.window.close();
        }
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;cleanUpAfterCrash()V"))
    private void skipEmergencySave(MinecraftClient instance) {

    }

    @Inject(method = "onResolutionChanged", at = @At("HEAD"))
    public void onResolutionChanged(CallbackInfo ci) {
        Drawer.shouldRecreate = true;
    }
}
