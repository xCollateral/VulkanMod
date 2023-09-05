package net.vulkanmod.vulkan.shader;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.shader.ShaderSPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.shader.ShaderSPIRVUtils.ShaderKind;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.UniformBuffers;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.shader.layout.UBO;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.vulkanmod.vulkan.shader.ShaderSPIRVUtils.compileShader;
import static net.vulkanmod.vulkan.shader.ShaderSPIRVUtils.compileShaderFile;
import static net.vulkanmod.vulkan.shader.PipelineState.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Pipeline {

    private static final VkDevice DEVICE = Vulkan.getDevice();
    private static final long PIPELINE_CACHE = createPipelineCache();
    private static final List<Pipeline> PIPELINES = new LinkedList<>();

    public static void recreateDescriptorSets(int frames) {
        PIPELINES.forEach(pipeline -> {
            pipeline.destroyDescriptorSets();
            pipeline.createDescriptorSets(frames);
        });
    }

    private long descriptorSetLayout;
    private long pipelineLayout;
    private final Map<PipelineState, Long> graphicsPipelines = new HashMap<>();
    private final VertexFormat vertexFormat;

    private DescriptorSets[] descriptorSets;
    private final List<UBO> UBOs;
    private final ManualUBO manualUBO;
    private final List<Sampler> samplers;
    private final PushConstants pushConstants;

    private long vertShaderModule = 0;
    private long fragShaderModule = 0;

    public Pipeline(VertexFormat vertexFormat, RenderPass renderPass, List<UBO> UBOs, ManualUBO manualUBO, List<Sampler> samplers, PushConstants pushConstants, SPIRV vertSpirv, SPIRV fragSpirv) {
        this.UBOs = UBOs;
        this.manualUBO = manualUBO;
        this.samplers = samplers;
        this.pushConstants = pushConstants;
        this.vertexFormat = vertexFormat;

        createDescriptorSetLayout();
        createPipelineLayout();
        createShaderModules(vertSpirv, fragSpirv);

        if(renderPass != null)
            graphicsPipelines.computeIfAbsent(new PipelineState(DEFAULT_BLEND_STATE, DEFAULT_DEPTH_STATE, DEFAULT_LOGICOP_STATE, DEFAULT_COLORMASK, renderPass),
                    this::createGraphicsPipeline);

        createDescriptorSets(Vulkan.getSwapChainImages().size());

        PIPELINES.add(this);
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

            if(state.cullState)
                rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            else
                rasterizer.cullMode(VK_CULL_MODE_NONE);

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
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            if(!Vulkan.DYNAMIC_RENDERING) {
                pipelineInfo.renderPass(state.renderPass.getId());
                pipelineInfo.subpass(0);
            }
            else {
                //dyn-rendering
                VkPipelineRenderingCreateInfoKHR renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack);
                renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
                renderingInfo.pColorAttachmentFormats(stack.ints(state.renderPass.getFramebuffer().getFormat()));
                renderingInfo.depthAttachmentFormat(state.renderPass.getFramebuffer().getDepthFormat());
                pipelineInfo.pNext(renderingInfo);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if(vkCreateGraphicsPipelines(DEVICE, PIPELINE_CACHE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);
        }
    }

    private void createDescriptorSetLayout() {
        try(MemoryStack stack = stackPush()) {
            int bindingsSize = this.UBOs.size() + samplers.size();

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.callocStack(bindingsSize, stack);

            for(UBO ubo : this.UBOs) {
                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(ubo.getBinding());
                uboLayoutBinding.binding(ubo.getBinding());
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(ubo.getFlags());

            }

            for(Sampler sampler : this.samplers) {
                VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(sampler.binding);
                samplerLayoutBinding.binding(sampler.binding);
                samplerLayoutBinding.descriptorCount(1);
                samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerLayoutBinding.pImmutableSamplers(null);
                samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS);

//                ++i;
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if(vkCreateDescriptorSetLayout(DEVICE, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }

//            this.descriptorSetLayout.put(0, pDescriptorSetLayout.get(0));
            this.descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    private void createPipelineLayout() {
        try(MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(this.descriptorSetLayout));

            if(this.pushConstants != null) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.callocStack(1, stack);
                pushConstantRange.size(this.pushConstants.getSize());
                pushConstantRange.offset(0);
                pushConstantRange.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

                pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
            }

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(DEVICE, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);
        }
    }

    private void createDescriptorSets(int frames) {
        descriptorSets = new DescriptorSets[frames];
        for(int i = 0; i < frames; ++i) {
            descriptorSets[i] = new DescriptorSets(i);
        }
    }

    private void createShaderModules(SPIRV vertSpirv, SPIRV fragSpirv) {
        this.vertShaderModule = createShaderModule(vertSpirv.bytecode());
        this.fragShaderModule = createShaderModule(fragSpirv.bytecode());
    }

    public void cleanUp() {
        vkDestroyShaderModule(DEVICE, vertShaderModule, null);
        vkDestroyShaderModule(DEVICE, fragShaderModule, null);

        destroyDescriptorSets();

        graphicsPipelines.forEach((state, pipeline) -> {
            vkDestroyPipeline(DEVICE, pipeline, null);
        });
        graphicsPipelines.clear();

        vkDestroyDescriptorSetLayout(DEVICE, descriptorSetLayout, null);
        vkDestroyPipelineLayout(DEVICE, pipelineLayout, null);

        PIPELINES.remove(this);
        Renderer.getInstance().removeUsedPipeline(this);
    }

    private void destroyDescriptorSets() {
        for(DescriptorSets descriptorSets : this.descriptorSets) {
            descriptorSets.cleanUp();
        }

        this.descriptorSets = null;
    }

    public ManualUBO getManualUBO() { return this.manualUBO; }

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

            VertexFormatElement formatElement = elements.get(i);
            VertexFormatElement.Usage usage = formatElement.getUsage();
            VertexFormatElement.Type type = formatElement.getType();
            switch (usage) {
                case POSITION :
                    if(type == VertexFormatElement.Type.FLOAT) {
                        posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                        posDescription.offset(offset);

                        offset += 12;
                    }
                    else if (type == VertexFormatElement.Type.SHORT) {
                        posDescription.format(VK_FORMAT_R16G16B16A16_SINT);
                        posDescription.offset(offset);

                        offset += 8;
                    }
                    else if (type == VertexFormatElement.Type.BYTE) {
                        posDescription.format(VK_FORMAT_R8G8B8A8_SINT);
                        posDescription.offset(offset);

                        offset += 4;
                    }

                    break;

                case COLOR:
                    posDescription.format(VK_FORMAT_R8G8B8A8_UNORM);
                    posDescription.offset(offset);

//                offset += 16;
                    offset += 4;
                    break;

                case UV:
                    if(type == VertexFormatElement.Type.FLOAT){
                        posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                        posDescription.offset(offset);

                        offset += 8;
                    }
                    else if(type == VertexFormatElement.Type.SHORT){
                        posDescription.format(VK_FORMAT_R16G16_SINT);
                        posDescription.offset(offset);

                        offset += 4;
                    }
                    else if(type == VertexFormatElement.Type.USHORT){
                        posDescription.format(VK_FORMAT_R16G16_UINT);
                        posDescription.offset(offset);

                        offset += 4;
                    }
                    break;

                case NORMAL:
                    posDescription.format(VK_FORMAT_R8G8B8A8_SNORM);
                    posDescription.offset(offset);

                    offset += 4;
                    break;

                case PADDING:

                default:
                    throw new RuntimeException(String.format("Unknown format: %s", usage));
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

            if(vkCreateShaderModule(DEVICE, createInfo, null, pShaderModule) != VK_SUCCESS) {
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

            if(vkCreatePipelineCache(DEVICE, cacheCreateInfo, null, pPipelineCache) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pPipelineCache.get(0);
        }
    }

    public static void destroyPipelineCache() {
        vkDestroyPipelineCache(DEVICE, PIPELINE_CACHE, null);
    }

    public void resetDescriptorPool(int i) {
        if(this.descriptorSets != null)
                this.descriptorSets[i].resetIdx();

    }

    public long getHandle(PipelineState state) {
        return graphicsPipelines.computeIfAbsent(state, this::createGraphicsPipeline);
    }

    public PushConstants getPushConstants() { return this.pushConstants; }

    public long getLayout() { return pipelineLayout; }

    public record Sampler(int binding, String type, String name) {}

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, int frame) {
        UniformBuffers uniformBuffers = Renderer.getDrawer().getUniformBuffers();
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffers);
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, UniformBuffers uniformBuffers, int frame) {
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffers);
    }

    private class DescriptorSets {
        private int poolSize = 10;
        private long descriptorPool;
        private LongBuffer sets;
        private long uniformBufferId;
        private long currentSet;
        private int currentIdx = -1;

        private final int frame;
        private final VulkanImage.Sampler[] boundTextures = new VulkanImage.Sampler[samplers.size()];
        private final IntBuffer dynamicOffsets = MemoryUtil.memAllocInt(UBOs.size());;

        DescriptorSets(int frame) {
            this.frame = frame;

            try(MemoryStack stack = stackPush()) {
                this.createDescriptorPool(stack);
                this.createDescriptorSets(stack);
            }
        }

        private void bindSets(VkCommandBuffer commandBuffer, UniformBuffers uniformBuffers) {
            try(MemoryStack stack = stackPush()) {

                this.updateUniforms(uniformBuffers);
                this.updateDescriptorSet(stack, uniformBuffers);

                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
                        0, stack.longs(currentSet), dynamicOffsets);
            }
        }

        private void updateUniforms(UniformBuffers uniformBuffers) {
            int currentOffset = uniformBuffers.getUsedBytes();

            int i = 0;
            for(UBO ubo : UBOs) {
//                ubo.update();
//                uniformBuffers.uploadUBO(ubo.getBuffer(), currentOffset, frame);

                this.dynamicOffsets.put(i, currentOffset);

                //TODO non mappable memory

                int alignedSize = UniformBuffers.getAlignedSize(ubo.getSize());
                uniformBuffers.checkCapacity(alignedSize);
                ubo.update(uniformBuffers.getPointer(frame));

                uniformBuffers.updateOffset(alignedSize);

                currentOffset = uniformBuffers.getUsedBytes();
                ++i;
            }
        }

        private void updateDescriptorSet(MemoryStack stack, UniformBuffers uniformBuffers) {

            boolean changed = false;
            for(int j = 0; j < samplers.size(); ++j) {
                Sampler sampler = samplers.get(j);

                VulkanImage.Sampler texture = VTextureSelector.getTexture(sampler.name()).getTextureSampler();
                texture.image().readOnlyLayout();
                if(this.boundTextures[j] != texture) {
                    changed = true;
                    break;
                }
            }

            if(!changed && this.currentIdx != -1 &&
                this.uniformBufferId == uniformBuffers.getId(frame)) {
                this.currentSet = this.sets.get(this.currentIdx);
                return;
            }

            this.uniformBufferId = uniformBuffers.getId(frame);
            this.currentIdx++;
            if(this.currentIdx >= this.poolSize) {
                this.poolSize *= 2;

                this.createDescriptorPool(stack);
                this.createDescriptorSets(stack);

                //debug
//                System.out.println("resized descriptor pool to: " + this.poolSize);

                this.resetIdx();
                this.updateDescriptorSet(stack, uniformBuffers);
                return;
            }

            this.currentSet = this.sets.get(this.currentIdx);

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(UBOs.size() + samplers.size(), stack);
            VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[UBOs.size()];

            //TODO maybe ubo update is not needed everytime
            int i = 0;
            for(UBO ubo : UBOs) {

                bufferInfos[i] = VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfos[i].buffer(this.uniformBufferId);
                bufferInfos[i].range(ubo.getSize());

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                uboDescriptorWrite.sType$Default();
                uboDescriptorWrite.dstBinding(ubo.getBinding());
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfos[i]);
                uboDescriptorWrite.dstSet(currentSet);

                ++i;
            }

            VkDescriptorImageInfo.Buffer[] imageInfo = new VkDescriptorImageInfo.Buffer[samplers.size()];

            for(int j = 0; j < samplers.size(); ++j) {
                Sampler sampler = samplers.get(j);
                VulkanImage texture = VTextureSelector.getTexture(sampler.name());
                VulkanImage.Sampler textureSampler = texture.getTextureSampler();
                texture.readOnlyLayout();

                imageInfo[j] = VkDescriptorImageInfo.callocStack(1, stack);
                imageInfo[j].imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                imageInfo[j].imageView(texture.getImageView());
                imageInfo[j].sampler(textureSampler.sampler());

                VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(sampler.binding());
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo[j]);
                samplerDescriptorWrite.dstSet(currentSet);

                this.boundTextures[j] = textureSampler;
                ++i;
            }

            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
        }

        private void createDescriptorSets(MemoryStack stack) {
            LongBuffer layout = stack.mallocLong(this.poolSize);
//            layout.put(0, descriptorSetLayout);

            for(int i = 0; i < this.poolSize; ++i) {
                layout.put(i, descriptorSetLayout);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType$Default();
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(layout);

            this.sets = MemoryUtil.memAllocLong(this.poolSize);

            int result = vkAllocateDescriptorSets(DEVICE, allocInfo, this.sets);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
            }
        }

        private void createDescriptorPool(MemoryStack stack) {
            int size =  UBOs.size() + samplers.size();

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(size, stack);

            int i;
            for(i = 0; i < UBOs.size(); ++i) {
                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(i);
//                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
                uniformBufferPoolSize.descriptorCount(this.poolSize);
            }

            for(; i < UBOs.size() + samplers.size(); ++i) {
                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(i);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(this.poolSize);
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(this.poolSize);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(DEVICE, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            if(this.descriptorPool != VK_NULL_HANDLE) {
                final long oldDescriptorPool = this.descriptorPool;
                MemoryManager.getInstance().addFrameOp(() -> {
                    vkDestroyDescriptorPool(DEVICE, oldDescriptorPool, null);
                });
            }

            this.descriptorPool = pDescriptorPool.get(0);
        }

        public void resetIdx() { this.currentIdx = -1; }

        private void cleanUp() {
            vkResetDescriptorPool(DEVICE, descriptorPool, 0);
            vkDestroyDescriptorPool(DEVICE, descriptorPool, null);
        }

    }

    public static class Builder {

        public static Pipeline create(VertexFormat format, String path) {
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            return pipelineBuilder.createPipeline();
        }

        private final VertexFormat vertexFormat;
        private final String shaderPath;
        private List<UBO> UBOs;
        private ManualUBO manualUBO;
        private PushConstants pushConstants;
        private List<Sampler> samplers;
        private int currentBinding;

        private SPIRV vertShaderSPIRV;
        private SPIRV fragShaderSPIRV;

        private RenderPass renderPass;

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

            if(this.manualUBO != null)
                this.UBOs.add(this.manualUBO);

            return new Pipeline(this.vertexFormat, this.renderPass,
                    this.UBOs, this.manualUBO, this.samplers, this.pushConstants, this.vertShaderSPIRV, this.fragShaderSPIRV);
        }

        public void setUniforms(List<UBO> UBOs, List<Sampler> samplers) {
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

            String resourcePath = String.format("/assets/vulkanmod/shaders/%s.json", this.shaderPath);
            InputStream stream = Pipeline.class.getResourceAsStream(resourcePath);

            if(stream == null)
                throw new NullPointerException(String.format("Failed to load: %s", resourcePath));

            jsonObject = GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));

            JsonArray jsonUbos = GsonHelper.getAsJsonArray(jsonObject, "UBOs", (JsonArray)null);
            JsonArray jsonManualUbos = GsonHelper.getAsJsonArray(jsonObject, "ManualUBOs", (JsonArray)null);
            JsonArray jsonSamplers = GsonHelper.getAsJsonArray(jsonObject, "samplers", (JsonArray)null);
            JsonArray jsonPushConstants = GsonHelper.getAsJsonArray(jsonObject, "PushConstants", (JsonArray)null);

            if (jsonUbos != null) {
                for (JsonElement jsonelement : jsonUbos) {
                    this.parseUboNode(jsonelement);
                }
            }

            if (jsonManualUbos != null) {
                this.parseManualUboNode(jsonManualUbos.get(0));
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

            if(binding > this.currentBinding)
                this.currentBinding = binding;

            this.UBOs.add(ubo);
        }

        private void parseManualUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "ManualUBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int type = getTypeFromString(GsonHelper.getAsString(jsonobject, "type"));
            int size = GsonHelper.getAsInt(jsonobject, "size");

            if(binding > this.currentBinding)
                this.currentBinding = binding;

            this.manualUBO = new ManualUBO(binding, type, size);
        }

        private void parseSamplerNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "UBO");
            String name = GsonHelper.getAsString(jsonobject, "name");

            this.currentBinding++;

            this.samplers.add(new Sampler(this.currentBinding, "sampler2D", name));
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
