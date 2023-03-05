package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.ArrayList;
import java.util.List;

public abstract class AlignedStruct {

    protected MappedBuffer buffer;
    protected List<Field<?>> fields = new ArrayList<>();
    protected int size;

    protected AlignedStruct(List<Field.FieldInfo> infoList, int size) {
        this.size = size;
        this.buffer = new MappedBuffer(size);

        for(Field.FieldInfo fieldInfo : infoList) {

            Field<?> field = Field.createField(fieldInfo, this.buffer.ptr);
            this.addField(field);
        }
    }

    public void addField(Field<?> field) {
        this.fields.add(field);
    }

    public void update() {
        for(Field<?> field : this.fields) {
            field.update();
        }

    }

    public long getBufferPtr() {
        return this.buffer.ptr;
    }

    public int getSize() {
        return size;
    }

    public static class Builder {

        final List<Field.FieldInfo> fields = new ArrayList<>();
        protected int currentOffset = 0;

        public void addFieldInfo(String type, String name, int count) {
            Field.FieldInfo fieldInfo = Field.createFieldInfo(type, name, count);

            this.currentOffset = fieldInfo.computeAlignmentOffset(this.currentOffset);
            this.currentOffset += fieldInfo.size;
            this.fields.add(fieldInfo);
        }

        public UBO buildUBO(int binding, int type) {
            //offset is expressed in floats/ints
            return new UBO(binding, type, this.currentOffset * 4, this.fields);
        }

        public PushConstants buildPushConstant() {
            return new PushConstants(this.fields, this.currentOffset * 4);
        }

    }

}
