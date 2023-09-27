package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import org.apache.commons.lang3.Validate;

public class Mat4f extends Field {
    public Mat4f(FieldInfo info) {
        super(info);
    }

    protected void setSupplier() {
        this.values = Uniforms.mat4f_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.values, "Field name not found: " + this.fieldInfo.name);
    }

}
