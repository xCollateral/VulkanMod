package net.vulkanmod.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderLoader
{
    private static final HashMap<String, Long> vkShaderModulesFrag;
    private static final HashMap<String, Long> vkShaderModulesVert;
    private static final HashMap<String, Integer> supportedStages;
//    private static final ArrayList<String> shaderNames;
    private static final ArrayList<Path> shaderFiles;
    private static final String defDir = ("compiled/Generated");

    static
    {
        //Loaded names correspond to the number of Distinct pipelines Created
        //Make sure erroring/Missing/Failed shaders are not added Accidentally
        //Enum Root COmpiled ShadreNames Dir

        try(Stream<Path> frNames = Files.walk(Path.of(defDir), 1))
        {
            shaderFiles = new ArrayList<>(0);
            frNames.forEach(shaderFiles::add);

            shaderFiles.remove(0); //remove Dummy Base Dir Index
//            System.out.println(shaderFiles);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        FRNames2 = Files.walk(defDir, 1);
//        System.out.println(Arrays.toString(frNames.toArray()));
//        List<Path> ax= FRNames.toList();

        vkShaderModulesVert = new HashMap<>(shaderFiles.size() >> 1);
        vkShaderModulesFrag = new HashMap<>(shaderFiles.size() >> 1);
        supportedStages = new HashMap<>(shaderFiles.size() >> 1);

        //        SPIRVSet = new ArrayList<>();
        for (Path frName : shaderFiles) {
//            System.out.println(frName);
            loadShaderFile(frName);
//            loadShaderFile(frName.toString());
        }
//        vkShaderModulesVert.rewind();
//        vkShaderModulesFrag.rewind();




    }

    private static ShaderSPIRVUtils.ShaderKind stageOf(String name) {
        return switch(name.substring(name.length()-3))
                {
                    case "vsh" -> ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
                    case "fsh" -> ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
                    case "gsh" -> ShaderSPIRVUtils.ShaderKind.GEOMETRY_SHADER;
                    default -> throw new IllegalStateException("Unexpected value: " + (name));
                };
    }
    public static void loadShaderFile(Path name)
    {

        long extracted = bufferShaders(name.toString());
        //        long value = memAddress0(extracted);

        final String key = name.getFileName().toString();
        final String substring = key.substring(0, key.length() - 4);
        switch(stageOf(name.toString()))
        {
            case VERTEX_SHADER -> vkShaderModulesVert.put(substring, extracted);
            case FRAGMENT_SHADER -> vkShaderModulesFrag.put(substring, extracted);
            default -> throw new IllegalStateException("Unexpected value: " + (name));
        }


    }

    private static long bufferShaders(String name) {
        try(final FileInputStream fileInputStream = new FileInputStream(name))
        {
            byte[] bytes = fileInputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlignedAlloc(Integer.BYTES, bytes.length); //Must be in Little Endian to be Loaded Correctly
            buffer.put(bytes);
            int capacity = buffer.capacity();

            return createShaderModule(memAddress0(buffer), capacity);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        return 0L;
    }

    public static long createShaderModule(long spirvCode, int size) {

        try(MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo vkShaderModuleCreateInfo = VkShaderModuleCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            long struct = vkShaderModuleCreateInfo.address();
            memPutAddress(struct + VkShaderModuleCreateInfo.PCODE, (spirvCode));
            VkShaderModuleCreateInfo.ncodeSize(struct, size);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(Vulkan.getDevice(), vkShaderModuleCreateInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }
            MemoryUtil.nmemAlignedFree(spirvCode);
            return pShaderModule.get(0);
        }
    }

    public static void addCustomPipeLineShaderSet(Path name)
    {
        loadShaderFile(name);
    }

//    public static long loadDirectFragHandle(String name)
//    {
//
//    }
    public static long loadDirectHandle(String name, ShaderSPIRVUtils.ShaderKind vertexShader)
    {
//        int integer = supportedStages.get(name);
//        if((integer | vertexShader.kind)!=integer)
//        {
//            throw new RuntimeException();
//        }
        return switch (vertexShader)
        {
            case VERTEX_SHADER -> vkShaderModulesVert.get(name);
            case CLOSESTHIT_SHADER, GEOMETRY_SHADER, COMPUTER_SHADER, MESH_SHADER, RAYGEN_SHADER, ANYHIT_SHADER, MISS_SHADER, INTERSECTION_SHADER -> 0L;
            case FRAGMENT_SHADER -> vkShaderModulesFrag.get(name);
        };
    }
}
