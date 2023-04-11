package net.vulkanmod.vulkan.shader;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.ShaderSPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.ShaderSPIRVUtils.ShaderKind;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.TransferQueue;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.UniformBuffers;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.shader.layout.UBO;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.vulkanmod.vulkan.ShaderSPIRVUtils.compileShader;
import static net.vulkanmod.vulkan.ShaderSPIRVUtils.compileShaderFile;
import static net.vulkanmod.vulkan.Vulkan.getSwapChainImages;
import static net.vulkanmod.vulkan.shader.PipelineState.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.vulkan.VK10.*;

public class Pipeline {

    private static final VkDevice device = Vulkan.getDevice();
    private static final long pipelineCache = createPipelineCache();
    private static final int imagesSize = getSwapChainImages().size();

    private long descriptorSetLayout;
    private long pipelineLayout;
    private Map<PipelineState, Long> graphicsPipelines = new HashMap<>();
    private VertexFormat vertexFormat;

    private final long[] descriptorPools = new long[imagesSize];
    private int descriptorCount = 500;

    private final List<UBO> UBOs;
    private final List<String> samplers;
    private PushConstants pushConstants;

    private Consumer<Integer> resetDescriptorPoolFun = defaultResetDPFun();

    private long vertShaderModule = 0;
    private long fragShaderModule = 0;

    public Pipeline(VertexFormat vertexFormat, List<UBO> UBOs, List<String> samplers, PushConstants pushConstants, SPIRV vertSpirv, SPIRV fragSpirv) {
        this.UBOs = UBOs;
        this.samplers = samplers;
        this.pushConstants = pushConstants;
        this.vertexFormat = vertexFormat;

//        parseBindings();
        createDescriptorSetLayout();
        createPipelineLayout();
        createShaderModules(vertSpirv, fragSpirv);

        graphicsPipelines.computeIfAbsent(new PipelineState(DEFAULT_BLEND_STATE, DEFAULT_DEPTH_STATE, DEFAULT_LOGICOP_STATE, DEFAULT_COLORMASK),
                this::createGraphicsPipeline);
        createDescriptorPool(descriptorCount);
        //allocateDescriptorSets();

    }

