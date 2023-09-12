package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(HttpTexture.class)
public class MPlayerSkinTexture {

    /**
     * @author
     */
    @Overwrite
    private void upload(NativeImage image) {
        VulkanImage vulkanImage = new VulkanImage.Builder(image.getWidth(), image.getHeight()).createVulkanImage();
        ((VAbstractTextureI)this).setVulkanImage(vulkanImage);
        ((VAbstractTextureI)this).bindTexture();
        image.upload(0, 0, 0, true);
    }
}
