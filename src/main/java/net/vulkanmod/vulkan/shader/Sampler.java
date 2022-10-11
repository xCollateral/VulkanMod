package net.vulkanmod.vulkan.shader;

import org.lwjgl.vulkan.VK10;

public record Sampler(String name, int binding, String type, String stage)
{
    public int getType()
    {
        return switch (type)
                {
//                    case "uniform" -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
                    case "texture" -> VK10.VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE; //Probably Wrong Flags
                    case "combined" -> VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
                    default -> VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
                };
    }
    public int getStage()
    {
        return switch (stage)
                {
                    case "vertex" -> VK10.VK_SHADER_STAGE_VERTEX_BIT;
                    case "fragment" -> VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
                    default -> VK10.VK_SHADER_STAGE_ALL_GRAPHICS;
                };
    }
}
