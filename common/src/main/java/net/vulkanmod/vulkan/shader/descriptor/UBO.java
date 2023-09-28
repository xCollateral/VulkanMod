package net.vulkanmod.vulkan.shader.descriptor;

import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.Field;

import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;

public class UBO extends AlignedStruct implements Descriptor {
    private final int binding;
    private final int stages;

    public UBO(int binding, int stages, int size, List<Field.FieldInfo> infoList) {
        super(infoList, size);
        this.binding = binding;
        this.stages = stages;
    }

    public int getBinding() {
        return binding;
    }

    @Override
    public int getType() {
        return VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
    }

    public int getStages() {
        return stages;
    }

}
