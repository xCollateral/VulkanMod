package net.vulkanmod.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Pipeline.createShaderModule;
import static net.vulkanmod.vulkan.Pipeline.pipelineCache;
import static net.vulkanmod.vulkan.ShaderSPIRVUtils.compileShaderFile;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;
import static org.lwjgl.vulkan.VK11.VK_PIPELINE_CREATE_DISPATCH_BASE;

public class ComputePipeline  {

    private final VkDevice device = Vulkan.getDevice();
//    private long ssboStorageBuffer;
    public long compPipeline;
    private final long compdescriptorSetLayout;
    private final long compdescriptorSetPool;
//    private VkExtent2D extent;

    public final long compDescriptorSet;

    public final long compPipelineLayout;
    private long compShaderModule;

    //Compute Pipelines are not interchangeable with Graphics Pipelines, hence why this is a seperate class
    //Is Hardcoded for simplicity; used only for Image Copies ATM
    public ComputePipeline(String path)
    {
        //        this.extent =extent;
//        super(path);
        compdescriptorSetLayout = createDescriptorSetLayout(path);

//        ssboStorageBuffer = getStorageBuffer(size);
//        ssioStorageImage = getStorageImage(extent);

//        pushConstant.addField(Field.createField("float", "ScreenSize", 2, pushConstant));
//        pushConstant.allocateBuffer();
        compPipelineLayout = createPipelineLayout();
        compPipeline = createComputePipeline(path);
        compdescriptorSetPool = createCompDescriptorPool();
        compDescriptorSet = allocateDescriptorSet();
//        imgView = Vulkan.getSwapChainImageViews().get(Drawer.getCurrentFrame());
//        try(MemoryStack stack = MemoryStack.stackPush())
//        {
//            updateDescriptorSets(stack);
//        }


//        vmaMapMemory(Vulkan.getAllocator(), storageBufferMem, data);

    }


    private long createCompDescriptorPool() {

            try(MemoryStack stack = stackPush()) {


                VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(1, stack);
                poolSizes.get(0)
                        .type(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                        .descriptorCount(1);
//                poolSizes.get(1)
//                        .type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
//                        .descriptorCount(1);



                VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
                poolInfo.sType$Default();
                poolInfo.pPoolSizes(poolSizes);
                poolInfo.maxSets(1);
                //poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);

                LongBuffer pDescriptorPool = stack.mallocLong(1);


                    if(vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                        throw new RuntimeException("Failed to create descriptor pool");
                    }
                    return pDescriptorPool.get(0);

            }

    }

    private long createComputePipeline(String path) {

//                SPIRV vertShaderSPIRV = compileShader(path + ".vsh", "vertex");
//                SPIRV fragShaderSPIRV = compileShader(path + ".fsh", "fragment");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ShaderSPIRVUtils.SPIRV compShaderSPIRV = compileShaderFile(path + ".csh", ShaderSPIRVUtils.ShaderKind.COMPUTE_SHADER);

            compShaderModule = createShaderModule(compShaderSPIRV.bytecode());

            ByteBuffer entryPoint = stack.UTF8("main");



            VkPipelineShaderStageCreateInfo compShaderStageInfo = VkPipelineShaderStageCreateInfo.callocStack(stack);
            compShaderStageInfo.sType$Default();
            compShaderStageInfo.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            compShaderStageInfo.module(compShaderModule);
            compShaderStageInfo.pName(entryPoint);



            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.callocStack(1, stack);
            pipelineInfo.sType$Default();
            pipelineInfo.flags(VK_PIPELINE_CREATE_DISPATCH_BASE);
            pipelineInfo.stage(compShaderStageInfo);
            pipelineInfo.layout(compPipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if(vkCreateComputePipelines(device, pipelineCache, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);

        }
    }

    private long createPipelineLayout() {
        try(MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType$Default();
            pipelineLayoutInfo.pSetLayouts(stack.longs(compdescriptorSetLayout));

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            return pPipelineLayout.get(0);
        }
    }

    private long createDescriptorSetLayout(String path) {



        try(MemoryStack stack = stackPush()) {

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.callocStack(1, stack);

            bindings.get(0).binding(0)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);

//            bindings.get(1).binding(1)
//                    .descriptorCount(1)
//                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
//                    .pImmutableSamplers(null)
//                    .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);



            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType$Default();
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if(vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            return pDescriptorSetLayout.get(0);
        }
    }

    private long allocateDescriptorSet() {
        try(MemoryStack stack = stackPush()) {

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType$Default();
            allocInfo.descriptorPool(compdescriptorSetPool);
            allocInfo.pSetLayouts(stack.longs(compdescriptorSetLayout));



            LongBuffer pDescriptorSet = stack.mallocLong(1);

            int result = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet);

            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
            }

            return pDescriptorSet.get(0);
        }
    }

    public void updateDescriptorSets(MemoryStack stack)
    {
//
//        VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.callocStack(1, stack);
//        bufferInfos.offset(0);
//        bufferInfos.range(size);
//        bufferInfos.buffer(ssboStorageBuffer);

        VkDescriptorImageInfo.Buffer imageInfos = VkDescriptorImageInfo.callocStack(1, stack);
        imageInfos.imageView(Vulkan.getSwapChainImageViews().get(Drawer.getCurrentFrame()));
        imageInfos.imageLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        imageInfos.sampler(MemoryUtil.NULL);



        VkWriteDescriptorSet.Buffer ssboDescriptorWrite = VkWriteDescriptorSet.callocStack(1, stack);
        ssboDescriptorWrite.get(0)
                .sType$Default()
                .dstBinding(0)
                .dstArrayElement(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                .descriptorCount(1)
                .pImageInfo(imageInfos)
                .dstSet(compDescriptorSet);
//
//        ssboDescriptorWrite.get(1)
//                .sType$Default()
//                .dstBinding(1)
//                .dstArrayElement(0)
//                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
//                .descriptorCount(1)
//                .pBufferInfo(bufferInfos)
//                .dstSet(compDescriptorSet);



        vkUpdateDescriptorSets(device, ssboDescriptorWrite, null);


    }


    public void close()
    {


//        vmaDestroyBuffer(Vulkan.getAllocator(), storageBuffer, storageBufferMem);
        vkDestroyShaderModule(device, compShaderModule, null);
        vkDestroyDescriptorSetLayout(device, compdescriptorSetLayout, null);
        vkDestroyDescriptorPool(device, compdescriptorSetPool, null);
        vkDestroyPipeline(device, compPipeline, null);
//        vkDestroyPipelineLayout(Vulkan.getDevice(), compPipelineLayout, null);

    }


    public void reload() {
        vkDestroyShaderModule(device, compShaderModule, null);
        vkDestroyPipeline(device, compPipeline, null);
        compPipeline = createComputePipeline("extra/swizzle");
    }

    public void setImage() {
        try(MemoryStack stack = MemoryStack.stackPush())
        {
            updateDescriptorSets(stack);
        }
    }
}
