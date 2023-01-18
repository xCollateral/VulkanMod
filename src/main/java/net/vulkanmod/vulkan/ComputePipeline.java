package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.shader.Field;
import net.vulkanmod.vulkan.shader.PushConstant;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
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

public class ComputePipeline  {

    public final long compPipeline;
    private final long compdescriptorSetLayout;
    private final long compdescriptorSetPool;

    public final long storageBuffer;
    public final long compDescriptorSet;
    private long storageBufferMem;
    public final long compPipelineLayout;

//    private final UBO ubo = new UBO(0, VK_SHADER_STAGE_COMPUTE_BIT);
    public final PushConstant pushConstant = new PushConstant();


    public ComputePipeline(String path)
    {
//        super(path);
        compdescriptorSetLayout = createDescriptorSetLayout(path);

        storageBuffer = getStorageBuffer(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT | VMA_ALLOCATION_CREATE_MAPPED_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        pushConstant.addField(Field.createField("float", "ScreenSize", 2, pushConstant));
        pushConstant.allocateBuffer();
        compPipelineLayout = createPipelineLayout();
        compPipeline = createComputePipeline(path);
        compdescriptorSetPool = createCompDescriptorPool();
        compDescriptorSet = allocateDescriptorSet();


    }

    private long getStorageBuffer(int usage, int memFlags) {
        final long storageBuffer;
        try(MemoryStack stack = stackPush())
        {
            PointerBuffer pBufferMemory=stack.mallocPointer(1);
            LongBuffer pBuffer=stack.mallocLong(1);



                VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
                bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
                bufferInfo.size(1920*1080*4*4);
                bufferInfo.usage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
                //bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
    //
                VmaAllocationCreateInfo allocationInfo  = VmaAllocationCreateInfo.callocStack(stack);
                allocationInfo.usage(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE);
                allocationInfo.requiredFlags(memFlags);

                int result = vmaCreateBuffer(Vulkan.getAllocator(), bufferInfo, allocationInfo, pBuffer, pBufferMemory, null);
                if(result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create buffer:" + result);
                }

    //            LongBuffer pBufferMem = MemoryUtil.memLongBuffer(MemoryUtil.memAddressSafe(pBufferMemory), 1);
    //
    //            if(vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
    //                throw new RuntimeException("Failed to create vertex buffer");
    //            }
    //
    //            VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
    //            vkGetBufferMemoryRequirements(device, pBuffer.get(0), memRequirements);
    //
    //            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
    //            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
    //            allocInfo.allocationSize(memRequirements.size());
    //            allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties));
    //
    //            if(vkAllocateMemory(device, allocInfo, null, pBufferMem) != VK_SUCCESS) {
    //                throw new RuntimeException("Failed to allocate vertex buffer memory");
    //            }
    //
    //            vkBindBufferMemory(device, pBuffer.get(0), pBufferMem.get(0), 0);


            storageBuffer = pBuffer.get(0);
            storageBufferMem = pBufferMemory.get(0);
        }
        return storageBuffer;
    }


    private long createCompDescriptorPool() {

            try(MemoryStack stack = stackPush()) {


                VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(1, stack);



                    VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
                    uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
                    uniformBufferPoolSize.descriptorCount(1);



                VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
                poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
                poolInfo.pPoolSizes(poolSizes);
                poolInfo.maxSets(1);
                //poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);

                LongBuffer pDescriptorPool = stack.mallocLong(1);


                    if(vkCreateDescriptorPool(Vulkan.getDevice(), poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                        throw new RuntimeException("Failed to create descriptor pool");
                    }
                    return pDescriptorPool.get(0);

            }

    }

    private long createComputePipeline(String path) {

//                SPIRV vertShaderSPIRV = compileShader(path + ".vsh", "vertex");
//                SPIRV fragShaderSPIRV = compileShader(path + ".fsh", "fragment");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ShaderSPIRVUtils.SPIRV vertShaderSPIRV = compileShaderFile(path + ".csh", ShaderSPIRVUtils.ShaderKind.COMPUTE_SHADER);

            long compShaderModule = createShaderModule(vertShaderSPIRV.bytecode());

            ByteBuffer entryPoint = stack.UTF8("main");



            VkPipelineShaderStageCreateInfo compShaderStageInfo = VkPipelineShaderStageCreateInfo.callocStack(stack);

            compShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            compShaderStageInfo.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            compShaderStageInfo.module(compShaderModule);
            compShaderStageInfo.pName(entryPoint);

            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.callocStack(1, stack);
            pipelineInfo.sType$Default();
            pipelineInfo.stage(compShaderStageInfo);


            pipelineInfo.layout(compPipelineLayout);

            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if(vkCreateComputePipelines(Vulkan.getDevice(), pipelineCache, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);

        }
    }

    private long createPipelineLayout() {
        try(MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(compdescriptorSetLayout));

            if(pushConstant.getSize() > 0) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.callocStack(1, stack);
                pushConstantRange.size(pushConstant.getSize());
                pushConstantRange.offset(0);
                pushConstantRange.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);

                pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
            }

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(Vulkan.getDevice(), pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            return pPipelineLayout.get(0);
        }
    }

    private long createDescriptorSetLayout(String path) {



        try(MemoryStack stack = stackPush()) {

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.callocStack(1, stack);


                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(0);
                uboLayoutBinding.binding(0);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);







            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if(vkCreateDescriptorSetLayout(Vulkan.getDevice(), layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            return pDescriptorSetLayout.get(0);
        }
    }

    private long allocateDescriptorSet() {
        try(MemoryStack stack = stackPush()) {

//                LongBuffer layout = stack.mallocLong(2);
//                layout.put(descriptorSetLayout);

            LongBuffer layouts = stack.mallocLong(1);
            {
                layouts.put(0, compdescriptorSetLayout);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(compdescriptorSetPool);
            allocInfo.pSetLayouts(layouts);

            LongBuffer pDescriptorSet = stack.mallocLong(1);

            int result = vkAllocateDescriptorSets(Vulkan.getDevice(), allocInfo, pDescriptorSet);

            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
            }

            return pDescriptorSet.get(0);
        }
    }

    public void updateDescriptorSets(MemoryStack stack)
    {

        VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.callocStack(1, stack);
        bufferInfos.offset(0);
        bufferInfos.range(854*480*4);
        bufferInfos.buffer(storageBuffer);
        VkWriteDescriptorSet.Buffer uboDescriptorWrite = VkWriteDescriptorSet.callocStack(1, stack);
        uboDescriptorWrite.get(0)
                .sType$Default()
                .dstBinding(0)
                .dstArrayElement(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .descriptorCount(1)
                .pBufferInfo(bufferInfos)
                .dstSet(compDescriptorSet);

        vkUpdateDescriptorSets(Vulkan.getDevice(), uboDescriptorWrite, null);


    }






}
