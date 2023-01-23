package net.vulkanmod.vulkan;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class ShaderSPIRVUtils {
    private static long compiler;
    private static final int vkEnv;
    private static final int spvVer;

    static
    {
        var vkCapabilitiesDevice = Vulkan.getDevice().getCapabilities();
        if(vkCapabilitiesDevice.Vulkan13 & vkCapabilitiesDevice.Vulkan12 & vkCapabilitiesDevice.Vulkan11)
        {
            vkEnv =shaderc_env_version_vulkan_1_3;
            spvVer=shaderc_spirv_version_1_6;
        }
        else if(!vkCapabilitiesDevice.Vulkan13 & vkCapabilitiesDevice.Vulkan12 &  vkCapabilitiesDevice.Vulkan11)
        {
            vkEnv =shaderc_env_version_vulkan_1_2;
            spvVer=shaderc_spirv_version_1_5;
        }else
        {
            vkEnv =shaderc_env_version_vulkan_1_1;
            spvVer=shaderc_spirv_version_1_3;
        }
    }

    public static SPIRV compileShaderFile(String shaderFile, ShaderKind shaderKind) {
        String path = ShaderSPIRVUtils.class.getResource("/assets/vulkanmod/shaders/" + shaderFile).toExternalForm();
        return compileShaderAbsoluteFile(path, shaderKind);
    }

    public static SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {

        if(compiler == 0) compiler = shaderc_compiler_initialize();

        if(compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        long options = shaderc_compile_options_initialize();

        if(options == NULL) {
            throw new RuntimeException("Failed to create compiler options");
        }


        shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, vkEnv);
        shaderc_compile_options_set_target_spirv(options, shaderKind == ShaderKind.COMPUTE_SHADER && vkEnv == shaderc_env_version_vulkan_1_3 ? shaderc_spirv_version_1_5 : spvVer);
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

        if(result == NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }

        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + shaderc_result_get_error_message(result));
        }

//        shaderc_compiler_release(compiler);

        return new SPIRV(result, shaderc_result_get_bytes(result));
    }

    private static SPIRV readFromStream(InputStream inputStream) {

        try {
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.position(0);

            return new SPIRV(MemoryUtil.memAddress(buffer), buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("unable to read inputStream");
    }

    public enum ShaderKind {

        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        COMPUTE_SHADER(shaderc_glsl_compute_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }

    public static final class SPIRV implements NativeResource {

        private final long handle;
        private ByteBuffer bytecode;

        public SPIRV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public void free() {
//            shaderc_result_release(handle);
            bytecode = null; // Help the GC
        }
    }

}