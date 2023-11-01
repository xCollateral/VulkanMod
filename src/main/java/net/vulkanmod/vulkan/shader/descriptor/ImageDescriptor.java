package net.vulkanmod.vulkan.shader.descriptor;

import static org.lwjgl.vulkan.VK10.*;

public class ImageDescriptor implements Descriptor {

    private final int descriptorType;
    private final int binding;
    public final String qualifier;
    public final String name;
    public final boolean useSampler;

    public ImageDescriptor(int binding, String type, String name) {
        this(binding, type, name, true);
    }

    public ImageDescriptor(int binding, String type, String name, boolean useSampler) {
        this.binding = binding;
        this.qualifier = type;
        this.name = name;
        this.useSampler = useSampler;

        descriptorType = useSampler ? VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER : VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
    }

    @Override
    public int getBinding() {
        return binding;
    }

    @Override
    public int getType() {
        return descriptorType;
    }

    @Override
    public int getStages() {
        return VK_SHADER_STAGE_ALL_GRAPHICS | VK_SHADER_STAGE_COMPUTE_BIT;
    }
}
