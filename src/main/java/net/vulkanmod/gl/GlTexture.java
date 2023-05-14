package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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

    public static void bindTexture(int i) {
        boundTextureId = i;
        boundTexture = map.get(i);

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

        if(width != boundTexture.width || height != boundTexture.height) {
            boundTexture.setParameters(target, level, internalFormat, width, height, border, format, type);
            boundTexture.allocateVulkanImage();
        }

        boundTexture.uploadImage(pixels);
    }

    public static void setVulkanImage(int id, VulkanImage vulkanImage) {
        GlTexture texture = map.get(id);

        texture.vulkanImage = vulkanImage;
    }

    final int id;
    VulkanImage vulkanImage;

    int internalFormat;
    int width;
    int height;
    int border;
    int format;
    int type;

    public GlTexture(int id) {
        this.id = id;
    }

    private void setParameters(int target, int level, int internalFormat, int width, int height, int border, int format, int type) {
        this.internalFormat = internalFormat;
        this.width = width;
        this.height = height;
        this.border = border;
        this.format = format;
        this.type = type;
    }

    private void allocateVulkanImage() {
        if(this.vulkanImage != null)
            this.vulkanImage.free();

        this.vulkanImage = new VulkanImage.Builder(width, height).createVulkanImage();
    }

    private void uploadImage(@Nullable ByteBuffer pixels) {
        if(pixels != null) {
//            if(pixels.remaining() != width * height * 4)
//                throw new IllegalArgumentException("buffer size does not match image size");

            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 4, 0, 0, 0, pixels);
        }
        else {
            pixels = MemoryUtil.memCalloc(width * height * 4);
            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 4, 0, 0, 0, pixels);
            MemoryUtil.memFree(pixels);
        }
    }

}
