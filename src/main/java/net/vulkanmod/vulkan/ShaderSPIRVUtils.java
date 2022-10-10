package net.vulkanmod.vulkan;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress0;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class ShaderSPIRVUtils {

    public static long compileShaderFile(String shaderFile, String outFile, ShaderKind shaderKind) {
        String shaderStage = shaderKind.kind == ShaderKind.VERTEX_SHADER.kind ? ".vsh" : ".fsh";
        String path = ShaderSPIRVUtils.class.getResource("/assets/vulkanmod/shaders/" + shaderFile+shaderStage).toExternalForm();
        return compileShaderAbsoluteFile(path, shaderStage, shaderKind, ShaderLoader.defDir+outFile);
    }

    public static long compileShaderAbsoluteFile(String shaderFile, String shaderStage, ShaderKind shaderKind, String outFile) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, outFile, shaderStage, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static long compileShader(String filename, String outFile, String shaderStage, String source, ShaderKind shaderKind) throws IOException {
        String pathname = outFile + shaderStage;// + ".spv";

            long compiler = shaderc_compiler_initialize();

            if(compiler == NULL) {
                throw new RuntimeException("Failed to create shader compiler");
            }

            long options = shaderc_compile_options_initialize();

            if(options == NULL) {
                throw new RuntimeException("Failed to create compiler options");
            }

        Shaderc.shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, VK10.VK_VERSION_MAJOR(Vulkan.vkRawVersion) << 22 | VK10.VK_VERSION_MINOR(Vulkan.vkRawVersion) << 12);
//            Shaderc.shaderc_compile_options_set_target_spirv(options, shaderc_spirv_version_1_0);
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

            long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

            if(result == NULL) {
                throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
            }

            if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + shaderc_result_get_error_message(result));
            }

            shaderc_compiler_release(compiler);


            ByteBuffer bytecode =  shaderc_result_get_bytes(result);

            try(final DataOutputStream dataOutputStreamD = new DataOutputStream(new FileOutputStream(pathname))) {

                for (int i = 0; i < (bytecode).capacity(); i++) {
                    dataOutputStreamD.writeByte(bytecode.get(i));
                }

            }
            return ShaderLoader.createShaderModule(memAddress0(bytecode), bytecode.capacity());

    }



    public enum ShaderKind {

        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

        final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }
    public record SPIRV(long handle, int size) implements NativeResource {


        public long bytecode() {
            return handle;
        }

        @Override
        public void free() {
            shaderc_result_release(handle);
            MemoryUtil.nmemAlignedFree(handle);
//            bytecode = null; // Help the GC
        }
    }

}