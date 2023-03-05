package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.List;

public class PushConstants extends AlignedStruct {

    protected PushConstants(List<Field.FieldInfo> infoList, int size) {
        super(infoList, size);
    }

    public long getAddress() {
        return buffer.ptr;
    }

}
