package net.vulkanmod.vulkan.texture;

import java.nio.ByteBuffer;

public abstract class VTextureSelector {
    private static VulkanImage boundTexture;
    private static VulkanImage boundTexture2;
    private static VulkanImage boundTexture3;
    private static VulkanImage lightTexture;
    private static VulkanImage overlayTexture;
    private static VulkanImage framebufferTexture;
    private static VulkanImage framebufferTexture2;

    private static final VulkanImage whiteTexture = VulkanImage.createWhiteTexture();

    private static int activeTexture = 0;

    public static void bindTexture(VulkanImage texture) {
        boundTexture = texture;
    }

    public static void bindTexture(int i, VulkanImage texture) {
        switch (i) {
            case 0 -> boundTexture = texture;
            case 1 -> lightTexture = texture;
            case 2 -> overlayTexture = texture;
        }
    }

    public static void bindTexture2(VulkanImage texture) {
        boundTexture2 = texture;
    }

    public static void bindTexture3(VulkanImage texture) {
        boundTexture3 = texture;
    }

    public static void bindFramebufferTexture(VulkanImage texture) {
        framebufferTexture = texture;
    }

    public static void bindFramebufferTexture2(VulkanImage texture) {
        framebufferTexture2 = texture;
    }

    public static void uploadSubTexture(int mipLevel, int width, int height, int xOffset, int yOffset, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        VulkanImage texture;
        if(activeTexture == 0) texture = boundTexture;
        else if(activeTexture == 1) texture = lightTexture;
        else texture = overlayTexture;

        texture.uploadSubTextureAsync(mipLevel, width, height, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, unpackRowLength, buffer);
    }

    public static VulkanImage getTexture(String name) {
        return switch (name) {
            case "Sampler0" -> getBoundTexture();
            case "Sampler1" -> getOverlayTexture();
            case "Sampler2" -> getLightTexture();
            case "Sampler3" -> boundTexture2;
            case "Sampler4" -> boundTexture3;
            case "Framebuffer0" -> framebufferTexture;
            case "Framebuffer1" -> framebufferTexture2;
            default -> throw new RuntimeException("unknown sampler name: " + name);
        };
    }

    public static void setLightTexture(VulkanImage texture) {
        lightTexture = texture;
    }

    public static void setOverlayTexture(VulkanImage texture) { overlayTexture = texture; }

    public static void setActiveTexture(int activeTexture) {
        VTextureSelector.activeTexture = activeTexture;
    }

    public static VulkanImage getLightTexture() {
        return lightTexture;
    }

    public static VulkanImage getOverlayTexture() {
        return overlayTexture;
    }

    public static VulkanImage getBoundTexture() { return boundTexture; }

    public static VulkanImage getWhiteTexture() { return whiteTexture; }
}
