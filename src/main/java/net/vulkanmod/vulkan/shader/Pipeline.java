package net.vulkanmod.vulkan.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.UniformBuffer;
import net.vulkanmod.vulkan.shader.SPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.shader.SPIRVUtils.ShaderKind;
import net.vulkanmod.vulkan.shader.descriptor.*;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.VUtil;
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

import static net.vulkanmod.vulkan.shader.SPIRVUtils.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Pipeline {

    private static final VkDevice DEVICE = Vulkan.getVkDevice();
    protected static final long PIPELINE_CACHE = createPipelineCache();
    protected static final List<Pipeline> PIPELINES = new LinkedList<>();
    final int setID;
    private static final Int2ObjectOpenHashMap<InlineUniformBlock> UniformBlockMap = new Int2ObjectOpenHashMap<>(32);
    private static final Int2IntOpenHashMap UniformBaseOffsetMap = new Int2IntOpenHashMap(32);

    private static long createPipelineCache() {
        try (MemoryStack stack = stackPush()) {

            VkPipelineCacheCreateInfo cacheCreateInfo = VkPipelineCacheCreateInfo.calloc(stack);
            cacheCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            LongBuffer pPipelineCache = stack.mallocLong(1);

            if (vkCreatePipelineCache(DEVICE, cacheCreateInfo, null, pPipelineCache) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pPipelineCache.get(0);
        }
    }

    public static void destroyPipelineCache() {
        vkDestroyPipelineCache(DEVICE, PIPELINE_CACHE, null);
    }

    public static void recreateDescriptorSets(int frames) {
        PIPELINES.stream().filter(pipeline -> !pipeline.bindless).forEach(pipeline -> {
            pipeline.destroyDescriptorSets();
            pipeline.createDescriptorSets(frames);
        });
    }

    public final String name;
    private final boolean bindless;
    private static int lastPushConstantState;

    protected long descriptorSetLayout;
    protected long pipelineLayout;

    protected BindfulDescriptorSets[] bindfulDescriptorSets;
    protected List<UBO> buffers;
    protected ManualUBO manualUBO;
    protected List<ImageDescriptor> imageDescriptors;
    protected List<PushConstants> pushConstants;

    public boolean isBindless() {
        return bindless;
    }

    public int getSetID() {
        return setID;
    }

    public Pipeline(String name, boolean bindless) {
        this.name = name;
        this.bindless = this.name==null ? false : name.contains("blit") ? false : bindless;


        setID = this.name!=null && this.name.contains("terrain") ? 1 : 0;
    }

    protected long createDescriptorSetLayout() {
        try (MemoryStack stack = stackPush()) {
            int bindingsSize = this.buffers.size() + imageDescriptors.size();

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindingsSize, stack);

            for (UBO ubo : this.buffers) {
                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(ubo.getBinding());
                uboLayoutBinding.binding(ubo.getBinding());
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(ubo.getType());
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(ubo.getStages());
            }

            for (ImageDescriptor imageDescriptor : this.imageDescriptors) {
                VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(imageDescriptor.getBinding());
                samplerLayoutBinding.binding(imageDescriptor.getBinding());
                samplerLayoutBinding.descriptorCount(1);
                samplerLayoutBinding.descriptorType(imageDescriptor.getType());
                samplerLayoutBinding.pImmutableSamplers(null);
                samplerLayoutBinding.stageFlags(imageDescriptor.getStages());
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(DeviceManager.vkDevice, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }

            return pDescriptorSetLayout.get(0);
        }
    }

    protected long createPipelineLayout() {
        try (MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(this.descriptorSetLayout));
            //Mod + PostEffect Shaders do not use PushConstants

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if (vkCreatePipelineLayout(DEVICE, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            return pPipelineLayout.get(0);
        }
    }

    protected void createDescriptorSets(int frames) {
        bindfulDescriptorSets = new BindfulDescriptorSets[frames];
        for (int i = 0; i < frames; ++i) {
            bindfulDescriptorSets[i] = new BindfulDescriptorSets(this);
        }
    }

    public void scheduleCleanUp() {
        MemoryManager.getInstance().addFrameOp(this::cleanUp);
    }

    public abstract void cleanUp();

    void destroyDescriptorSets() {
        for (BindfulDescriptorSets bindfulDescriptorSets : this.bindfulDescriptorSets) {
            bindfulDescriptorSets.cleanUp();
        }

        this.bindfulDescriptorSets = null;
    }

    public ManualUBO getManualUBO() {
        return this.manualUBO;
    }

    public void resetDescriptorPool(int i) {
        if (this.bindfulDescriptorSets != null)
            this.bindfulDescriptorSets[i].resetIdx();

    }

//    public PushConstants getPushConstants() {
//        return this.pushConstants;
//    }

    public long getLayout() {
        return pipelineLayout;
    }

    public List<ImageDescriptor> getImageDescriptors() {
        return imageDescriptors;
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, int frame) {
        UniformBuffer uniformBuffer = Renderer.getDrawer().getAuxUniformBuffer();
        this.bindfulDescriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS);
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int frame) {
        this.bindfulDescriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS);
    }

    static long createShaderModule(ByteBuffer spirvCode) {

        try (MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if (vkCreateShaderModule(DEVICE, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }

    public int updateImageState() {
        if(!bindless) return 0;
        //TODO: move Texture registration to VTextureSelector to allow Async texture updates + reduced CPu overhead during rendering
        //                  VulkanImage vulkanImage = VTextureSelector.getBoundTexture(state.imageIdx);
        boolean isNewTexture = false;

        for (ImageDescriptor state : imageDescriptors) {

            //get the currently atcive TextureIDs
            final int shaderTexture = RenderSystem.getShaderTexture(state.imageIdx);

            if(shaderTexture != 0) {

                //TODO: maybe map >0 ShaderTextre indicies/slots diertcly (w. no Tetxure IDs needed)_ to VretOnlySMapler Slots (i.e. more Immutable fiendly)
                //Add texture to the DescriptorSet if its new.unique
                DescriptorManager.registerTexture(setID, state.imageIdx, shaderTexture);

                //Convert TextureID to Sampler Index

                VTextureSelector.setSamplerIndex(state.imageIdx, DescriptorManager.getTexture(setID, state.imageIdx, shaderTexture));
                isNewTexture |= DescriptorManager.isTexUnInitialised(setID, shaderTexture);
            }


        }
        //Skip rendering if the texture is new/updated (Can be fixed with Update after bind)
        return isNewTexture ? -1 : VTextureSelector.getSamplerIndex(0);

    }


    public void pushUniforms(UniformBuffer uniformBuffers) {


        int uniformAggregateHash = 0;
        for (Uniform a : this.buffers.get(0).getUniforms()) {
            uniformAggregateHash += UniformState.valueOf(a.getName()).getCurrentHash();
        }
       if(!UniformBaseOffsetMap.containsKey(uniformAggregateHash))
       {
           int uniformBlockOffset = uniformBuffers.getBlockOffset();
           UniformBaseOffsetMap.put(uniformAggregateHash, uniformBlockOffset);

           int blockSize_t = 0;
           for (Uniform a : this.buffers.get(0).getUniforms()) {
               UniformState.valueOf(a.getName()).uploadUniform(uniformBuffers, blockSize_t);
               blockSize_t+=a.getSize()*4;
           }

           uniformBuffers.updateOffset(blockSize_t);
       }


        Renderer.getDrawer().updateUniformOffset(UniformBaseOffsetMap.get(uniformAggregateHash)/64);




    }

    public static void reset()
    {
        lastPushConstantState=0;
        UniformBaseOffsetMap.clear();
    }

    public void pushConstants(VkCommandBuffer commandBuffer) {

        if (this.pushConstants != null) {
            int currentAggregateHash = getCurrentAggregateHash(); //Skip calling PushConstant if hash is the same (reduces constant memory spilling + CPU usage)
            if (currentAggregateHash != lastPushConstantState) {
                for (PushConstants pushConstant : pushConstants) {
                    int stage = pushConstant.getStage();
                    int offset = stage == VK_SHADER_STAGE_VERTEX_BIT ? 0 : 32;
                    for (Uniform uniform : pushConstant.getUniforms()) {
                        UniformState uniformState = UniformState.valueOf(uniform.getName());

                        final long ptr = uniformState.ptr();
                        switch (uniformState) {
                            case USE_FOG -> VUtil.UNSAFE.putInt(ptr, RenderSystem.getShaderFogStart() == Float.MAX_VALUE ? 0 : 1);
                            case LineWidth -> VUtil.UNSAFE.putFloat(ptr, RenderSystem.getShaderLineWidth());
                        }
                        nvkCmdPushConstants(commandBuffer, this.pipelineLayout, stage, offset, uniformState.getByteSize(), ptr);
                        offset += uniformState.getByteSize();
                    }

                }
                lastPushConstantState = currentAggregateHash;
            }
        }
    }


    private int getCurrentAggregateHash() {
        int aggregatePushConstantHash = 0;
        for (PushConstants pushConstant : this.pushConstants) {
            for (Uniform uniform : pushConstant.getUniforms()) {
                aggregatePushConstantHash += UniformState.valueOf(uniform.getName()).getCurrentHash();
            }
        }
        return aggregatePushConstantHash;
    }



    protected static class BindfulDescriptorSets {
        private final Pipeline pipeline;
        private int poolSize = 10;
        private long descriptorPool;
        private LongBuffer sets;
        private long currentSet;
        private int currentIdx = -1;

        private final long[] boundUBs;
        private final ImageDescriptor.State[] boundTextures;
        private final IntBuffer dynamicOffsets;

        BindfulDescriptorSets(Pipeline pipeline) {
            this.pipeline = pipeline;
            this.boundTextures = new ImageDescriptor.State[pipeline.imageDescriptors.size()];
            this.dynamicOffsets = MemoryUtil.memAllocInt(pipeline.buffers.size());
            this.boundUBs = new long[pipeline.buffers.size()];

            Arrays.setAll(boundTextures, i -> new ImageDescriptor.State(0, 0));

            try (MemoryStack stack = stackPush()) {
                this.createDescriptorPool(stack);
                this.createDescriptorSets(stack);
            }
        }

        protected void bindSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int bindPoint) {
            try (MemoryStack stack = stackPush()) {

                this.updateUniforms(uniformBuffer);
                this.updateDescriptorSet(stack, uniformBuffer);

                vkCmdBindDescriptorSets(commandBuffer, bindPoint, pipeline.pipelineLayout,
                        0, stack.longs(currentSet), dynamicOffsets);
            }
        }

        private void updateUniforms(UniformBuffer globalUB) {
            int i = 0;
            for (UBO ubo : pipeline.buffers) {
                boolean useOwnUB = ubo.getUniformBuffer() != null;
                UniformBuffer ub = useOwnUB ? ubo.getUniformBuffer() : globalUB;

                int currentOffset = (int) ub.getUsedBytes();
                this.dynamicOffsets.put(i, currentOffset);

                // TODO: non mappable memory

                int alignedSize = UniformBuffer.getAlignedSize(ubo.getSize());
                ub.checkCapacity(alignedSize);

                if (!useOwnUB) {
                    ubo.update(ub.getPointer());
                    ub.updateOffset(alignedSize);
                }

                ++i;
            }
        }

        private boolean needsUpdate(UniformBuffer uniformBuffer) {
            if (currentIdx == -1)
                return true;

            for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = image.getSampler();

                if (imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                if (!this.boundTextures[j].isCurrentState(view, sampler)) {
                    return true;
                }
            }

            for (int j = 0; j < pipeline.buffers.size(); ++j) {
                UBO ubo = pipeline.buffers.get(j);
                UniformBuffer uniformBufferI = ubo.getUniformBuffer();

                if (uniformBufferI == null)
                    uniformBufferI = uniformBuffer;

                if (this.boundUBs[j] != uniformBufferI.getId()) {
                    return true;
                }
            }

            return false;
        }

        private void checkPoolSize(MemoryStack stack) {
            if (this.currentIdx >= this.poolSize) {
                this.poolSize *= 2;

                this.createDescriptorPool(stack);
                this.createDescriptorSets(stack);
                this.currentIdx = 0;

                //debug
//                System.out.println("resized descriptor pool to: " + this.poolSize);
            }
        }

        private void updateDescriptorSet(MemoryStack stack, UniformBuffer uniformBuffer) {

            //Check if update is needed
            if (!needsUpdate(uniformBuffer))
                return;

            this.currentIdx++;

            //Check pool size
            checkPoolSize(stack);

            this.currentSet = this.sets.get(this.currentIdx);

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(pipeline.buffers.size() + pipeline.imageDescriptors.size(), stack);
            VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[pipeline.buffers.size()];

            //TODO maybe ubo update is not needed everytime
            int i = 0;
            for (UBO ubo : pipeline.buffers) {
                UniformBuffer ub = ubo.getUniformBuffer();
                if (ub == null)
                    ub = uniformBuffer;
                boundUBs[i] = ub.getId();

                bufferInfos[i] = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfos[i].buffer(boundUBs[i]);
                bufferInfos[i].range(ubo.getSize());

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                uboDescriptorWrite.sType$Default();
                uboDescriptorWrite.dstBinding(ubo.getBinding());
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(ubo.getType());
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfos[i]);
                uboDescriptorWrite.dstSet(currentSet);

                ++i;
            }

            VkDescriptorImageInfo.Buffer[] imageInfo = new VkDescriptorImageInfo.Buffer[pipeline.imageDescriptors.size()];

            for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = image.getSampler();
                int layout = imageDescriptor.getLayout();

                if (imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                imageInfo[j] = VkDescriptorImageInfo.calloc(1, stack);
                imageInfo[j].imageLayout(layout);
                imageInfo[j].imageView(view);

                if (imageDescriptor.useSampler)
                    imageInfo[j].sampler(sampler);

                VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(imageDescriptor.getBinding());
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(imageDescriptor.getType());
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo[j]);
                samplerDescriptorWrite.dstSet(currentSet);

                this.boundTextures[j].set(view, sampler);
                ++i;
            }

            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
        }

        private void createDescriptorSets(MemoryStack stack) {
            LongBuffer layout = stack.mallocLong(this.poolSize);
//            layout.put(0, descriptorSetLayout);

            for (int i = 0; i < this.poolSize; ++i) {
                layout.put(i, pipeline.descriptorSetLayout);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
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
            int size = pipeline.buffers.size() + pipeline.imageDescriptors.size();

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(size, stack);

            int i;
            for (i = 0; i < pipeline.buffers.size(); ++i) {
                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(i);
//                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
                uniformBufferPoolSize.descriptorCount(this.poolSize);
            }

            for (; i < pipeline.buffers.size() + pipeline.imageDescriptors.size(); ++i) {
                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(i);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(this.poolSize);
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(this.poolSize);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if (vkCreateDescriptorPool(DEVICE, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            if (this.descriptorPool != VK_NULL_HANDLE) {
                final long oldDescriptorPool = this.descriptorPool;
                MemoryManager.getInstance().addFrameOp(() -> {
                    vkDestroyDescriptorPool(DEVICE, oldDescriptorPool, null);
                });
            }

            this.descriptorPool = pDescriptorPool.get(0);
        }

        public void resetIdx() {
            this.currentIdx = -1;
        }

        private void cleanUp() {
            vkResetDescriptorPool(DEVICE, descriptorPool, 0);
            vkDestroyDescriptorPool(DEVICE, descriptorPool, null);

            MemoryUtil.memFree(this.dynamicOffsets);
        }

    }

    public static class Builder {

        final EnumSet<SpecConstant> specConstants = EnumSet.noneOf(SpecConstant.class);

        public static GraphicsPipeline createGraphicsPipeline(VertexFormat format, String path) {
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            return pipelineBuilder.createGraphicsPipeline(true);
        }

        final VertexFormat vertexFormat;
        final String shaderPath;
        List<UBO> UBOs;
        ManualUBO manualUBO;
        List<PushConstants> pushConstants;
        List<ImageDescriptor> imageDescriptors;
        int nextBinding;

        SPIRV vertShaderSPIRV;
        SPIRV fragShaderSPIRV;

        RenderPass renderPass;

        public Builder(VertexFormat vertexFormat, String path) {
            this.vertexFormat = vertexFormat;
            this.shaderPath = path;
        }

        public Builder(VertexFormat vertexFormat) {
            this(vertexFormat, null);
        }

        public GraphicsPipeline createGraphicsPipeline(boolean bindless) {
            Validate.isTrue(this.imageDescriptors != null && this.UBOs != null
                            && this.vertShaderSPIRV != null && this.fragShaderSPIRV != null,
                    "Cannot create Pipeline: resources missing");

            if (this.manualUBO != null)
                this.UBOs.add(this.manualUBO);

            return new GraphicsPipeline(this, bindless);
        }

        public void setUniforms(List<UBO> UBOs, List<ImageDescriptor> imageDescriptors) {
            this.UBOs = UBOs;
            this.imageDescriptors = imageDescriptors;
        }

        public void setSPIRVs(SPIRV vertShaderSPIRV, SPIRV fragShaderSPIRV) {
            this.vertShaderSPIRV = vertShaderSPIRV;
            this.fragShaderSPIRV = fragShaderSPIRV;
        }

        public void compileShaders() {
            String resourcePath = SPIRVUtils.class.getResource("/assets/vulkanmod/shaders/").toExternalForm();

            this.vertShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.vsh", resourcePath, this.shaderPath), ShaderKind.VERTEX_SHADER);
            this.fragShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.fsh", resourcePath, this.shaderPath), ShaderKind.FRAGMENT_SHADER);
        }

        public void compileShaders(String name, String vsh, String fsh) {
            this.vertShaderSPIRV = compileShader(String.format("%s.vsh", name), vsh, ShaderKind.VERTEX_SHADER);
            this.fragShaderSPIRV = compileShader(String.format("%s.fsh", name), fsh, ShaderKind.FRAGMENT_SHADER);
        }

        public void parseBindingsJSON() {
            Validate.notNull(this.shaderPath, "Cannot parse bindings: shaderPath is null");

            this.UBOs = new ArrayList<>();
            this.imageDescriptors = new ArrayList<>();
            this.pushConstants = new ArrayList<>();

            JsonObject jsonObject;

            String resourcePath = String.format("/assets/vulkanmod/shaders/%s.json", this.shaderPath);
            InputStream stream = Pipeline.class.getResourceAsStream(resourcePath);

            if (stream == null)
                throw new NullPointerException(String.format("Failed to load: %s", resourcePath));

            jsonObject = GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));

            JsonArray jsonUbos = GsonHelper.getAsJsonArray(jsonObject, "UBOs", null);
            JsonArray jsonManualUbos = GsonHelper.getAsJsonArray(jsonObject, "ManualUBOs", null);
            JsonArray jsonSamplers = GsonHelper.getAsJsonArray(jsonObject, "samplers", null);
            JsonArray jsonPushConstants = GsonHelper.getAsJsonArray(jsonObject, "PushConstants", null);
            JsonArray jsonSpecConstants = GsonHelper.getAsJsonArray(jsonObject, "SpecConstants", null);

            if (jsonUbos != null) {
                for (JsonElement jsonelement : jsonUbos) {
                    this.parseUboNode(jsonelement);
                }
            }

            if (jsonManualUbos != null) {
                this.parseManualUboNode(jsonManualUbos.get(0));
            }

            if (jsonSamplers != null) {
                for (JsonElement jsonelement : jsonSamplers) {
                    this.parseSamplerNode(jsonelement);
                }
            }

            if (jsonPushConstants != null) {
                for (JsonElement jsonelement : jsonPushConstants) {
                    this.parsePushConstantNode(jsonelement);
                }
            }
            if(jsonSpecConstants != null) {
                this.parseSpecConstantNode(jsonSpecConstants);
            }
        }

        private void parseSpecConstantNode(JsonArray jsonSpecConstants) {
//            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for(JsonElement jsonelement : jsonSpecConstants) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement, "SC");

                String name = GsonHelper.getAsString(jsonobject2, "name");
//                String type2 = GsonHelper.getAsString(jsonobject2, "type");


                this.specConstants.add(SpecConstant.valueOf(name));
            }

        }

        private void parseUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "UBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int type = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            JsonArray fields = GsonHelper.getAsJsonArray(jsonobject, "fields");

            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for (JsonElement jsonelement2 : fields) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement2, "uniform");
                //need to store some infos
                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addUniformInfo(type2, name, j);

            }
            UBO ubo = builder.buildUBO(binding, type);

            if (binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.UBOs.add(ubo);
        }

        private void parseManualUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "ManualUBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int stage = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            int size = GsonHelper.getAsInt(jsonobject, "size");

            if (binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.manualUBO = new ManualUBO(binding, stage, size);
        }

        private void parseSamplerNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "Sampler");
            String name = GsonHelper.getAsString(jsonobject, "name");

            int imageIdx = VTextureSelector.getTextureIdx(name);
            this.imageDescriptors.add(new ImageDescriptor(this.nextBinding, "sampler2D", name, imageIdx));
            this.nextBinding++;
        }

        private void parsePushConstantNode(JsonElement jsonelement) {
            AlignedStruct.Builder builder = new AlignedStruct.Builder();
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "PC");
            int stage = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            JsonArray fields = GsonHelper.getAsJsonArray(jsonobject, "fields");

            for (JsonElement ConstantUniform : fields) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(ConstantUniform, "ConstantUniform");

                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addUniformInfo(type2, name, j);
            }

            this.pushConstants.add(builder.buildPushConstant(stage));
        }

        public static int getStageFromString(String s) {
            return switch (s) {
                case "vertex" -> VK_SHADER_STAGE_VERTEX_BIT;
                case "fragment" -> VK_SHADER_STAGE_FRAGMENT_BIT;
                case "all" -> VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT; //VK_SHADER_STAGE_ALL_GRAPHICS includes tessellation/geometry stages, which are unused
                case "compute" -> VK_SHADER_STAGE_COMPUTE_BIT;

                default -> throw new RuntimeException("cannot identify type..");
            };
        }
    }
}
