package net.vulkanmod.vulkan.shader.descriptor;

import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;

import static org.lwjgl.vulkan.VK10.*;

public class ImageDescriptor implements Descriptor {

    private final int descriptorType;
    private final int binding;
    public final String qualifier;
    public final String name;
    public final int imageIdx;

    public final boolean isStorageImage;
    private final int stage;
    public boolean useSampler;
    public boolean isReadOnlyLayout;
    private int layout;
    private int mipLevel = -1;

    public ImageDescriptor(String type, String name, int imageIdx) {
        this(type, name, imageIdx, false);
    }

    public ImageDescriptor(String type, String name, int imageIdx, boolean isStorageImage) {
        this.qualifier = type;
        this.name = name;
        this.isStorageImage = isStorageImage;
        this.useSampler = !isStorageImage;
        this.imageIdx = imageIdx;
        this.stage = switch (name) {
            case "Sampler0", "DiffuseSampler", "SamplerProj" -> VK_SHADER_STAGE_FRAGMENT_BIT;
            case "Sampler1", "Sampler2" -> VK_SHADER_STAGE_VERTEX_BIT;
            default -> VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
        };
        this.binding = this.stage == VK_SHADER_STAGE_VERTEX_BIT ? DescriptorSetArray.VERTEX_SAMPLER_ID : DescriptorSetArray.FRAG_SAMPLER_ID;

        //TODO: Check if STROAGE and Samplers can Alias in the same BINDING Slot, or if a dedicated Stroage BINDING SLOID ID Is needed
        descriptorType = isStorageImage ? VK_DESCRIPTOR_TYPE_STORAGE_IMAGE : VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        setLayout(isStorageImage ? VK_IMAGE_LAYOUT_GENERAL : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
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
        return stage;
    }

    public void setLayout(int layout) {
        this.layout = layout;
        this.isReadOnlyLayout = layout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    }

    public int getLayout() {
        return layout;
    }

    public void setMipLevel(int mipLevel) {
        this.mipLevel = mipLevel;
    }

    public int getMipLevel() {
        return mipLevel;
    }

//    public VulkanImage getImage() {
//        return VTextureSelector.getImage(this.imageIdx);
//    }

    public long getImageView(VulkanImage image) {
        long view;

        if(mipLevel == -1)
            view = image.getImageView();
        else
            view = image.getLevelImageView(mipLevel);

        return view;
    }

    public static class State {
        long imageView, sampler;

        public State(long imageView, long sampler) {
            set(imageView, sampler);
        }

        public void set(long imageView, long sampler) {
            this.imageView = imageView;
            this.sampler = sampler;
        }

        public boolean isCurrentState(long imageView, long sampler) {
            return this.imageView == imageView && this.sampler == sampler;
        }

    }
}
