package net.vulkanmod.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress0;
import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderLoader
{
    //Add Compile Thread  //Unboxing problems...
    private static final WeakHashMap<Long,Integer> fragNamesHandles =  new WeakHashMap<>(64);
    private static final WeakHashMap<Long,Integer> vertNamesHandles =  new WeakHashMap<>(64);
    private static final HashMap<String, ShaderSPIRVUtils.SPIRV> vkShaderModulesFrag;
    private static final HashMap<String, ShaderSPIRVUtils.SPIRV> vkShaderModulesVert;
    private static final HashMap<String, Integer> supportedStages;
    private static final IntBuffer vkShaderSizesFrag;
    private static final IntBuffer vkShaderSizesVert;
    private static final ArrayList<String> FRNames;
//    private static final ArrayList<ShaderSPIRVUtils.SPIRV> SPIRVSet;
    static final String defDir = ("compiled/Generated");
    private static final boolean defDirs = new File(defDir+"/frag/").mkdirs();
    private static final boolean defDirs2 = new File(defDir+"/vert/").mkdirs();
    private static int idx;
    private static int fragOff;
    private static int vertOff;

    static
    {
        //Laoded names corspond to the number of Distinct pipelines Created
        //Make sure erroring/Missing/Failed shaders are not added Accidentally
        //Enum Root COmpiled ShadreNames Dir

        try(Stream<Path> frNames = Files.walk(Path.of(defDir + "/frag/"), 1))
        {
            FRNames = new ArrayList<>(0);
//        int i = 0;
            frNames.forEach(name ->
                    FRNames.add(name.getFileName().toString()));

            FRNames.remove(0); //remove Dummy Base Dir Index
            System.out.println(FRNames);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        FRNames2 = Files.walk(defDir, 1);
//        System.out.println(Arrays.toString(frNames.toArray()));
//        List<Path> ax= FRNames.toList();

        vkShaderModulesVert = new HashMap<>(FRNames.size());
        vkShaderModulesFrag = new HashMap<>(FRNames.size());
        supportedStages = new HashMap<>(FRNames.size());
        vkShaderSizesVert = IntBuffer.allocate(FRNames.size()*2);
        vkShaderSizesFrag = IntBuffer.allocate(FRNames.size()*2);
//        SPIRVSet = new ArrayList<>();
        for (String frName : FRNames) {
            System.out.println(frName);
            loadShaderFile(frName);
//            loadShaderFile(frName.toString());
        }
//        vkShaderModulesVert.rewind();
//        vkShaderModulesFrag.rewind();
        vkShaderSizesVert.rewind();
        vkShaderSizesFrag.rewind();




    }
//    static void loadShaderNames() { //Laoded names corspond to the number of Distinct pipelines Created
//        //Make sure erroring/Missing/Failed shaders are not added Accidentally
//        //Enum Root COmpiled ShadreNames Dir
//
//        try(Stream<Path> frNames = Files.walk(Path.of(defDir + "/frag/"), 1))
//        {
//            FRNames = new ArrayList<>(0);
////        int i = 0;
//            frNames.forEach(name ->
//                    FRNames.add(name.getFileName().toString()));
//
//            FRNames.remove(0); //remove Dummy Base Dir Index
//            System.out.println(FRNames);
//
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
////        FRNames2 = Files.walk(defDir, 1);
////        System.out.println(Arrays.toString(frNames.toArray()));
////        List<Path> ax= FRNames.toList();
//
//        for (String frName : FRNames) {
//            System.out.println(frName);
//            loadShaderFile(frName);
////            loadShaderFile(frName.toString());
//        }
//
//
//    }
    //this could prob be cleaned to look nicer
    public static void loadShaderFile(String name){
//        String inputStream = (ShaderLoader.defDir+name +".fsh" + ".spv");
//        String inputStream2 = (ShaderLoader.defDir+name + "vsh" + ".spv");

//ideally nd to enumeate shaders types that all match teh same name (and not with a shared itemName Index 9e.g  shaderSet A has Frag and Vert stages but ShaderSetB has Frag,Vert and Geom Stages)
        var extracted = bufferShaders(defDir + "/frag/"+name);
        var extracted1 = bufferShaders( defDir + "/vert/"+name);
        if(extracted==null ||extracted1==null) //try to Skip /*Failing*//Missing Shaders
        {
            return;
        }
//        long value = memAddress0(extracted);
//        long value1 = memAddress0(extracted1);
//        int capacity = extracted.capacity();
//        int capacity1 = extracted1.capacity();
        vkShaderModulesFrag.put(name, extracted);
        vkShaderModulesVert.put(name, extracted1);
//        vkShaderSizesFrag.put(capacity);
//        vkShaderSizesVert.put(capacity1);
        vertOff++;
        fragOff++;
//        SPIRVSet.add(new ShaderSPIRVUtils.SPIRV(value, capacity));
//        SPIRVSet.add(new ShaderSPIRVUtils.SPIRV(value1, capacity1));
        idx+=2;
//        throw new RuntimeException("unable to read inputStream");
    }

    private static ShaderSPIRVUtils.SPIRV bufferShaders(String name) {
        try(final FileInputStream fileInputStream = new FileInputStream(name))
        {
            byte[] bytes = fileInputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlignedAlloc(Integer.BYTES, bytes.length); //Must be in Little Endian to be Loaded Correctly
            buffer.put(bytes);
            int capacity = buffer.capacity();
            long handle = createShaderModule(memAddress0(buffer), capacity);

            return new ShaderSPIRVUtils.SPIRV(handle, capacity);

//            buffer.rewind();
//            if (bytes.length==0)
//            {
//                throw new RuntimeException();
//            }
//            inputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

            return pShaderModule.get(0);
        }
    }

    public static void addShadername(String name)
    {
        loadShaderFile(name);
    }

//    public static long loadDirectFragHandle(String name)
//    {
//
//    }
    public static long loadDirectHandle(String name, ShaderSPIRVUtils.ShaderKind vertexShader)
    {
        int integer = supportedStages.get(name);
        if((integer | vertexShader.kind)!=integer)
        {
            throw new RuntimeException();
        }
        return switch (vertexShader)
        {
            case VERTEX_SHADER -> vkShaderModulesVert.get(name).handle();
            case GEOMETRY_SHADER, COMPUTER_SHADER, MESH_SHADER, RAYGEN_SHADER, ANYHIT_SHADER, MISS_SHADER, INTERSECTION_SHADER -> 0L;
            case FRAGMENT_SHADER -> vkShaderModulesFrag.get(name).handle();
        };
    }
}
