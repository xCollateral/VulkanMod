package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.ArrayList;
import java.util.List;

public abstract class AlignedStruct {

    protected List<Field> fields = new ArrayList<>();
    protected int size;

    protected AlignedStruct(List<Field.FieldInfo> infoList, int size) {
        this.size = size;

        if(infoList == null)
            return;

        for(Field.FieldInfo fieldInfo : infoList) {

            Field field = Field.createField(fieldInfo);
            this.fields.add(field);
        }
    }

    public void update(long ptr) {
        for(Field field : this.fields) {
            field.update(ptr);
        }
    }

    public List<Field> getFields() {
        return this.fields;
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

        public void addFieldInfo(String type, String name) {
            Field.FieldInfo fieldInfo = Field.createFieldInfo(type, name);

            this.currentOffset = fieldInfo.computeAlignmentOffset(this.currentOffset);
            this.currentOffset += fieldInfo.size;
            this.fields.add(fieldInfo);
        }

        public UBO buildUBO(int binding, int stages) {
            //offset is expressed in floats/ints
            return new UBO(binding, stages, this.currentOffset * 4, this.fields);
        }

        public PushConstants buildPushConstant() {
            if(this.fields.isEmpty()) return null;
            return new PushConstants(this.fields, this.currentOffset * 4);
        }

    }

}
