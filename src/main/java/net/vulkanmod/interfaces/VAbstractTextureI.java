package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.texture.VulkanImage;

public interface VAbstractTextureI {

    public void bind();

    public VulkanImage getVulkanImage();

    public void setVulkanImage(VulkanImage image);
}
