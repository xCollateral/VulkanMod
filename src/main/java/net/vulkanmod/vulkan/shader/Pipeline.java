package net.vulkanmod.vulkan.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.shader.SPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.shader.SPIRVUtils.ShaderKind;
import net.vulkanmod.vulkan.memory.UniformBuffers;
import net.vulkanmod.vulkan.shader.descriptor.DescriptorSetArray;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.ManualUBO;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.VUtil;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
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

    public final String name;

//    protected static long descriptorSetLayout;
    protected static final long pipelineLayout = createPipelineLayout();

    protected DescriptorSets[] descriptorSets;
    protected List<UBO> buffers;
    protected List<ImageDescriptor> imageDescriptors;
    protected PushConstants pushConstants;


    public Pipeline(String name) {

        this.name = name;
    }

    protected static long createPipelineLayout() {
        try(MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            final long x = Renderer.getDescriptorSetArray().getDescriptorSetLayout(0);


            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(x));

            //TODO; PushConstants temp disabed to work aroudn compatiblity isues with DescriptorSet layouts

          {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
                pushConstantRange.size(12);
                pushConstantRange.offset(0);
                pushConstantRange.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

                pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
            }

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(DEVICE, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            return pPipelineLayout.get(0);
        }
    }

    protected void createDescriptorSets(int frames) {
        descriptorSets = new DescriptorSets[frames];
        for(int i = 0; i < frames; ++i) {
            descriptorSets[i] = new DescriptorSets(i);
        }
    }

    public abstract void cleanUp();

    public PushConstants getPushConstants() { return this.pushConstants; }

    public static long getLayout() { return pipelineLayout; }

    public int bindDescriptorSets(int frame, boolean shouldUpdate) {
        UniformBuffers uniformBuffers = Renderer.getDrawer().getUniformBuffers();
        return this.descriptorSets[frame].updateSets(uniformBuffers, shouldUpdate);
    }

    public void bindDescriptorSets(UniformBuffers uniformBuffers, int frame, boolean shouldUpdate) {
        this.descriptorSets[frame].updateSets(uniformBuffers, shouldUpdate);
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
        private final int frame;


        DescriptorSets(int frame) {
            this.frame = frame;
        }



        protected int updateSets(UniformBuffers uniformBuffers, boolean shouldUpdate) {

            if(shouldUpdate) this.pushUniforms(uniformBuffers);

            return this.updateImageState();


        }

        private int updateImageState() {

            int currentTexture = 0;

            for(ImageDescriptor state : imageDescriptors)
            {
                //TODO; Disseminate between Fragment and VERTEX Stage binding/reserve slots
//                 VTextureSelector.getBoundId(state.imageIdx);

//                 if(state.getStages()!=VK_SHADER_STAGE_VERTEX_BIT) VTextureSelector.assertImageState(state.imageIdx);

                final int shaderTexture = VTextureSelector.getBoundId(state.imageIdx);

              if(state.getStages()!=VK_PIPELINE_STAGE_VERTEX_SHADER_BIT && shaderTexture!=0)
              {
//                  VulkanImage vulkanImage = VTextureSelector.getBoundTexture(state.imageIdx);
                  final DescriptorSetArray descriptorSetArray = Renderer.getDescriptorSetArray();
                  descriptorSetArray.registerTexture(state.imageIdx, shaderTexture, null);
                  currentTexture = descriptorSetArray.getInitialisedFragSamplers().TextureID2SamplerIdx(shaderTexture);
              }


            }
            return currentTexture;

        }


        private void pushUniforms(UniformBuffers uniformBuffers) {
            int currentOffset = uniformBuffers.getUsedBytes();

            //TODO: Use Hashtable for uniforms to reuse old values and reduce Uniform memory Usage: (Assuming Mojang Popsback Matrix stack to prior state and reused old Matrices)
            // + PreComputed/dynamic uniform offset table: Automatically optimise/+remove redundant hashes+contents, and use Offsets w/ matching hashed instead of using linear bump allocation
            // i..e interleaved Uniforms may also be possible via exploiting aliasing + recyling the overlapping offsets of prior uniforms
            //
            // TODO: e.g. fine the hash of the TOPMOSt uniform, them if the hash matches, overwrite the prior data w. the new non-aligned uniforms


           if(UniformState.MVP.requiresUpdate()&&!UniformState.MVP.hasUniqueHash()) {
               UniformState.MVP.storeCurrentOffset(currentOffset);
            for(UBO ubo : buffers) {

                //TODO non mappable memory

                int alignedSize = VUtil.align(ubo.getSize(), 64);//Only the Uniform descriptor needs to be aligned, not the contents
                uniformBuffers.checkCapacity(alignedSize);
//                int x = (ubo.getStages()&VK_SHADER_STAGE_FRAGMENT_BIT)!=0 ? 1024 : 0;
                ubo.update(uniformBuffers.getPointer(frame));

                uniformBuffers.updateOffset(alignedSize);

            }

            UniformState.MVP.resetAndUpdate();
            Renderer.getDrawer().updateUniformOffset();
            }

        }


    }

    public static class Builder {

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

            int imageIdx = VTextureSelector.getTextureIdx(name);
            final ImageDescriptor sampler2D = new ImageDescriptor("sampler2D", name, imageIdx);
            this.imageDescriptors.add(sampler2D);
//            VTextureSelector.registerUniqueBinding(sampler2D, shaderPath);
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
