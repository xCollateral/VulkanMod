package net.vulkanmod.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelM {

    @Inject(method = "setLightReady", at = @At("RETURN"))
    private void setLightReady(int i, int j, CallbackInfo ci) {
        WorldRenderer.getInstance().setSectionsLightReady(i, j);
    }
}
