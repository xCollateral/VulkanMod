package net.vulkanmod.vulkan.shader;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.DeviceManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static net.vulkanmod.vulkan.shader.PipelineState.*;
import static net.vulkanmod.vulkan.shader.PipelineState.DEFAULT_COLORMASK;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;

public class GraphicsPipeline extends Pipeline {

    private final Map<PipelineState, Long> graphicsPipelines = new HashMap<>();
    private final VertexFormat vertexFormat;
    private final EnumSet<SPIRVUtils.SpecConstant> specConstants;

    private long vertShaderModule = 0;
    private long fragShaderModule = 0;
    private PipelineState state;

    GraphicsPipeline(Builder builder) {
        super(builder.shaderPath);
        this.buffers = builder.UBOs;
        this.manualUBO = builder.manualUBO;
        this.imageDescriptors = builder.imageDescriptors;
        this.pushConstants = builder.pushConstants;
        this.vertexFormat = builder.vertexFormat;
        this.specConstants = builder.specConstants;

        createDescriptorSetLayout();
        createPipelineLayout();
        createShaderModules(builder.vertShaderSPIRV, builder.fragShaderSPIRV);

        if(builder.renderPass != null) {
            this.state = new PipelineState(DEFAULT_BLEND_STATE, DEFAULT_DEPTH_STATE, DEFAULT_LOGICOP_STATE, DEFAULT_COLORMASK, builder.renderPass);
            graphicsPipelines.computeIfAbsent(state,
                    this::createGraphicsPipeline);
        }

        createDescriptorSets(Renderer.getFramesNum());

        PIPELINES.add(this);
    }

    public long getHandle(PipelineState state) {
        return graphicsPipelines.computeIfAbsent(state, this::createGraphicsPipeline);
    }

    private long createGraphicsPipeline(PipelineState state) {
        this.state=state;
        try(MemoryStack stack = stackPush()) {

            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);


            VkSpecializationMapEntry.Buffer specEntrySet =  VkSpecializationMapEntry.malloc(specConstants.size(), stack);


            boolean equals = !this.specConstants.isEmpty();
            VkSpecializationInfo specInfo = equals ? VkSpecializationInfo.malloc(stack)
                    .pMapEntries(specEntrySet)
                    .pData(enumSpecConstants(stack, specEntrySet)) : null;


            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);
            vertShaderStageInfo.pSpecializationInfo(specInfo);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);
            fragShaderStageInfo.pSpecializationInfo(specInfo);

            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(getBindingDescription(vertexFormat));
            vertexInputInfo.pVertexAttributeDescriptions(getAttributeDescriptions(vertexFormat));

            // ===> ASSEMBLY STAGE <===

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);

            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            // ===> RASTERIZATION STAGE <===

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
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

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // ===> DEPTH TEST <===

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(state.depthState.depthTest);
            depthStencil.depthWriteEnable(state.depthState.depthMask);
            depthStencil.depthCompareOp(state.depthState.function);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional
            depthStencil.stencilTestEnable(false);

            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
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

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(state.logicOpState.enabled);
            colorBlending.logicOp(state.logicOpState.getLogicOp());
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> DYNAMIC STATES <===

            VkPipelineDynamicStateCreateInfo dynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
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

            if(vkCreateGraphicsPipelines(DeviceManager.device, PIPELINE_CACHE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);
        }
    }


    private ByteBuffer enumSpecConstants(MemoryStack stack, VkSpecializationMapEntry.Buffer specEntrySet) {
        int i = 0;
        int x = 0;
        ByteBuffer byteBuffer = stack.malloc(specConstants.size()*Integer.BYTES);

        for(var specDef : specConstants)
        {
            specEntrySet.get(i)
                    .constantID(specDef.ordinal())
                    .offset(x)
                    .size(4);

            byteBuffer.putInt(i, specDef.getValue());
            i++; x+=4;
        }
        return byteBuffer;
    }

    //Vulkan spec mandates that VkBool32 must always be aligned to uint32_t, which is 4 Bytes
    private static ByteBuffer alignedVkBool32(MemoryStack stack, int i) {
        return stack.malloc(Integer.BYTES).putInt(0, i); //Malloc as Int is always Unaligned, so asIntBuffer doesn't help here afaik
    }

    private void createShaderModules(SPIRVUtils.SPIRV vertSpirv, SPIRVUtils.SPIRV fragSpirv) {
        this.vertShaderModule = createShaderModule(vertSpirv.bytecode());
        this.fragShaderModule = createShaderModule(fragSpirv.bytecode());
    }

    private static VkVertexInputBindingDescription.Buffer getBindingDescription(VertexFormat vertexFormat) {

        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.calloc(1);

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
                VkVertexInputAttributeDescription.calloc(size, stackGet());

        int offset = 0;

        for(int i = 0; i < size; ++i) {
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);

            VertexFormatElement formatElement = elements.get(i);
            VertexFormatElement.Usage usage = formatElement.getUsage();
            VertexFormatElement.Type type = formatElement.getType();
            int elementCount = formatElement.getCount();

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
                    //Do nothing as padding format (VK_FORMAT_R8) is not supported everywhere
                    break;

                case GENERIC:
                    if(type == VertexFormatElement.Type.SHORT && elementCount == 1){
                        posDescription.format(VK_FORMAT_R16_SINT);
                        posDescription.offset(offset);

                        offset += 2;
                        break;
                    }
                    else if (type == VertexFormatElement.Type.INT && elementCount == 1) {
                        posDescription.format(VK_FORMAT_R32_SINT);
                        posDescription.offset(offset);

                        offset += 4;
                        break;
                    }
                    else {
                        throw new RuntimeException(String.format("Unknown format: %s", usage));
                    }


                default:
                    throw new RuntimeException(String.format("Unknown format: %s", usage));
            }

            posDescription.offset(((VertexFormatMixed)(vertexFormat)).getOffset(i));
        }

        return attributeDescriptions.rewind();
    }

    // SpecConstants can be set to be unique per Pipeline
    // but that would involve adding boilerplate to PipelineState
    // So to simplify the code, SpecConstants are limited to "Static Global State" rn
    public void updateSpecConstant(SPIRVUtils.SpecConstant specConstant)
    {

        if(this.specConstants.contains(specConstant))
        {
            if(graphicsPipelines.size()>1)
            {
                graphicsPipelines.values().forEach(pipeline -> vkDestroyPipeline(DeviceManager.device, pipeline, null));
                graphicsPipelines.clear();
            }
            this.graphicsPipelines.put(this.state, this.createGraphicsPipeline(this.state));
        }
//        PIPELINES.remove(this);
//        Renderer.getInstance().removeUsedPipeline(this);
//        this.graphicsPipelines.remove(this.state);
//        PIPELINES.add(this);
//        Renderer.getInstance().addUsedPipeline(this);
    }

    public void cleanUp() {
        vkDestroyShaderModule(DeviceManager.device, vertShaderModule, null);
        vkDestroyShaderModule(DeviceManager.device, fragShaderModule, null);

        destroyDescriptorSets();

        graphicsPipelines.forEach((state, pipeline) -> {
            vkDestroyPipeline(DeviceManager.device, pipeline, null);
        });
        graphicsPipelines.clear();

        vkDestroyDescriptorSetLayout(DeviceManager.device, descriptorSetLayout, null);
        vkDestroyPipelineLayout(DeviceManager.device, pipelineLayout, null);

        PIPELINES.remove(this);
        Renderer.getInstance().removeUsedPipeline(this);
    }
}
