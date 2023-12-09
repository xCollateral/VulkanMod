package net.vulkanmod.mixin.render;

import net.minecraft.client.renderer.FogRenderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
public class FogRendererM {
    @Redirect(method = "setupColor", at=@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V"))
    private static void getSFogClearColor(float f, float g, float h, float i)
    {
        VRenderSystem.setFogClearColor(f, g, h, i);
    }
}
