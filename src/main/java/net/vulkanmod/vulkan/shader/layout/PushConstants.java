package net.vulkanmod.vulkan.shader.layout;

import java.util.List;

public class PushConstants extends AlignedStruct {

    protected PushConstants(List<Uniform.Info> infoList, int size) {
        super(infoList, size);
    }

}
