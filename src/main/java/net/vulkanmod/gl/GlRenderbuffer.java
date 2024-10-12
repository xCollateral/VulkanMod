package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GlRenderbuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlRenderbuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlRenderbuffer bound;

    public static int genId() {
        int id = ID_COUNTER;
        map.put(id, new GlRenderbuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindRenderbuffer(int target, int id) {
        boundId = id;
        bound = map.get(id);

        if (id <= 0)
            return;

        if (bound == null)
            throw new NullPointerException("bound texture is null");

        VulkanImage vulkanImage = bound.vulkanImage;
        if (vulkanImage != null)
            VTextureSelector.bindTexture(vulkanImage);
    }

    public static void deleteRenderbuffer(int i) {
        map.remove(i);
    }

    public static GlRenderbuffer getRenderbuffer(int id) {
        return map.get(id);
    }

    public static void renderbufferStorage(int target, int internalFormat, int width, int height) {
        if (width == 0 || height == 0)
            return;

        bound.internalFormat = internalFormat;

        bound.allocateIfNeeded(width, height, internalFormat);
    }

    public static void texParameteri(int target, int pName, int param) {
        if (target != GL11.GL_TEXTURE_2D)
            throw new UnsupportedOperationException();

        switch (pName) {
            case GL30.GL_TEXTURE_MAX_LEVEL -> bound.setMaxLevel(param);
            case GL30.GL_TEXTURE_MAX_LOD -> bound.setMaxLod(param);
            case GL30.GL_TEXTURE_MIN_LOD -> {}
            case GL30.GL_TEXTURE_LOD_BIAS -> {}

            case GL11.GL_TEXTURE_MAG_FILTER -> bound.setMagFilter(param);
            case GL11.GL_TEXTURE_MIN_FILTER -> bound.setMinFilter(param);

            default -> {
            }
        }

        //TODO
    }

    public static int getTexLevelParameter(int target, int level, int pName) {
        if (bound == null || target == GL11.GL_TEXTURE_2D)
            return -1;

        return switch (pName) {
            case GL11.GL_TEXTURE_INTERNAL_FORMAT -> GlUtil.getGlFormat(bound.vulkanImage.format);
            case GL11.GL_TEXTURE_WIDTH -> bound.vulkanImage.width;
            case GL11.GL_TEXTURE_HEIGHT -> bound.vulkanImage.height;

            default -> -1;
        };
    }

    public static void generateMipmap(int target) {
        if (target != GL11.GL_TEXTURE_2D)
            throw new UnsupportedOperationException();

        bound.generateMipmaps();
    }

    public static void setVulkanImage(int id, VulkanImage vulkanImage) {
        GlRenderbuffer texture = map.get(id);

        texture.vulkanImage = vulkanImage;
    }

    public static GlRenderbuffer getBound() {
        return bound;
    }

    final int id;
    VulkanImage vulkanImage;
    int internalFormat;

    boolean needsUpdate = false;
    int maxLevel = 0;
    int maxLod = 0;
    int minFilter, magFilter = GL11.GL_LINEAR;

    public GlRenderbuffer(int id) {
        this.id = id;
    }

    void allocateIfNeeded(int width, int height, int format) {
        int vkFormat = GlUtil.vulkanFormat(format);

        needsUpdate |= vulkanImage == null ||
                vulkanImage.width != width || vulkanImage.height != height ||
                vkFormat != vulkanImage.format;

        if (needsUpdate) {
            allocateImage(width, height, vkFormat);
            updateSampler();

            needsUpdate = false;
        }
    }

    void allocateImage(int width, int height, int vkFormat) {
        if (this.vulkanImage != null)
            this.vulkanImage.free();

        if (VulkanImage.isDepthFormat(vkFormat))
            this.vulkanImage = VulkanImage.createDepthImage(vkFormat,
                    width, height,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
                    false, true);
        else
            this.vulkanImage = new VulkanImage.Builder(width, height)
                    .setMipLevels(maxLevel + 1)
                    .setFormat(vkFormat)
                    .addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .createVulkanImage();

        VTextureSelector.bindTexture(this.vulkanImage);
    }

    void updateSampler() {
        if (vulkanImage == null)
            return;

        byte samplerFlags = magFilter == GL11.GL_LINEAR ? SamplerManager.LINEAR_FILTERING_BIT : 0;

        samplerFlags |= switch (minFilter) {
            case GL11.GL_LINEAR_MIPMAP_LINEAR ->
                    SamplerManager.USE_MIPMAPS_BIT | SamplerManager.MIPMAP_LINEAR_FILTERING_BIT;
            case GL11.GL_NEAREST_MIPMAP_NEAREST -> SamplerManager.USE_MIPMAPS_BIT;
            default -> 0;
        };

        vulkanImage.updateTextureSampler(maxLod, samplerFlags);
    }

    private void uploadImage(ByteBuffer pixels) {
        int width = this.vulkanImage.width;
        int height = this.vulkanImage.height;

        if (internalFormat == GL11.GL_RGB && vulkanImage.format == VK_FORMAT_R8G8B8A8_UNORM) {
            ByteBuffer RGBA_buffer = GlUtil.RGBtoRGBA_buffer(pixels);
            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, RGBA_buffer);
            MemoryUtil.memFree(RGBA_buffer);
        } else
            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, pixels);

    }

    void generateMipmaps() {
        //TODO test
        ImageUtil.generateMipmaps(vulkanImage);
    }

    void setMaxLevel(int l) {
        if (l < 0)
            throw new IllegalStateException("max level cannot be < 0.");

        if (maxLevel != l) {
            maxLevel = l;
            needsUpdate = true;
        }
    }

    void setMaxLod(int l) {
        if (l < 0)
            throw new IllegalStateException("max level cannot be < 0.");

        if (maxLod != l) {
            maxLod = l;
            updateSampler();
        }
    }

    void setMagFilter(int v) {
        switch (v) {
            case GL11.GL_LINEAR, GL11.GL_NEAREST -> {
            }

            default -> throw new IllegalArgumentException("illegal mag filter value: " + v);
        }

        this.magFilter = v;
        updateSampler();
    }

    void setMinFilter(int v) {
        switch (v) {
            case GL11.GL_LINEAR, GL11.GL_NEAREST,
                 GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_NEAREST_MIPMAP_LINEAR,
                 GL11.GL_LINEAR_MIPMAP_NEAREST, GL11.GL_NEAREST_MIPMAP_NEAREST -> {
            }

            default -> throw new IllegalArgumentException("illegal min filter value: " + v);
        }

        this.minFilter = v;
        updateSampler();
    }

    public VulkanImage getVulkanImage() {
        return vulkanImage;
    }

    public void setVulkanImage(VulkanImage vulkanImage) {
        this.vulkanImage = vulkanImage;
    }
}
