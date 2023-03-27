package net.vulkanmod.mixin.screen;

import net.minecraft.client.gui.screens.LoadingOverlay;
import net.vulkanmod.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(LoadingOverlay.class)
public class LoadingOverlayM
{
    @Redirect(method = "render", at = @At(value="INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_clearColor(FFFF)V"))
    private void clearColor(float f, float g, float h, float i)
    {
        VRenderSystem.setRenderPassColor(f, g, h);
    }

}
