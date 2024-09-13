package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.DescriptorManager;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;

import java.nio.ByteBuffer;

public abstract class VTextureSelector {
    public static final int SIZE = 12;

    private static final VulkanImage[] boundTextures = new VulkanImage[SIZE];
    private static final int[] DescriptorIndices = new int[SIZE];

    private static final int[] levels = new int[SIZE];

    private static final VulkanImage whiteTexture = VulkanImage.createWhiteTexture();

    private static int activeTexture = 0;

    public static void bindTexture(VulkanImage texture) {
        boundTextures[0] = texture;
    }

    public static void bindTexture(int i, VulkanImage texture) {
        if(i < 0 || i >= SIZE) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        levels[i] = -1;
    }

    public static void bindImage(int i, VulkanImage texture, int level) {
        if(i < 0 || i > 7) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        levels[i] = level;
    }

    public static void uploadSubTexture(int mipLevel, int width, int height, int xOffset, int yOffset, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        VulkanImage texture = boundTextures[activeTexture];

        if(texture == null)
            throw new NullPointerException("Texture is null at index: " + activeTexture);

        texture.uploadSubTextureAsync(mipLevel, width, height, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, unpackRowLength, buffer);
    }

    public static int getTextureIdx(String name) {
        return switch (name) {
            case "Sampler0", "DiffuseSampler" -> 0;
            case "Sampler1", "SamplerProj" -> 1;
            case "Sampler2" -> 2;
            case "Sampler3" -> 3;
            case "Sampler4" -> 4;
            case "Sampler5" -> 5;
            case "Sampler6" -> 6;
            case "Sampler7" -> 7;
            default -> throw new IllegalStateException("Unknown sampler name: " + name);
        };
    }

    public static int bindShaderTextures(Pipeline pipeline) {
        boolean isNewTexture = false;
        final boolean bindless = pipeline.isBindless();
        final int setID = pipeline.getSetID();

        for (ImageDescriptor state : pipeline.getFragImageDescriptors()) {
            final int shaderTexture = RenderSystem.getShaderTexture(state.imageIdx);

            if(bindless) {
                //Add texture to the DescriptorSet if its new.unique
                DescriptorManager.registerTexture(setID, state.imageIdx, shaderTexture);

                //Convert TextureID to Sampler Index
                VTextureSelector.setSamplerIndex(state.imageIdx, DescriptorManager.getTexture(setID, state.imageIdx, shaderTexture));
                isNewTexture |= DescriptorManager.isTexUnInitialised(setID, shaderTexture);
            }
            else {
                final GlTexture texture = GlTexture.getTexture(shaderTexture);
                if (texture != null)
                    VTextureSelector.bindTexture(state.imageIdx, texture.getVulkanImage());
            }

        }
        return bindless ? isNewTexture ? -1 : VTextureSelector.getSamplerIndex(0) : 0;
    }

    public static VulkanImage getImage(int i) {
        return boundTextures[i];
    }

    public static void setLightTexture(VulkanImage texture) {
        boundTextures[2] = texture;
    }

    public static void setOverlayTexture(VulkanImage texture) {
        boundTextures[1] = texture;
    }

    public static void setActiveTexture(int activeTexture) {
        if(activeTexture < 0 || activeTexture >= SIZE) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", activeTexture, SIZE - 1));
        }

        VTextureSelector.activeTexture = activeTexture;
    }

    public static VulkanImage getBoundTexture(int i) { return boundTextures[i]; }

    public static VulkanImage getWhiteTexture() { return whiteTexture; }

    public static void setSamplerIndex(int imageIdx, int texture) {
        DescriptorIndices[imageIdx] = texture;
    }

    public static int getSamplerIndex(int i) {
        return DescriptorIndices[i];
    }
}
