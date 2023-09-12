package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SimpleTexture.class)
public class MSimpleTexture {

    /**
     * @author
     */
    @Overwrite
    private void doLoad(NativeImage nativeImage, boolean blur, boolean clamp) {
        VulkanImage image = new VulkanImage.Builder(nativeImage.getWidth(), nativeImage.getHeight())
                .setLinearFiltering(blur)
                .setClamp(clamp)
                .createVulkanImage();
        ((VAbstractTextureI)this).setVulkanImage(image);
        ((VAbstractTextureI)this).bindTexture();
        nativeImage.upload(0, 0, 0, 0, 0, nativeImage.getWidth(), nativeImage.getHeight(), blur, clamp, false, true);
    }

}
