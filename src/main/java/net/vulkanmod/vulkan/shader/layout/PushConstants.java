package net.vulkanmod.vulkan.shader.layout;

import java.util.List;

public class PushConstants extends AlignedStruct {

    protected PushConstants(List<Field.FieldInfo> infoList, int size) {
        super(infoList, size);
    }

    public long getAddress() {
        return buffer.ptr;
    }

}
