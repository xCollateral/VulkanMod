package net.vulkanmod.mixin.texture;

import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ResourceTexture.class)
public class MSimpleTexture {



    /**
     * @author
     */
    @Overwrite
    private void upload(NativeImage nativeImage, boolean blur, boolean clamp) {
        ((VAbstractTextureI)this).setVulkanImage(new VulkanImage(1, nativeImage.getWidth(), nativeImage.getHeight(), nativeImage.getFormat().getChannelCount(), blur, clamp));
        ((VAbstractTextureI)this).bind();
        nativeImage.upload(0, 0, 0, 0, 0, nativeImage.getWidth(), nativeImage.getHeight(), blur, clamp, false, true);
    }

}
