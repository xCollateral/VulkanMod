package net.vulkanmod.gl;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GlTexture {
    private static int ID_COUNT = 0;
    private static final Int2ReferenceOpenHashMap<GlTexture> map = new Int2ReferenceOpenHashMap<>();
    private static int boundTextureId = 0;
    private static GlTexture boundTexture;

    public static int genTextureId() {
        int id = ID_COUNT;
        map.put(id, new GlTexture(id));
        ID_COUNT++;
        return id;
    }

    public static void bindTexture(int id) {
        if(id == -1)
            return;

        boundTextureId = id;
        boundTexture = map.get(id);

        if(boundTexture == null)
            throw new NullPointerException("bound texture is null");

        VulkanImage vulkanImage = boundTexture.vulkanImage;
        if(vulkanImage != null)
            VTextureSelector.bindTexture(vulkanImage);
    }

    public static void glDeleteTextures(int i) {
        map.remove(i);
    }

    public static GlTexture getTexture(int id) {
        return map.get(id);
    }

    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        if(width == 0 || height == 0)
            return;

        boundTexture.internalFormat = internalFormat;

        if(width != boundTexture.vulkanImage.width || height != boundTexture.vulkanImage.height || vulkanFormat(format, type) != boundTexture.vulkanImage.format) {
            boundTexture.allocateVulkanImage(width, height);
        }

        boundTexture.uploadImage(pixels);
    }

    public static void texSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, @Nullable ByteBuffer pixels) {
        if(width == 0 || height == 0)
            return;

        VTextureSelector.uploadSubTexture(level, width, height, xOffset, yOffset,0, 0, width, pixels);
    }

    public static int getTexLevelParameter(int target, int level, int pName) {
        if(boundTexture == null || target == GL11.GL_TEXTURE_2D)
            return -1;

        return switch (pName) {
            case GL11.GL_TEXTURE_INTERNAL_FORMAT -> getGlFormat(boundTexture.vulkanImage.format);
            case GL11.GL_TEXTURE_WIDTH -> boundTexture.vulkanImage.width;
            case GL11.GL_TEXTURE_HEIGHT -> boundTexture.vulkanImage.height;

            default -> -1;
        };
    }

    public static void setVulkanImage(int id, VulkanImage vulkanImage) {
        GlTexture texture = map.get(id);

        texture.vulkanImage = vulkanImage;
    }

    public static GlTexture getBoundTexture() {
        return boundTexture;
    }

    final int id;
    VulkanImage vulkanImage;
    int internalFormat;

    public GlTexture(int id) {
        this.id = id;
    }

    private void allocateVulkanImage(int width, int height) {
        if(this.vulkanImage != null)
            this.vulkanImage.free();

        this.vulkanImage = new VulkanImage.Builder(width, height).createVulkanImage();
        VTextureSelector.bindTexture(this.vulkanImage);
    }

    private void uploadImage(@Nullable ByteBuffer pixels) {
        int width = this.vulkanImage.width;
        int height = this.vulkanImage.height;

        if(pixels != null) {

            if(internalFormat == GL11.GL_RGB && vulkanImage.format == VK_FORMAT_R8G8B8A8_UNORM) {

                ByteBuffer RGBA_buffer = Util.RGBtoRGBA_buffer(pixels);
                this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, RGBA_buffer);
                MemoryUtil.memFree(RGBA_buffer);

                return;
            }

            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, pixels);
        }
        else {
            pixels = MemoryUtil.memCalloc(width * height * 4);
            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, pixels);
            MemoryUtil.memFree(pixels);
        }
    }

    public VulkanImage getVulkanImage() {
        return vulkanImage;
    }

    public void setVulkanImage(VulkanImage vulkanImage) {
        this.vulkanImage = vulkanImage;
    }

    private static int vulkanFormat(int glFormat, int type) {
        return switch (glFormat) {
            case GL11.GL_RGBA ->
                    switch (type) {
                        case GL11.GL_UNSIGNED_BYTE -> VK_FORMAT_R8G8B8A8_UNORM;
                        default -> throw new IllegalStateException("Unexpected value: " + type);
                    };
            case GL11.GL_RED ->
                    switch (type) {
                        case GL11.GL_UNSIGNED_BYTE -> VK_FORMAT_R8_UNORM;
                        default -> throw new IllegalStateException("Unexpected value: " + type);
                    };

            default -> throw new IllegalStateException("Unexpected value: " + glFormat);
        };
    }

    public static int getGlFormat(int vFormat) {
        return switch (vFormat) {
            case VK_FORMAT_R8G8B8A8_UNORM -> GL11.GL_RGBA;
            case VK_FORMAT_R8_UNORM -> GL11.GL_RED;
            default -> throw new IllegalStateException("Unexpected value: " + vFormat);
        };
    }

}
