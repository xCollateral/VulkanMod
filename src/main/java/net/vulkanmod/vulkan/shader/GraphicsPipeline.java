package net.vulkanmod.vulkan.shader;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.descriptor.DescriptorManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.EnumSet;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends Pipeline {

    private final Object2LongMap<PipelineState> graphicsPipelines = new Object2LongOpenHashMap<>();

    private final VertexFormat vertexFormat;
    private final EnumSet<SPIRVUtils.SpecConstant> specConstants;

    private final long vertShaderModule;
    private final long fragShaderModule;

    GraphicsPipeline(Builder builder, boolean bindless) {
        super(builder.shaderPath, bindless);
        this.buffers = builder.UBOs;
        this.manualUBO = builder.manualUBO;
        this.vertImageDescriptors = builder.vertImageDescriptors;
        this.fragImageDescriptors = builder.fragImageDescriptors;
        this.pushConstants = builder.pushConstants;
        this.vertexFormat = builder.vertexFormat;

        descriptorSetLayout = this.isBindless() ? DescriptorManager.getDescriptorSetLayout() : createDescriptorSetLayout();
        pipelineLayout = this.isBindless() ? Renderer.getLayout() : createPipelineLayout();

        this.specConstants = builder.specConstants;
        this.vertShaderModule = createShaderModule(builder.vertShaderSPIRV.bytecode());
        this.fragShaderModule = createShaderModule(builder.fragShaderSPIRV.bytecode());

        if (builder.renderPass != null)
            graphicsPipelines.computeIfAbsent(PipelineState.DEFAULT,
                    this::createGraphicsPipeline);

        if (!this.isBindless()) createDescriptorSets(Renderer.getFramesNum());

        PIPELINES.add(this);
    }

    public long getHandle(PipelineState state) {
        return graphicsPipelines.computeIfAbsent(state, this::createGraphicsPipeline);
    }

    private long createGraphicsPipeline(PipelineState state) {

        try (MemoryStack stack = stackPush()) {

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

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);
            fragShaderStageInfo.pSpecializationInfo(specInfo); //Incorrect warning: pSpecializationInfo is marked as @Nullable

            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(getBindingDescription(vertexFormat));
            vertexInputInfo.pVertexAttributeDescriptions(getAttributeDescriptions(vertexFormat));

            // ===> ASSEMBLY STAGE <===

            final int topology = PipelineState.AssemblyRasterState.decodeTopology(state.assemblyRasterState);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(topology);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);

            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            // ===> RASTERIZATION STAGE <===

            final int polygonMode = PipelineState.AssemblyRasterState.decodePolygonMode(state.assemblyRasterState);
            final int cullMode = PipelineState.AssemblyRasterState.decodeCullMode(state.assemblyRasterState);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(polygonMode);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(cullMode);
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
            depthStencil.depthTestEnable(PipelineState.DepthState.depthTest(state.depthState_i));
            depthStencil.depthWriteEnable(PipelineState.DepthState.depthMask(state.depthState_i));
            depthStencil.depthCompareOp(PipelineState.DepthState.decodeDepthFun(state.depthState_i));
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional
            depthStencil.stencilTestEnable(false);

            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(state.colorMask_i);

            if (PipelineState.BlendState.enable(state.blendState_i)) {
                colorBlendAttachment.blendEnable(true);
                colorBlendAttachment.srcColorBlendFactor(PipelineState.BlendState.getSrcRgbFactor(state.blendState_i));
                colorBlendAttachment.dstColorBlendFactor(PipelineState.BlendState.getDstRgbFactor(state.blendState_i));
                colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
                colorBlendAttachment.srcAlphaBlendFactor(PipelineState.BlendState.getSrcAlphaFactor(state.blendState_i));
                colorBlendAttachment.dstAlphaBlendFactor(PipelineState.BlendState.getDstAlphaFactor(state.blendState_i));
                colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);
            } else {
                colorBlendAttachment.blendEnable(false);
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(PipelineState.LogicOpState.enable(state.logicOp_i));
            colorBlending.logicOp(PipelineState.LogicOpState.decodeFun(state.logicOp_i));
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> DYNAMIC STATES <===

            VkPipelineDynamicStateCreateInfo dynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);

            if (topology == VK_PRIMITIVE_TOPOLOGY_LINE_LIST || polygonMode == VK_POLYGON_MODE_LINE)
                dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR, VK_DYNAMIC_STATE_LINE_WIDTH));
            else
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

            if (!Vulkan.DYNAMIC_RENDERING) {
                pipelineInfo.renderPass(state.renderPass.getId());
                pipelineInfo.subpass(0);
            } else {
                //dyn-rendering
                VkPipelineRenderingCreateInfoKHR renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack);
                renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
                renderingInfo.pColorAttachmentFormats(stack.ints(state.renderPass.getFramebuffer().getFormat()));
                renderingInfo.depthAttachmentFormat(state.renderPass.getFramebuffer().getDepthFormat());
                pipelineInfo.pNext(renderingInfo);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if (vkCreateGraphicsPipelines(DeviceManager.vkDevice, PIPELINE_CACHE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);
        }
    }


    private ByteBuffer enumSpecConstants(MemoryStack stack, VkSpecializationMapEntry.Buffer specEntrySet) {
        int i = 0, offset = 0;
        ByteBuffer byteBuffer = stack.malloc(specConstants.size()*Integer.BYTES);

        for(var specDef : specConstants)
        {
            specEntrySet.get(i)
                    .constantID(specDef.ordinal())
                    .offset(offset)
                    .size(4);

            byteBuffer.putInt(i, specDef.getValue());
            i++; offset+=4;
        }
        return byteBuffer;
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
        if (elements.stream().anyMatch(vertexFormatElement -> vertexFormatElement.getUsage() == VertexFormatElement.Usage.PADDING)) {
            size--;
        }

        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.calloc(size, stackGet());

        int offset = 0;

        for (int i = 0; i < size; ++i) {
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);

            VertexFormatElement formatElement = elements.get(i);
            VertexFormatElement.Usage usage = formatElement.getUsage();
            VertexFormatElement.Type type = formatElement.getType();
            int elementCount = formatElement.getCount();

            switch (usage) {
                case POSITION:
                    if (type == VertexFormatElement.Type.FLOAT) {
                        posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                        posDescription.offset(offset);

                        offset += 12;
                    } else if (type == VertexFormatElement.Type.SHORT) {
                        posDescription.format(VK_FORMAT_R16G16B16A16_SINT);
                        posDescription.offset(offset);

                        offset += 8;
                    } else if (type == VertexFormatElement.Type.BYTE) {
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
                    if (type == VertexFormatElement.Type.FLOAT) {
                        posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                        posDescription.offset(offset);

                        offset += 8;
                    } else if (type == VertexFormatElement.Type.SHORT) {
                        posDescription.format(VK_FORMAT_R16G16_SINT);
                        posDescription.offset(offset);

                        offset += 4;
                    } else if (type == VertexFormatElement.Type.USHORT) {
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
                    if (type == VertexFormatElement.Type.SHORT && elementCount == 1) {
                        posDescription.format(VK_FORMAT_R16_SINT);
                        posDescription.offset(offset);

                        offset += 2;
                        break;
                    } else if (type == VertexFormatElement.Type.INT && elementCount == 1) {
                        posDescription.format(VK_FORMAT_R32_SINT);
                        posDescription.offset(offset);

                        offset += 4;
                        break;
                    } else {
                        throw new RuntimeException(String.format("Unknown format: %s", usage));
                    }


                default:
                    throw new RuntimeException(String.format("Unknown format: %s", usage));
            }

            posDescription.offset(((VertexFormatMixed) (vertexFormat)).getOffset(i));
        }

        return attributeDescriptions.rewind();
    }

    public void cleanUp() {
        vkDestroyShaderModule(DeviceManager.vkDevice, vertShaderModule, null);
        vkDestroyShaderModule(DeviceManager.vkDevice, fragShaderModule, null);

        if (!isBindless()) destroyDescriptorSets();

        graphicsPipelines.forEach((state, pipeline) -> {
            vkDestroyPipeline(DeviceManager.vkDevice, pipeline, null);
        });
        graphicsPipelines.clear();

        if (!isBindless()) {
            vkDestroyDescriptorSetLayout(DeviceManager.vkDevice, descriptorSetLayout, null);
            vkDestroyPipelineLayout(DeviceManager.vkDevice, pipelineLayout, null);
        }

        PIPELINES.remove(this);
        Renderer.getInstance().removeUsedPipeline(this);
    }
}
