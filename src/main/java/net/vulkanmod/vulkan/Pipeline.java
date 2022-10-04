package net.vulkanmod.vulkan;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.UniformBuffers;
import net.vulkanmod.vulkan.shader.Field;
import net.vulkanmod.vulkan.shader.PushConstant;
import net.vulkanmod.vulkan.shader.UBO;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static net.vulkanmod.vulkan.ShaderSPIRVUtils.*;
import static net.vulkanmod.vulkan.Vulkan.getSwapChainImages;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Pipeline {
    public static final BlendState DEFAULT_BLEND_STATE = defaultBlend();
    public static final DepthState DEFAULT_DEPTH_STATE = defaultDepthState();
    public static final LogicOpState DEFAULT_LOGICOP_STATE = new LogicOpState(false, 0);
    public static final ColorMask DEFAULT_COLORMASK = new ColorMask(true, true, true, true);
    public static final BlendState NO_BLEND_STATE = new BlendState(false, 0, 0, 0, 0);

    private static final VkDevice device = Vulkan.getDevice();
    private static final long pipelineCache = createPipelineCache();
    private static final int imagesSize = getSwapChainImages().size();

    private String path;
    private long descriptorSetLayout;
    private long pipelineLayout;
    private Map<PipelineState, Long> graphicsPipelines = new HashMap<>();
    private VertexFormat vertexFormat;

    private final long[] descriptorPools = new long[imagesSize];
    private int descriptorCount = 500;

    private final List<UBO> UBOs = new ArrayList<>();
    private final List<String> samplers = new ArrayList<>();
    private final PushConstant pushConstant = new PushConstant();

    private Consumer<Integer> resetDescriptorPoolFun = defaultResetDPFun();

    private long vertShaderModule = 0;
    private long fragShaderModule = 0;

    public Pipeline(VertexFormat vertexFormat, String path) {
        this.path = path;

        createDescriptorSetLayout(path);
        createPipelineLayout();
        graphicsPipelines.computeIfAbsent(new PipelineState(DEFAULT_BLEND_STATE, DEFAULT_DEPTH_STATE, DEFAULT_LOGICOP_STATE, DEFAULT_COLORMASK),
                (pipelineState) -> createGraphicsPipeline(vertexFormat, pipelineState));
        createDescriptorPool(descriptorCount);
        //allocateDescriptorSets();

    }

    private long createGraphicsPipeline(VertexFormat vertexFormat, PipelineState state) {
        this.vertexFormat = vertexFormat;

        try(MemoryStack stack = stackPush()) {

            // Let's compile the GLSL shaders into SPIR-V at runtime using the org.lwjgl.util.shaderc library
            // Check ShaderSPIRVUtils class to see how it can be done

            if(vertShaderModule == 0 || fragShaderModule == 0) {
//                SPIRV vertShaderSPIRV = compileShader(path + ".vsh", "vertex");
//                SPIRV fragShaderSPIRV = compileShader(path + ".fsh", "fragment");

                SPIRV vertShaderSPIRV = compileShaderFile(path + ".vsh", ShaderKind.VERTEX_SHADER);
                SPIRV fragShaderSPIRV = compileShaderFile(path + ".fsh", ShaderKind.FRAGMENT_SHADER);

                vertShaderModule = createShaderModule(vertShaderSPIRV.bytecode());
                fragShaderModule = createShaderModule(fragShaderSPIRV.bytecode());
            }



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

    private void createDescriptorSetLayout(String path) {
        JsonObject jsonObject;

        Identifier id = new Identifier("modid", "shaders/" + path + ".json");
        //ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        InputStream stream = Pipeline.class.getResourceAsStream("/assets/vulkanmod/shaders/" + path + ".json");


//        InputStream stream = getSystemClassLoader().getResourceAsStream("shaders/" + path + ".json");
//        if(stream == null) throw new RuntimeException("no such file: " + "shaders/" + path);
        jsonObject = JsonHelper.deserialize(new InputStreamReader(stream, StandardCharsets.UTF_8));

        JsonArray jsonUbos = JsonHelper.getArray(jsonObject, "UBOs", (JsonArray)null);
        JsonArray jsonSamplers = JsonHelper.getArray(jsonObject, "samplers", (JsonArray)null);
        JsonArray jsonPushConstants = JsonHelper.getArray(jsonObject, "PushConstants", (JsonArray)null);

        int bindingsSize = 0; //UBOs + sampler

        if (jsonUbos != null) {

            for (JsonElement jsonelement : jsonUbos) {
                this.parseUboNode(jsonelement);
            }
            bindingsSize = jsonUbos.size();
        }

        if(jsonSamplers != null) {

            for (JsonElement jsonelement : jsonSamplers) {
                this.parseSamplerNode(jsonelement);
            }
            bindingsSize += jsonSamplers.size();
        }

        if(jsonPushConstants != null) {

            this.parsePushConstantNode(jsonPushConstants);
        }

        try(MemoryStack stack = stackPush()) {

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

            if(pushConstant.getSize() > 0) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.callocStack(1, stack);
                pushConstantRange.size(pushConstant.getSize());
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
        if(elements.stream().anyMatch(vertexFormatElement -> vertexFormatElement.getType() == VertexFormatElement.Type.PADDING)) {
            size--;
        }

        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.callocStack(size);

        int offset = 0;

        for(int i = 0; i < size; ++i) {
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);

            VertexFormatElement.Type type = elements.get(i).getType();
            if (type == VertexFormatElement.Type.POSITION)
            {
                posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                posDescription.offset(offset);

                offset += 12;
            }
            else if (type == VertexFormatElement.Type.COLOR)
            {
//                posDescription.format(VK_FORMAT_R32G32B32A32_SFLOAT);
                posDescription.format(VK_FORMAT_R8G8B8A8_UNORM);
                posDescription.offset(offset);

//                offset += 16;
                offset += 4;
            }
            else if (type == VertexFormatElement.Type.UV)
            {
                if(elements.get(i).getDataType() == VertexFormatElement.DataType.FLOAT){
                    posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                    posDescription.offset(offset);

                    offset += 8;
                }
                else if(elements.get(i).getDataType() == VertexFormatElement.DataType.SHORT){
                    posDescription.format(VK_FORMAT_R16G16_SINT);
                    posDescription.offset(offset);

                    offset += 4;
                }
            }
            else if (type == VertexFormatElement.Type.NORMAL)
            {
//                posDescription.format(VK_FORMAT_R8G8B8_UNORM);
                posDescription.format(VK_FORMAT_R8G8B8A8_SNORM);
                posDescription.offset(offset);

                offset += 3;
            }
            else if (type == VertexFormatElement.Type.PADDING)
            {
//                posDescription.format(VK_FORMAT_R8_UNORM);
//                posDescription.offset(offset);

                offset += 1;
            }

            else {
                throw new RuntimeException("Unknown format:");
            }

            posDescription.offset(((VertexFormatMixed)(vertexFormat)).getOffset(i));
        }

        return attributeDescriptions.rewind();
    }

    private void parseUboNode(JsonElement jsonelement) {
        JsonObject jsonobject = JsonHelper.asObject(jsonelement, "UBO");
        int binding = JsonHelper.getInt(jsonobject, "binding");
        int type = this.getTypeFromString(JsonHelper.getString(jsonobject, "type"));
        JsonArray fields = JsonHelper.getArray(jsonobject, "fields");

        UBO ubo = new UBO(binding, type);


        for (JsonElement jsonelement2 : fields) {
            JsonObject jsonobject2 = JsonHelper.asObject(jsonelement2, "uniform");
            //need to store some infos
            String name = JsonHelper.getString(jsonobject2, "name");
            String type2 = JsonHelper.getString(jsonobject2, "type");
            int j = JsonHelper.getInt(jsonobject2, "count");

            ubo.addField(Field.createField(type2, name, j, ubo));

        }

        UBOs.add(ubo);
    }

    private void parseSamplerNode(JsonElement jsonelement) {
        JsonObject jsonobject = JsonHelper.asObject(jsonelement, "UBO");
        String name = JsonHelper.getString(jsonobject, "name");

        samplers.add(name);
    }

    private void parsePushConstantNode(JsonArray jsonArray) {

        for(JsonElement jsonelement : jsonArray) {
            JsonObject jsonobject2 = JsonHelper.asObject(jsonelement, "PC");

            String name = JsonHelper.getString(jsonobject2, "name");
            String type2 = JsonHelper.getString(jsonobject2, "type");
            int j = JsonHelper.getInt(jsonobject2, "count");

            pushConstant.addField(Field.createField(type2, name, j, pushConstant));
        }

        pushConstant.allocateBuffer();
    }

    private int getTypeFromString(String s) {
        return switch (s) {
            case "vertex" -> VK_SHADER_STAGE_VERTEX_BIT;
            case "fragment" -> VK_SHADER_STAGE_FRAGMENT_BIT;
            case "all" -> VK_SHADER_STAGE_ALL_GRAPHICS;

            default -> throw new RuntimeException("cannot identify type..");
        };
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
            return createGraphicsPipeline(this.vertexFormat, state1);
        });
    }

    public PushConstant getPushConstant() { return pushConstant; }

    public long getLayout() { return pipelineLayout; }

    private class DescriptorSetsUnit {
        private long descriptorSet;

        private DescriptorSetsUnit(VkCommandBuffer commandBuffer, int frame, UniformBuffers uniformBuffers) {

            allocateDescriptorSet(frame);
            updateDescriptorSet(commandBuffer, frame, uniformBuffers);
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

        private void updateDescriptorSet(VkCommandBuffer commandBuffer, int frame, UniformBuffers uniformBuffers) {
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

                VkDescriptorImageInfo.Buffer[] imageInfos = new VkDescriptorImageInfo.Buffer[samplers.size()];

                if(samplers.size() > 0) {
                    //TODO: make an auxiliary function
                    VulkanImage texture = VTextureSelector.getBoundTexture();
                    texture.readOnlyLayout();
                    //VulkanImage texture = VTextureManager.getCurrentTexture();

                    imageInfos[0] = VkDescriptorImageInfo.callocStack(1, stack);
                    imageInfos[0].imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                    imageInfos[0].imageView(texture.getTextureImageView());
                    imageInfos[0].sampler(texture.getTextureSampler());

                    VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                    samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                    samplerDescriptorWrite.dstBinding(i);
                    samplerDescriptorWrite.dstArrayElement(0);
                    samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                    samplerDescriptorWrite.descriptorCount(1);
                    samplerDescriptorWrite.pImageInfo(imageInfos[0]);

                    ++i;

                    if (samplers.size() > 1) {
                        texture = VTextureSelector.getLightTexture();
                        texture.readOnlyLayout();

                        imageInfos[1] = VkDescriptorImageInfo.callocStack(1, stack);
                        imageInfos[1].imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                        imageInfos[1].imageView(texture.getTextureImageView());
                        imageInfos[1].sampler(texture.getTextureSampler());

                        samplerDescriptorWrite = descriptorWrites.get(i);
                        samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                        samplerDescriptorWrite.dstBinding(i);
                        samplerDescriptorWrite.dstArrayElement(0);
                        samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                        samplerDescriptorWrite.descriptorCount(1);
                        samplerDescriptorWrite.pImageInfo(imageInfos[1]);

                        ++i;

                        if(samplers.size() > 2) {
                            texture = VTextureSelector.getOverlayTexture();
                            texture.readOnlyLayout();

                            imageInfos[2] = VkDescriptorImageInfo.callocStack(1, stack);
                            imageInfos[2].imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                            imageInfos[2].imageView(texture.getTextureImageView());
                            imageInfos[2].sampler(texture.getTextureSampler());

                            samplerDescriptorWrite = descriptorWrites.get(i);
                            samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                            samplerDescriptorWrite.dstBinding(i);
                            samplerDescriptorWrite.dstArrayElement(0);
                            samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                            samplerDescriptorWrite.descriptorCount(1);
                            samplerDescriptorWrite.pImageInfo(imageInfos[2]);
                        }
                    }
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
    }


    public static BlendState defaultBlend() {
        return new BlendState(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
    }

    public static DepthState defaultDepthState() {
        return new DepthState(true, true, 515);
    }

    public static ColorMask defaultColorMask() { return new ColorMask(true, true, true, true); }

    public static class PipelineState {
        final BlendState blendState;
        final DepthState depthState;
        final ColorMask colorMask;
        final LogicOpState logicOpState;
        final boolean cullState;

        public PipelineState(BlendState blendState, DepthState depthState, LogicOpState logicOpState, ColorMask colorMask) {
            this.blendState = blendState;
            this.depthState = depthState;
            this.logicOpState = logicOpState;
            this.colorMask = colorMask;
            this.cullState = VRenderSystem.cull;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PipelineState that = (PipelineState) o;
            return blendState.equals(that.blendState) && depthState.equals(that.depthState) && logicOpState.equals(that.logicOpState) && (cullState == that.cullState) && colorMask.equals(that.colorMask);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blendState, depthState, logicOpState, cullState);
        }
    }

    public static class BlendState {
        public final boolean enabled;
        public final int srcRgbFactor;
        public final int dstRgbFactor;
        public final int srcAlphaFactor;
        public final int dstAlphaFactor;
        public final int blendOp = 0;

        private BlendState(boolean enabled, GlStateManager.SrcFactor srcRgb, GlStateManager.DstFactor dstRgb, GlStateManager.SrcFactor srcAlpha, GlStateManager.DstFactor dstAlpha) {
            this.enabled = enabled;

            this.srcRgbFactor = glToVulkan(srcRgb.value);
            this.dstRgbFactor = glToVulkan(dstRgb.value);
            this.srcAlphaFactor = glToVulkan(srcAlpha.value);
            this.dstAlphaFactor = glToVulkan(dstAlpha.value);
        }

        public BlendState(GlStateManager.SrcFactor srcRgb, GlStateManager.DstFactor dstRgb, GlStateManager.SrcFactor srcAlpha, GlStateManager.DstFactor dstAlpha) {
            this(true, srcRgb, dstRgb, srcAlpha, dstAlpha);
        }

        public BlendState(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this(true, glToVulkan(srcRgb), glToVulkan(dstRgb), glToVulkan(srcAlpha), glToVulkan(dstAlpha));
        }

        protected BlendState(boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this.enabled = enabled;
            this.srcRgbFactor = srcRgb;
            this.dstRgbFactor = dstRgb;
            this.srcAlphaFactor = srcAlpha;
            this.dstAlphaFactor = dstAlpha;
        }

        private static int glToVulkan(int value) {
            return switch (value) {
                case 1 -> VK_BLEND_FACTOR_ONE;
                case 0 -> VK_BLEND_FACTOR_ZERO;
                case 771 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
                case 770 -> VK_BLEND_FACTOR_SRC_ALPHA;
                case 775 -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
                case 769 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
                case 774 -> VK_BLEND_FACTOR_DST_COLOR;
                case 768 -> VK_BLEND_FACTOR_SRC_COLOR;
                default -> throw new RuntimeException("unknown blend factor..");


//                        CONSTANT_ALPHA(32771),
//                        CONSTANT_COLOR(32769),
//                        DST_ALPHA(772),
//                        DST_COLOR(774),
//                        ONE(1),
//                        ONE_MINUS_CONSTANT_ALPHA(32772),
//                        ONE_MINUS_CONSTANT_COLOR(32770),
//                        ONE_MINUS_DST_ALPHA(773),
//                        ONE_MINUS_DST_COLOR(775),
//                        ONE_MINUS_SRC_ALPHA(771),
//                        ONE_MINUS_SRC_COLOR(769),
//                        SRC_ALPHA(770),
//                        SRC_ALPHA_SATURATE(776),
//                        SRC_COLOR(768),
//                        ZERO(0);
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlendState blendState = (BlendState) o;
            if(this.enabled != blendState.enabled) return false;
            return srcRgbFactor == blendState.srcRgbFactor && dstRgbFactor == blendState.dstRgbFactor && srcAlphaFactor == blendState.srcAlphaFactor && dstAlphaFactor == blendState.dstAlphaFactor && blendOp == blendState.blendOp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(srcRgbFactor, dstRgbFactor, srcAlphaFactor, dstAlphaFactor, blendOp);
        }
    }

    public static class LogicOpState {
        public final boolean enabled;
        private int logicOp;

        public LogicOpState(boolean enable, int op) {
            this.enabled = enable;
            this.logicOp = op;
        }

        public void setLogicOp(GlStateManager.LogicOp logicOp) {
            switch (logicOp) {
                case OR_REVERSE -> setLogicOp(VK_LOGIC_OP_OR_REVERSE);
            }

        }

        public void setLogicOp(int logicOp) {
            this.logicOp = logicOp;
        }

        public int getLogicOp() {
            return logicOp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LogicOpState logicOpState = (LogicOpState) o;
            if(this.enabled != logicOpState.enabled) return false;
            return logicOp == logicOpState.logicOp;
        }

        public int hashCode() {
            return Objects.hash(enabled, logicOp);
        }
    }

    public static class ColorMask {
        public final int colorMask;

        public ColorMask(boolean r, boolean g, boolean b, boolean a) {
            this.colorMask = (r ? VK_COLOR_COMPONENT_R_BIT : 0) | (g ? VK_COLOR_COMPONENT_G_BIT : 0) | (b ? VK_COLOR_COMPONENT_B_BIT : 0) | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        public ColorMask(int mask) {
            this.colorMask = mask;
        }

        public static int getColorMask(boolean r, boolean g, boolean b, boolean a) {
            return (r ? VK_COLOR_COMPONENT_R_BIT : 0) | (g ? VK_COLOR_COMPONENT_G_BIT : 0) | (b ? VK_COLOR_COMPONENT_B_BIT : 0) | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColorMask colorMask = (ColorMask) o;
            return this.colorMask == colorMask.colorMask;
        }
    }

    public static class DepthState {
        public final boolean depthTest;
        public final boolean depthMask;
        public final int function;

        public DepthState(boolean depthTest, boolean depthMask, int function) {
            this.depthTest = depthTest;
            this.depthMask = depthMask;
            this.function = glToVulkan(function);
        }

        private int glToVulkan(int value) {
            return switch (value) {
                case 515 -> VK_COMPARE_OP_LESS_OR_EQUAL;
                case 519 -> VK_COMPARE_OP_ALWAYS;
                case 516 -> VK_COMPARE_OP_GREATER;
                case 518 -> VK_COMPARE_OP_GREATER_OR_EQUAL;
                case 514 -> VK_COMPARE_OP_EQUAL;
                default -> throw new RuntimeException("unknown blend factor..");


//                public static final int GL_NEVER = 512;
//                public static final int GL_LESS = 513;
//                public static final int GL_EQUAL = 514;
//                public static final int GL_LEQUAL = 515;
//                public static final int GL_GREATER = 516;
//                public static final int GL_NOTEQUAL = 517;
//                public static final int GL_GEQUAL = 518;
//                public static final int GL_ALWAYS = 519;
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DepthState that = (DepthState) o;
            return depthTest == that.depthTest && depthMask == that.depthMask && function == that.function;
        }

        @Override
        public int hashCode() {
            return Objects.hash(depthTest, depthMask, function);
        }
    }
}
