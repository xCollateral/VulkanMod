package net.vulkanmod.mixin.texture;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureTickListener;
import net.minecraft.util.Identifier;
import net.vulkanmod.vulkan.TransferQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(TextureManager.class)
public abstract class MTextureManager {

    @Shadow @Final private Set<TextureTickListener> tickListeners;

    @Shadow public abstract AbstractTexture getOrDefault(Identifier id, AbstractTexture fallback);

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

    /**
     * @author
     */
    @Overwrite
    public void destroyTexture(Identifier id) {
        AbstractTexture abstractTexture = this.getOrDefault(id, MissingSprite.getMissingSpriteTexture());
        if (abstractTexture != MissingSprite.getMissingSpriteTexture()) {
            //TODO: delete
        }
    }
}
