package net.vulkanmod.gl;

import net.vulkanmod.vulkan.shader.SPIRVUtils;

public class Util {

    public static SPIRVUtils.ShaderKind extToShaderKind(String in) {
        return switch (in) {
            case ".vsh" -> SPIRVUtils.ShaderKind.VERTEX_SHADER;
            case ".fsh" -> SPIRVUtils.ShaderKind.FRAGMENT_SHADER;
            default -> throw new RuntimeException("unknown shader type: " + in);
        };
    }
}