    private long createGraphicsPipeline(PipelineState state) {

        try(MemoryStack stack = stackPush()) {

            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.callocStack(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.callocStack(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(getBindingDescription(vertexFormat));
            vertexInputInfo.pVertexAttributeDescriptions(getAttributeDescriptions(vertexFormat));

            // ===> ASSEMBLY STAGE <===

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.callocStack(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);

            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            // ===> RASTERIZATION STAGE <===

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.callocStack(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);

            if(state.cullState) rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            else rasterizer.cullMode(VK_CULL_MODE_NONE);

            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
            rasterizer.depthBiasEnable(true);

            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.callocStack(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // ===> DEPTH TEST <===

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.callocStack(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(state.depthState.depthTest);
            depthStencil.depthWriteEnable(state.depthState.depthMask);
            depthStencil.depthCompareOp(state.depthState.function);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional
            depthStencil.stencilTestEnable(false);

            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.callocStack(1, stack);
            colorBlendAttachment.colorWriteMask(state.colorMask.colorMask);

            if(state.blendState.enabled) {
                colorBlendAttachment.blendEnable(true);
                colorBlendAttachment.srcColorBlendFactor(state.blendState.srcRgbFactor);
                colorBlendAttachment.dstColorBlendFactor(state.blendState.dstRgbFactor);
                colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
                colorBlendAttachment.srcAlphaBlendFactor(state.blendState.srcAlphaFactor);
                colorBlendAttachment.dstAlphaBlendFactor(state.blendState.dstAlphaFactor);
                colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);
            }
            else {
                colorBlendAttachment.blendEnable(false);
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.callocStack(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(state.logicOpState.enabled);
            colorBlending.logicOp(state.logicOpState.getLogicOp());
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> DYNAMIC STATES <===

            VkPipelineDynamicStateCreateInfo dynamicStates = VkPipelineDynamicStateCreateInfo.callocStack(stack);
            dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.callocStack(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicStates);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(Vulkan.getRenderPass());
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if(vkCreateGraphicsPipelines(device, pipelineCache, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);
        }
    }

    private void createDescriptorSetLayout() {
        try(MemoryStack stack = stackPush()) {

            int bindingsSize = this.UBOs.size() + this.samplers.size();

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.callocStack(bindingsSize, stack);

            int i = 0;
            for(UBO ubo : this.UBOs) {
                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(ubo.getBinding());
                uboLayoutBinding.binding(ubo.getBinding());
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(ubo.getFlags());

                ++i;
            }

            for(String s : this.samplers) {
                VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(i);
                samplerLayoutBinding.binding(i);
                samplerLayoutBinding.descriptorCount(1);
                samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerLayoutBinding.pImmutableSamplers(null);
                samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS);

                ++i;
            }



            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if(vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    private void createPipelineLayout() {
        try(MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));

            if(this.pushConstants != null) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.callocStack(1, stack);
                pushConstantRange.size(this.pushConstants.getSize());
                pushConstantRange.offset(0);
                pushConstantRange.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

                pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
            }

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);
        }
    }

    private void createDescriptorPool(int descriptorCount) {

        try(MemoryStack stack = stackPush()) {
            int size =  UBOs.size() + samplers.size();

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(size, stack);

            int i;
            for(i = 0; i < UBOs.size(); ++i) {
                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(i);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.descriptorCount(descriptorCount);
            }

            for(; i < UBOs.size() + samplers.size(); ++i) {
                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(i);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(descriptorCount);
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(descriptorCount);
            //poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            for(i = 0; i < imagesSize; ++i) {
                if(vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor pool");
                }
                descriptorPools[i] = pDescriptorPool.get(0);
            }
        }
    }

    private void createShaderModules(SPIRV vertSpirv, SPIRV fragSpirv) {
        this.vertShaderModule = createShaderModule(vertSpirv.bytecode());
        this.fragShaderModule = createShaderModule(fragSpirv.bytecode());
    }

    public void cleanUp() {
        vkDestroyShaderModule(device, vertShaderModule, null);
        vkDestroyShaderModule(device, fragShaderModule, null);

        for(long descriptorPool : descriptorPools)
        vkDestroyDescriptorPool(device, descriptorPool, null);

        this.resetDescriptorPoolFun = i -> {};

        graphicsPipelines.forEach((state, pipeline) -> {
            vkDestroyPipeline(device, pipeline, null);
        });
        graphicsPipelines.clear();

        vkDestroyPipelineLayout(device, pipelineLayout, null);
    }

    public long createDescriptorSets(VkCommandBuffer commandBuffer, int frame, UniformBuffers uniformBuffers) {
        DescriptorSetsUnit dsUnit = new DescriptorSetsUnit(commandBuffer, frame, uniformBuffers);
        //descriptorSetsLists[frame].add(dsUnit);
        return dsUnit.descriptorSet;
    }

    private static VkVertexInputBindingDescription.Buffer getBindingDescription(VertexFormat vertexFormat) {

        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.callocStack(1);

        bindingDescription.binding(0);
        bindingDescription.stride(vertexFormat.getVertexSize());
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        return bindingDescription;
    }

    private static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(VertexFormat vertexFormat) {

        ImmutableList<VertexFormatElement> elements = vertexFormat.getElements();

        int size = elements.size();
        if(elements.stream().anyMatch(vertexFormatElement -> vertexFormatElement.getUsage() == VertexFormatElement.Usage.PADDING)) {
            size--;
        }

        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.callocStack(size);

        int offset = 0;

        for(int i = 0; i < size; ++i) {
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);

            VertexFormatElement.Usage usage = elements.get(i).getUsage();
            if (usage == VertexFormatElement.Usage.POSITION)
            {
                posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                posDescription.offset(offset);

                offset += 12;
            }
            else if (usage == VertexFormatElement.Usage.COLOR)
            {
//                posDescription.format(VK_FORMAT_R32G32B32A32_SFLOAT);
                posDescription.format(VK_FORMAT_R8G8B8A8_UNORM);
                posDescription.offset(offset);

//                offset += 16;
                offset += 4;
            }
            else if (usage == VertexFormatElement.Usage.UV)
            {
                if(elements.get(i).getType() == VertexFormatElement.Type.FLOAT){
                    posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                    posDescription.offset(offset);

                    offset += 8;
                }
                else if(elements.get(i).getType() == VertexFormatElement.Type.SHORT){
                    posDescription.format(VK_FORMAT_R16G16_SINT);
                    posDescription.offset(offset);

                    offset += 4;
                }
            }
            else if (usage == VertexFormatElement.Usage.NORMAL)
            {
//                posDescription.format(VK_FORMAT_R8G8B8_SNORM);
                posDescription.format(VK_FORMAT_R8G8B8A8_SNORM);
                posDescription.offset(offset);

                offset += 4;
            }
            else if (usage == VertexFormatElement.Usage.PADDING)
            {
//                posDescription.format(VK_FORMAT_R8_SNORM);
//                posDescription.offset(offset);

//                offset += 1;
            }

            else {
                throw new RuntimeException("Unknown format:");
            }

            posDescription.offset(((VertexFormatMixed)(vertexFormat)).getOffset(i));
        }

        return attributeDescriptions.rewind();
    }

    private static long createShaderModule(ByteBuffer spirvCode) {

        try(MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }

    private static long createPipelineCache() {
        try(MemoryStack stack = stackPush()) {

            VkPipelineCacheCreateInfo cacheCreateInfo = VkPipelineCacheCreateInfo.callocStack(stack);
            cacheCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            LongBuffer pPipelineCache = stack.mallocLong(1);

            if(vkCreatePipelineCache(device, cacheCreateInfo, null, pPipelineCache) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pPipelineCache.get(0);
        }
    }

    private Consumer<Integer> defaultResetDPFun() {
        Consumer<Integer> fun = (i) -> {
            if(vkResetDescriptorPool(device, descriptorPools[i], 0) != VK_SUCCESS) {
                throw new RuntimeException("Failed to reset descriptor pool");
            }
        };
        return fun;
    }

    public void resetDescriptorPool(int i) {
        this.resetDescriptorPoolFun.accept(i);

    }

    public long getHandle(PipelineState state) {
        return graphicsPipelines.computeIfAbsent(state, state1 -> {
            return createGraphicsPipeline(state1);
        });
    }

    public PushConstants getPushConstants() { return this.pushConstants; }

    public long getLayout() { return pipelineLayout; }

    private class DescriptorSetsUnit {
        private long descriptorSet;

        private DescriptorSetsUnit(VkCommandBuffer commandBuffer, int frame, UniformBuffers uniformBuffers) {

            allocateDescriptorSet(frame);
            updateDescriptorSet(frame, uniformBuffers);
        }

        private void allocateDescriptorSet(int frame) {
            try(MemoryStack stack = stackPush()) {

//                LongBuffer layout = stack.mallocLong(2);
//                layout.put(descriptorSetLayout);

                LongBuffer layouts = stack.mallocLong(1);
                for (int i = 0; i < layouts.capacity(); i++) {
                    layouts.put(i, descriptorSetLayout);
                }

                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                allocInfo.descriptorPool(descriptorPools[frame]);
                allocInfo.pSetLayouts(layouts);

                LongBuffer pDescriptorSet = stack.mallocLong(1);

                int result = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet);
                if (result == -1000069000) {
                    descriptorCount *= 2;
                    createDescriptorPool(descriptorCount);
                    allocateDescriptorSet(frame);

                    //TODO: free old descriptorPool when its relative cmdBuffer has finished drawing

                    System.out.println("resized DescriptorPool to: " + descriptorCount);
                    return;
                }
                if (result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
                }

                descriptorSet = pDescriptorSet.get(0);
            }
        }

        private void updateDescriptorSet(int frame, UniformBuffers uniformBuffers) {
            try(MemoryStack stack = stackPush()) {

                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.callocStack(UBOs.size() + samplers.size(), stack);
                VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[UBOs.size()];

                int currentOffset = uniformBuffers.getUsedBytes();

                int i = 0;
                for(UBO ubo : UBOs) {

                    ubo.update();
                    uniformBuffers.uploadUBO(ubo.getBuffer(), currentOffset, frame);

                    bufferInfos[i] = VkDescriptorBufferInfo.callocStack(1, stack);
                    bufferInfos[i].offset(currentOffset);
                    bufferInfos[i].range(ubo.getSize());

                    VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                    uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                    uboDescriptorWrite.dstBinding(ubo.getBinding());
                    uboDescriptorWrite.dstArrayElement(0);
                    uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                    uboDescriptorWrite.descriptorCount(1);
                    uboDescriptorWrite.pBufferInfo(bufferInfos[i]);

                    currentOffset = uniformBuffers.getUsedBytes();
                    ++i;
                }

                //Don't create empty CommandbUffer if samplers are empty
                if(!samplers.isEmpty())
                {
                    VkDescriptorImageInfo.Buffer imageInfos = VkDescriptorImageInfo.malloc(samplers.size(), stack);
                    TransferQueue.CommandBuffer commandBuffer = TransferQueue.beginCommands();
                    int j=0;
                    for (final VkDescriptorImageInfo imageInfo : imageInfos) {

                        //TODO: make an auxiliary function
                        VulkanImage texture = getTex(j);

                        texture.readOnlyLayout(commandBuffer);
                        //VulkanImage texture = VTextureManager.getCurrentTexture();


                        imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                        imageInfo.imageView(texture.getTextureImageView());
                        imageInfo.sampler(texture.getTextureSampler());

                        VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                        samplerDescriptorWrite.sType$Default();
                        samplerDescriptorWrite.dstBinding(i);
                        samplerDescriptorWrite.dstArrayElement(0);
                        samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                        samplerDescriptorWrite.descriptorCount(1);
                        memPutAddress(samplerDescriptorWrite.address() + VkWriteDescriptorSet.PIMAGEINFO, imageInfo.address());
                        ++i;
                        ++j;

                    }
                    long fence = TransferQueue.endCommands(commandBuffer);
                    if (fence != 0) Synchronization.addFence(fence);
                }

                //long descriptorSet = descriptorSets.get(Drawer.getCurrentFrame());

                for(i = 0; i < UBOs.size(); ++i) {
                    bufferInfos[i].buffer(uniformBuffers.getId(frame));
                    VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                    uboDescriptorWrite.dstSet(descriptorSet);
                }

                for(; i < UBOs.size() + samplers.size(); ++i){
                    VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                    samplerDescriptorWrite.dstSet(descriptorSet);
                }


                vkUpdateDescriptorSets(device, descriptorWrites, null);
            }
        }

        private VulkanImage getTex(int j) {
            return switch (j)
            {
                case 0 -> VTextureSelector.getBoundTexture();
                case 1 -> VTextureSelector.getLightTexture();
                case 2 -> VTextureSelector.getOverlayTexture();
                default -> throw new IllegalStateException("Unexpected value: " + j);
            };
        }
    }

    public static class Builder {
        private final VertexFormat vertexFormat;
        private final String shaderPath;
        private List<UBO> UBOs;
        private PushConstants pushConstants;
        private List<String> samplers;

        private SPIRV vertShaderSPIRV;
        private SPIRV fragShaderSPIRV;

        public Builder(VertexFormat vertexFormat, String path) {
            this.vertexFormat = vertexFormat;
            this.shaderPath = path;
        }

        public Builder(VertexFormat vertexFormat) {
            this(vertexFormat, null);
        }

        public Pipeline createPipeline() {
            Validate.isTrue(this.samplers != null && this.UBOs != null
                    && this.vertShaderSPIRV != null && this.fragShaderSPIRV != null,
                    "Cannot create Pipeline: resources missing");

            return new Pipeline(this.vertexFormat, this.UBOs, this.samplers, this.pushConstants, this.vertShaderSPIRV, this.fragShaderSPIRV);
        }

        public void setUniforms(List<UBO> UBOs, List<String> samplers) {
            this.UBOs = UBOs;
            this.samplers = samplers;
        }

        public void compileShaders() {
            this.vertShaderSPIRV = compileShaderFile(this.shaderPath + ".vsh", ShaderKind.VERTEX_SHADER);
            this.fragShaderSPIRV = compileShaderFile(this.shaderPath + ".fsh", ShaderKind.FRAGMENT_SHADER);
        }

        public void compileShaders(String vsh, String fsh) {
            this.vertShaderSPIRV = compileShader("vertex shader", vsh, ShaderKind.VERTEX_SHADER);
            this.fragShaderSPIRV = compileShader("fragment shader", fsh, ShaderKind.FRAGMENT_SHADER);
        }

        public void parseBindingsJSON() {
            Validate.notNull(this.shaderPath, "Cannot parse bindings: shaderPath is null");

            this.UBOs = new ArrayList<>();
            this.samplers = new ArrayList<>();

            JsonObject jsonObject;

            InputStream stream = Pipeline.class.getResourceAsStream("/assets/vulkanmod/shaders/" + this.shaderPath + ".json");

            jsonObject = GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));

            JsonArray jsonUbos = GsonHelper.getAsJsonArray(jsonObject, "UBOs", (JsonArray)null);
            JsonArray jsonSamplers = GsonHelper.getAsJsonArray(jsonObject, "samplers", (JsonArray)null);
            JsonArray jsonPushConstants = GsonHelper.getAsJsonArray(jsonObject, "PushConstants", (JsonArray)null);

            if (jsonUbos != null) {
                for (JsonElement jsonelement : jsonUbos) {
                    this.parseUboNode(jsonelement);
                }
            }

            if(jsonSamplers != null) {
                for (JsonElement jsonelement : jsonSamplers) {
                    this.parseSamplerNode(jsonelement);
                }
            }

            if(jsonPushConstants != null) {
                this.parsePushConstantNode(jsonPushConstants);
            }
        }

        private void parseUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "UBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int type = getTypeFromString(GsonHelper.getAsString(jsonobject, "type"));
            JsonArray fields = GsonHelper.getAsJsonArray(jsonobject, "fields");

            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for (JsonElement jsonelement2 : fields) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement2, "uniform");
                //need to store some infos
                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addFieldInfo(type2, name, j);

            }
            UBO ubo = builder.buildUBO(binding, type);

            this.UBOs.add(ubo);
        }

        private void parseSamplerNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "UBO");
            String name = GsonHelper.getAsString(jsonobject, "name");

            this.samplers.add(name);
        }

        private void parsePushConstantNode(JsonArray jsonArray) {
            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for(JsonElement jsonelement : jsonArray) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement, "PC");

                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addFieldInfo(type2, name, j);
            }

            this.pushConstants = builder.buildPushConstant();
        }

        public static int getTypeFromString(String s) {
            return switch (s) {
                case "vertex" -> VK_SHADER_STAGE_VERTEX_BIT;
                case "fragment" -> VK_SHADER_STAGE_FRAGMENT_BIT;
                case "all" -> VK_SHADER_STAGE_ALL_GRAPHICS;

                default -> throw new RuntimeException("cannot identify type..");
            };
        }
    }
}
