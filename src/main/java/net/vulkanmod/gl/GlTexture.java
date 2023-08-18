package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;

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
        if(i == -1)
            return;

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

    public static void setVulkanImage(int id, VulkanImage vulkanImage) {
        GlTexture texture = map.get(id);

        texture.vulkanImage = vulkanImage;
    }

    public static int texLevelParameter(int target, int level, int pname) {

        if (target != GL11.GL_TEXTURE_2D || boundTexture == null) {
            // TODO: other targets are not implemented
            return 0;
        }


        if (pname == GL11.GL_TEXTURE_INTERNAL_FORMAT) {
            return boundTexture.vulkanImage.getFormat().glFormat();
        }
        if (pname == GL11.GL_TEXTURE_WIDTH) {
            return boundTexture.vulkanImage.width;
        }
        if (pname == GL11.GL_TEXTURE_HEIGHT) {
            return boundTexture.vulkanImage.height;
        }

        // TODO: pname is one of:
            // GL12#GL_TEXTURE_DEPTH
            // GL32#GL_TEXTURE_SAMPLES
            // GL32#GL_TEXTURE_FIXED_SAMPLE_LOCATIONS
            // GL11C#GL_TEXTURE_INTERNAL_FORMAT
            // GL11C#GL_TEXTURE_RED_SIZE
            // GL11C#GL_TEXTURE_GREEN_SIZE
            // GL11C#GL_TEXTURE_BLUE_SIZE
            // GL11C#GL_TEXTURE_ALPHA_SIZE
            // GL14#GL_TEXTURE_DEPTH_SIZE
            // GL30#GL_TEXTURE_STENCIL_SIZE
            // GL30#GL_TEXTURE_SHARED_SIZE
            // GL30#GL_TEXTURE_ALPHA_TYPE
            // GL30#GL_TEXTURE_DEPTH_TYPE
            // GL13#GL_TEXTURE_COMPRESSED
            // GL13#GL_TEXTURE_COMPRESSED_IMAGE_SIZE
            // GL31#GL_TEXTURE_BUFFER_DATA_STORE_BINDING
            // GL43#GL_TEXTURE_BUFFER_OFFSET
            // GL43#GL_TEXTURE_BUFFER_SIZE

        return 0;
    }

    final int id;
    VulkanImage vulkanImage;

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
//            if(pixels.remaining() != width * height * 4)
//                throw new IllegalArgumentException("buffer size does not match image size");

            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, pixels);
        }
        else {
            pixels = MemoryUtil.memCalloc(width * height * 4);
            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, pixels);
            MemoryUtil.memFree(pixels);
        }
    }

    private static int vulkanFormat(int glFormat, int type) {
        return switch (glFormat) {
            case 6408 ->
                    switch (type) {
                        case 5121 -> VK_FORMAT_R8G8B8A8_UNORM;
                        default -> throw new IllegalStateException("Unexpected value: " + type);
                    };

            default -> throw new IllegalStateException("Unexpected value: " + glFormat);
        };
    }

}
