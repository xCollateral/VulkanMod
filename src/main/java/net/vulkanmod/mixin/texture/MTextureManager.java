package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.texture.SpriteUtil;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(TextureManager.class)
public abstract class MTextureManager {

    @Shadow @Final private Set<Tickable> tickableTextures;

    /**
     * @author
     */
    @Overwrite
    public void tick() {
        if (Renderer.skipRendering | !Initializer.CONFIG.enableAnimations)
            return;

        //Debug D
        if (SpriteUtil.shouldUpload())
            DeviceManager.getGraphicsQueue().startRecording();
        for (Tickable tickable : this.tickableTextures) {
            tickable.tick();
        }
        if (SpriteUtil.shouldUpload()) {
            SpriteUtil.transitionLayouts(DeviceManager.getGraphicsQueue().getCommandBuffer().getHandle());
            DeviceManager.getGraphicsQueue().endRecordingAndSubmit();
        }
    }

    @Inject(method = "register(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V", at= @At(value = "RETURN", target = "Lnet/minecraft/client/renderer/texture/TextureManager;tickableTextures:Ljava/util/Set;"))
    private void injectRegister(ResourceLocation resourceLocation, AbstractTexture abstractTexture, CallbackInfo ci)
    {
        VTextureSelector.registerTexture(((VAbstractTextureI)(abstractTexture)).getId2(), -1, resourceLocation);
    }
}
