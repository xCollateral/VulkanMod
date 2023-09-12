package net.vulkanmod.gl;

import net.vulkanmod.vulkan.shader.ShaderSPIRVUtils;

public class Util {

    public static ShaderSPIRVUtils.ShaderKind extToShaderKind(String in) {
        return switch (in) {
            case ".vsh" -> ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
            case ".fsh" -> ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
            default -> throw new RuntimeException("unknown shader type: " + in);
        };
    }
}
