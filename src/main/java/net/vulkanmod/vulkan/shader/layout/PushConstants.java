package net.vulkanmod.vulkan.shader.layout;

import java.util.List;

public class PushConstants extends AlignedStruct {

    private final int stage;

    protected PushConstants(List<Uniform.Info> infoList, int size, int stage) {
        super(infoList, size);
        this.stage = stage;
    }

    public int getStage() {
        return stage;
    }

}
