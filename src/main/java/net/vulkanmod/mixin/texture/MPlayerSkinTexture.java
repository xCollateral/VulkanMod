package net.vulkanmod.mixin.texture;

import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PlayerSkinTexture.class)
public class MPlayerSkinTexture {

    /**
     * @author
     */
    @Overwrite
    private void uploadTexture(NativeImage image) {
        ((VAbstractTextureI)this).setVulkanImage(new VulkanImage(image.getWidth(), image.getHeight()));
        ((VAbstractTextureI)this).bind();
        image.upload(0, 0, 0, true);
    }
}
