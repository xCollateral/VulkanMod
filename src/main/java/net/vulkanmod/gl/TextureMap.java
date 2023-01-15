package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.texture.VulkanImage;

public class TextureMap {

    static Int2ReferenceOpenHashMap<VulkanImage> textures = new Int2ReferenceOpenHashMap<>();

    public static void addTexture(VulkanImage vulkanImage) {
        textures.put(getId(vulkanImage), vulkanImage);
    }

    public static VulkanImage getTexture(int id) {
        return textures.get(id);
    }

    public static boolean removeTexture(int id) { return textures.remove(id) != null; }

    public static int getId(VulkanImage vulkanImage) {
        return (int) vulkanImage.getId();
    }
}
