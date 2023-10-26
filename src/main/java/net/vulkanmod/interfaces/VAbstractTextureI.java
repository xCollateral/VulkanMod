package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.texture.VulkanImage;

public interface VAbstractTextureI {

    void bindTexture();

    void setId(int i);

    VulkanImage getVulkanImage();

    void setVulkanImage(VulkanImage image);
}
