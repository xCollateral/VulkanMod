package net.vulkanmod.mixin.render.clouds;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.render.sky.CloudRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererM {

    @Shadow private int ticks;
    @Shadow private @Nullable ClientLevel level;
    @Shadow @Final protected static ResourceLocation CLOUDS_LOCATION;

    @Unique
    private CloudRenderer cloudRenderer;

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    public void renderClouds(PoseStack poseStack, Matrix4f modelView, Matrix4f projection, float partialTicks, double camX, double camY, double camZ, CallbackInfo ci) {
        if (this.cloudRenderer == null) {
            this.cloudRenderer = new CloudRenderer(CLOUDS_LOCATION);
        }

        this.cloudRenderer.renderClouds(this.level, poseStack, modelView, projection, this.ticks, partialTicks, camX, camY, camZ);
        ci.cancel();
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onAllChanged(CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.reset();
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void onReload(ResourceManager resourceManager, CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.loadTexture(CLOUDS_LOCATION);
        }
    }

}
