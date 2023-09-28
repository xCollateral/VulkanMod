package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import org.apache.commons.lang3.Validate;

public class Vec2f extends Field {

    public Vec2f(FieldInfo fieldInfo) {
        super(fieldInfo);
    }

    void setSupplier() {
        this.values = Uniforms.vec2f_uniformMap.get(this.fieldInfo.name);

        Validate.notNull(this.values, "Field name not found: " + this.fieldInfo.name);
    }

}
