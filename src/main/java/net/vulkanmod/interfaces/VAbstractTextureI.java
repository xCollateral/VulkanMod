package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.texture.VulkanImage;

public interface VAbstractTextureI {

    void bindTexture();

    int getId2();
    void setId(int i);

    VulkanImage getVulkanImage();

    void setVulkanImage(VulkanImage image);
}
