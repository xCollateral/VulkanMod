package net.vulkanmod.mixin.settings.fog;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.vulkanmod.Initializer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Shadow
    @Nullable
    private static FogRenderer.MobEffectFogFunction getPriorityFogFunction(Entity entity, float f) {
        return null;
    }

    @Inject(method = "setupFog", at = @At(value = "TAIL"))
    private static void onSetupFog(Camera camera, FogRenderer.FogMode fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        float fogDistance = Initializer.CONFIG.fogDistance;
        Entity entity = camera.getEntity();
        FogRenderer.MobEffectFogFunction mobEffectFogFunction = getPriorityFogFunction(entity, tickDelta);

        if (fogDistance == 0 || mobEffectFogFunction != null) {
            return;
        }

        if (camera.getFluidInCamera() == FogType.NONE && (thickFog || fogType == FogRenderer.FogMode.FOG_TERRAIN)) {
            float fogStart = (float) Initializer.CONFIG.fogStart / 100;
            if (fogDistance == 33) {
                RenderSystem.setShaderFogColor(1f, 1f, 1f, 0f);
            } else {
                RenderSystem.setShaderFogStart(fogDistance * 16 * fogStart);
                RenderSystem.setShaderFogEnd((fogDistance + 1) * 16);
            }
        }
    }
}
