package net.vulkanmod.mixin.texture;

import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureTickListener;
import net.vulkanmod.vulkan.TransferQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(TextureManager.class)
public class MTextureManager {

    @Shadow @Final private Set<TextureTickListener> tickListeners;

    /**
     * @author
     */
    @Overwrite
    public void tick() {
        TransferQueue.startRecording();
        for (TextureTickListener textureTickListener : this.tickListeners) {
            textureTickListener.tick();
        }
        TransferQueue.endRecording();
    }
}
