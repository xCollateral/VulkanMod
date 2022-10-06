package net.vulkanmod.vulkan;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.Shaderc;

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

    public static SPIRV compileShaderFile(String shaderFile, String outFile, ShaderKind shaderKind) {
        String shaderStage = shaderKind.kind == ShaderKind.VERTEX_SHADER.kind ? ".vsh" : ".fsh";
        String path = ShaderSPIRVUtils.class.getResource("/assets/vulkanmod/shaders/" + shaderFile+shaderStage).toExternalForm();
        return compileShaderAbsoluteFile(path, shaderKind, outFile, shaderStage);
    }

    public static SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind, String outFile, String shaderStage) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, shaderStage, outFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SPIRV compileShader(String filename, String shaderStage, String outFile, String source, ShaderKind shaderKind) throws IOException {
//        System.out.println(filename);
        String pathname = outFile + shaderStage + ".spv";
//        File fileOutputStream = new File(pathname);
//        boolean overWrite =  (!fileOutputStream.exists())? !fileOutputStream.createNewFile(): fileOutputStream.canWrite();
//        if(overWrite)
//            {
            long compiler = shaderc_compiler_initialize();

            if(compiler == NULL) {
                throw new RuntimeException("Failed to create shader compiler");
            }

            long options = shaderc_compile_options_initialize();

            if(options == NULL) {
                throw new RuntimeException("Failed to create compiler options");
            }

            Shaderc.shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_1);
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
//            if(bytecode==null)
//            {
//                throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + shaderc_result_get_error_message(result));
//            }

            try(final DataOutputStream dataOutputStreamD = new DataOutputStream(new FileOutputStream(pathname))) {
//                fileOutputStream.createNewFile();


//                DataOutputStream fileOutputStreamD = new DataOutputStream(new FileOutputStream(fileOutputStream));
                for (int i = 0; i < (bytecode).capacity(); i++) {
                    dataOutputStreamD.writeByte(bytecode.get(i));
                }
//                System.out.println(fileOutputStream.getAbsolutePath());
            }
            return new SPIRV(memAddress0(bytecode), bytecode.capacity());
//        }
//        else {
//           return readFromStream(/*new FileInputStream*/(pathname));
//        }
    }

    private static SPIRV readFromStream(String inputStream) {

        try(final FileInputStream fileInputStream = new FileInputStream(inputStream)) {
            byte[] bytes = fileInputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlignedAlloc(Integer.BYTES, bytes.length); //Must be in Little Endian to be Loaded Correctly
            buffer.put(bytes);
//            buffer.rewind();
//            if (bytes.length==0)
//            {
//                throw new RuntimeException();
//            }
//            inputStream.close();

            return new SPIRV(MemoryUtil.memAddress0(buffer), buffer.capacity());
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("unable to read inputStream");
    }

    public static SPIRV loadShaderFile(String name, ShaderKind vertexShader){
        return readFromStream((name + (vertexShader.kind==ShaderKind.VERTEX_SHADER.kind ? ".vsh" : ".fsh") + ".spv"));
    }

    public enum ShaderKind {

        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }
    //don;t cal this if a bad/failed handle as a result of failed Alloctaion?COmpialtion toa void Unessacery Heap Fragmentation/Alloctaions amdtcistaios eg. e.tc i.e. /.MSIc .e.rerer.eeree
    public record SPIRV(long handle, int size) implements NativeResource {

//        public final long handle;
//        public int size;
////        private ByteBuffer bytecode;
//
//        public SPIRV(long handle, int bytecode) {
//            this.handle = handle;
//            this.size = bytecode;
//        }

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