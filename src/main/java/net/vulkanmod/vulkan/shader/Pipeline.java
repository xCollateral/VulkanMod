package net.vulkanmod.vulkan.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.shader.SPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.shader.SPIRVUtils.ShaderKind;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.UniformBuffers;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.ManualUBO;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
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

import static net.vulkanmod.vulkan.shader.SPIRVUtils.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Pipeline {

    private static final VkDevice DEVICE = Vulkan.getDevice();
    private static final boolean hasBindlessUBOs = Vulkan.getDeviceInfo().isHasBindlessUBOs();
    protected static final long PIPELINE_CACHE = createPipelineCache();
    protected static final List<Pipeline> PIPELINES = new LinkedList<>();
//    private static final long descriptorSetLayout = Renderer.getDescriptorSetArray().getDescriptorSetLayout();

    private static long createPipelineCache() {
        try(MemoryStack stack = stackPush()) {

            VkPipelineCacheCreateInfo cacheCreateInfo = VkPipelineCacheCreateInfo.calloc(stack);
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

    public static void recreateDescriptorSets(int frames) {
//        PIPELINES.forEach(pipeline -> {
//            pipeline.destroyDescriptorSets();
//            pipeline.createDescriptorSets(frames);
//        });
    }

    public final String name;

//    protected static long descriptorSetLayout;
    protected static long pipelineLayout;

    protected DescriptorSets[] descriptorSets;
    protected List<UBO> buffers;
    protected PushConstants pushConstants;

    public Pipeline(String name) {

        this.name = name;
    }

    protected void createPipelineLayout() {
        try(MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            final long x = Renderer.getDescriptorSetArray().getDescriptorSetLayout(0);


            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(x));

            //TODO; PushConstants temp disabed to work aroudn compatiblity isues with DescriptorSet layouts

            if(this.pushConstants != null) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
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

    protected void createDescriptorSets(int frames) {
        descriptorSets = new DescriptorSets[frames];
        for(int i = 0; i < frames; ++i) {
            descriptorSets[i] = new DescriptorSets(i);
        }
    }

    public void scheduleCleanUp() {
        MemoryManager.getInstance().addFrameOp(this::cleanUp);
    }

    public abstract void cleanUp();

    public void resetDescriptorPool(int i) {
        if(this.descriptorSets != null)
        {
            this.descriptorSets[i].resetIdx();
            this.descriptorSets[i].bound=false;
        }

    }

    public PushConstants getPushConstants() { return this.pushConstants; }

    public long getLayout() { return pipelineLayout; }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, int frame, boolean shouldUpdate) {
        UniformBuffers uniformBuffers = Renderer.getDrawer().getUniformBuffers();
        this.descriptorSets[frame].updateSets(commandBuffer, uniformBuffers, VK_PIPELINE_BIND_POINT_GRAPHICS, shouldUpdate);
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, UniformBuffers uniformBuffers, int frame, boolean shouldUpdate) {
        this.descriptorSets[frame].updateSets(commandBuffer, uniformBuffers, VK_PIPELINE_BIND_POINT_GRAPHICS, shouldUpdate);
    }

    static long createShaderModule(ByteBuffer spirvCode) {

        try(MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(DEVICE, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }

    protected class DescriptorSets {
        public boolean bound = false;
        //        private long descriptorPool;
        private long uniformBufferId;
//        private long currentSet;
        private int currentIdx = -1;

        private final int frame;
//        private final ImageDescriptor.State[] boundTextures = new ImageDescriptor.State[imageDescriptors.size()];
        private final IntBuffer dynamicOffsets = MemoryUtil.memAllocInt(buffers.size());

        DescriptorSets(int frame) {
            this.frame = frame;

//            Arrays.setAll(boundTextures, i -> new ImageDescriptor.State(0, 0));

//            try(MemoryStack stack = stackPush()) {
//                this.createDescriptorPool(stack);
//                this.createDescriptorSets(stack);
//            }
        }



        protected void updateSets(VkCommandBuffer commandBuffer, UniformBuffers uniformBuffers, int bindPoint, boolean shouldUpdate) {
            try(MemoryStack stack = stackPush()) {



                if(shouldUpdate) {

//                    final boolean textureUpdate = this.transitionSamplers(uniformBuffers);

                    this.updateUniforms(uniformBuffers);
//                    this.updateDescriptorSet(stack, uniformBuffers, !this.bound);



//                 {
//                     final LongBuffer descriptorSets = Renderer.getDescriptorSetArray().getDescriptorSets();
//                     vkCmdBindDescriptorSets(commandBuffer, bindPoint, pipelineLayout,
//                             0, descriptorSets, null);
//                     this.bound = true;
//                 }
                }




            }
        }


        protected void bindSets(VkCommandBuffer commandBuffer, UniformBuffers uniformBuffers, int bindPoint) {
            try(MemoryStack stack = stackPush()) {




                    this.updateUniforms(uniformBuffers);
//                    this.updateDescriptorSet(stack, uniformBuffers, true);
                    this.bound=true;

//                    vkCmdBindDescriptorSets(commandBuffer, bindPoint, pipelineLayout,
//                            0, stack.longs(currentSet), null);



            }
        }

        private void updateUniforms(UniformBuffers uniformBuffers) {
            int currentOffset = uniformBuffers.getUsedBytes();
            //TODO: Might be possible to replace w/ BaseDeviceAddress + Pointer Arithmetic
            int i = 0;
            for(UBO ubo : buffers) {
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
//            uniformBuffers.reset();
        }

        /*private void updateDescriptorSet(MemoryStack stack, UniformBuffers uniformBuffers, boolean UBOUpdate) {


            this.uniformBufferId = uniformBuffers.getId(frame);


            final int samplerStaticSize = 2;
            final int uboStaticSize = 2;




            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(buffers.size() + imageDescriptors.size(), stack);
            VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[buffers.size()];

            //TODO maybe ubo update is not needed everytime
            int i = 0;
            int x = 0;
            for(UBO ubo : buffers) {
                final int binding = ubo.getBinding();


                long currentSet = Renderer.getDescriptorSetArray().getDescriptorSet(binding);
                bufferInfos[i] = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfos[i].buffer(this.uniformBufferId);
                bufferInfos[i].offset(x);
                bufferInfos[i].range(ubo.getSize());
                x+= UniformBuffers.getAlignedSize(ubo.getSize());
                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                uboDescriptorWrite.sType$Default();
                uboDescriptorWrite.dstBinding(binding);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(ubo.getType());
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfos[i]);
                uboDescriptorWrite.dstSet(currentSet);

                ++i;
            }

            VkDescriptorImageInfo.Buffer[] imageInfo = new VkDescriptorImageInfo.Buffer[imageDescriptors.size()];

            for(int j = 0; j < imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = image.getSampler();
                int layout = imageDescriptor.getLayout();

                  final int binding = imageDescriptor.getBinding();
                  long currentSet = Renderer.getDescriptorSetArray().getDescriptorSet(binding);

                  if(imageDescriptor.isReadOnlyLayout)
                      image.readOnlyLayout();

                  imageInfo[j] = VkDescriptorImageInfo.calloc(1, stack);
                  imageInfo[j].imageLayout(layout);
                  imageInfo[j].imageView(view);

                  if(imageDescriptor.useSampler)
                      imageInfo[j].sampler(sampler);


                  VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                  samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                  samplerDescriptorWrite.dstBinding(binding);
                  samplerDescriptorWrite.dstArrayElement(0);
                  samplerDescriptorWrite.descriptorType(imageDescriptor.getType());
                  samplerDescriptorWrite.descriptorCount(1);
                  samplerDescriptorWrite.pImageInfo(imageInfo[j]);
                  samplerDescriptorWrite.dstSet(currentSet);

                  this.boundTextures[j].set(view, sampler);
                  ++i;
              }


            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
        }*/

        /*private boolean transitionSamplers(UniformBuffers uniformBuffers) {
            boolean changed = false;
            for(int j = 0; j < imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = image.getSampler();

                if(imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                if(!this.boundTextures[j].isCurrentState(view, sampler)) {
                    changed = true;
                    break;
                }
            }

            if(!changed && this.currentIdx != -1 &&
                this.uniformBufferId == uniformBuffers.getId(frame)) {

                return false;
            }
            return true;
        }*/



        public void resetIdx() { this.currentIdx = -1; }

/*        private void cleanUp() {
            vkResetDescriptorPool(DEVICE, descriptorPool, 0);
            vkDestroyDescriptorPool(DEVICE, descriptorPool, null);
        }*/

    }

    public static class Builder {

        public static GraphicsPipeline createGraphicsPipeline(VertexFormat format, String path) {
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            return pipelineBuilder.createGraphicsPipeline();
        }

        final VertexFormat vertexFormat;
        final String shaderPath;
        List<UBO> UBOs;
        ManualUBO manualUBO;
        PushConstants pushConstants;
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

        public GraphicsPipeline createGraphicsPipeline() {
            Validate.isTrue(this.imageDescriptors != null && this.UBOs != null
                    && this.vertShaderSPIRV != null && this.fragShaderSPIRV != null,
                    "Cannot create Pipeline: resources missing");

            return new GraphicsPipeline(this);
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

            JsonObject jsonObject;

            String resourcePath = String.format("/assets/vulkanmod/shaders/%s.json", this.shaderPath);
            InputStream stream = Pipeline.class.getResourceAsStream(resourcePath);

            if(stream == null)
                throw new NullPointerException(String.format("Failed to load: %s", resourcePath));

            jsonObject = GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));

            JsonArray jsonUbos = GsonHelper.getAsJsonArray(jsonObject, "UBOs", null);
            JsonArray jsonManualUbos = GsonHelper.getAsJsonArray(jsonObject, "ManualUBOs", null);
            JsonArray jsonSamplers = GsonHelper.getAsJsonArray(jsonObject, "samplers", null);
            JsonArray jsonPushConstants = GsonHelper.getAsJsonArray(jsonObject, "PushConstants", null);

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

            if(binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.UBOs.add(ubo);
        }

        private void parseManualUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "ManualUBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int stage = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            int size = GsonHelper.getAsInt(jsonobject, "size");

            if(binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.manualUBO = new ManualUBO(binding, stage, size);
            this.UBOs.add(this.manualUBO);
        }

        private void parseSamplerNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "Sampler");
            String name = GsonHelper.getAsString(jsonobject, "name");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");

            int imageIdx = VTextureSelector.getTextureIdx(name);
            this.imageDescriptors.add(new ImageDescriptor("sampler2D", name, imageIdx));
            this.nextBinding++;
        }

        private void parsePushConstantNode(JsonArray jsonArray) {
            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for(JsonElement jsonelement : jsonArray) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement, "PC");

                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addUniformInfo(type2, name, j);
            }

            this.pushConstants = builder.buildPushConstant();
        }

        public static int getStageFromString(String s) {
            return switch (s) {
                case "vertex" -> VK_SHADER_STAGE_VERTEX_BIT;
                case "fragment" -> VK_SHADER_STAGE_FRAGMENT_BIT;
                case "all" -> VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
                case "compute" -> VK_SHADER_STAGE_COMPUTE_BIT;

                default -> throw new RuntimeException("cannot identify type..");
            };
        }
    }
}
