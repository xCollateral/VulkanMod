package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.texture.SamplerManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.stream.Stream;

import static org.lwjgl.system.Checks.remainingSafe;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorManager {
    private static final VkDevice DEVICE = Vulkan.getVkDevice();
    static final int VERT_SAMPLER_MAX_LIMIT = 4;

    static final int MAX_POOL_SAMPLERS = 16384; //MoltenVk Bug: https://github.com/KhronosGroup/MoltenVK/issues/2227
    static final int VERT_UBO_ID = 0, FRAG_UBO_ID = 1, VERTEX_SAMPLER_ID = 2, FRAG_SAMPLER_ID = 3;
    private static final int bindingsSize = 4;

    private static final long descriptorSetLayout;
    private static final long globalDescriptorPoolArrayPool;

    private static final int PER_SET_ALLOCS = 2; //Sets used per BindlessDescriptorSet
    private static final int MAX_SETS = 2;// * PER_SET_ALLOCS;
    private static final Int2ObjectArrayMap<BindlessDescriptorSet> sets = new Int2ObjectArrayMap<>(MAX_SETS);


    private static final InlineUniformBlock uniformStates = new InlineUniformBlock(FRAG_UBO_ID,  UniformState.FogColor, UniformState.FogStart, UniformState.FogEnd, UniformState.GlintAlpha, UniformState.GameTime, UniformState.LineWidth);
    static final int INLINE_UNIFORM_SIZE = uniformStates.size_t();

    static final int maxPerStageSamplers = DeviceManager.deviceProperties.limits().maxPerStageDescriptorSamplers();
    private static final boolean semiBindless = maxPerStageSamplers <MAX_POOL_SAMPLERS; //When the device has a textureLimit < MAX_POOL_SAMPLERS


    private static int texturePool = 0;
    private static final int TOTAL_SETS = 32;


    static {

        Initializer.LOGGER.info("Setting Rendering Mode: Bindless mode: {}", semiBindless ? "Semi-Bindless" : "Fully-Bindless");
        Initializer.LOGGER.info("Max Per Stage Samplers: {}", maxPerStageSamplers);
        Initializer.LOGGER.info("Sets: {}", semiBindless ? MAX_SETS : TOTAL_SETS);
        try (MemoryStack stack = stackPush()) {


            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindingsSize, stack);
            IntBuffer bindingFlags = stack.callocInt(bindingsSize);



            bindings.get(VERT_UBO_ID)
                    .binding(VERT_UBO_ID)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            bindingFlags.put(VERT_UBO_ID, 0);
            //Vertex samplers are always immutable: they are never updated or removed unlike Frag Samplers
            final long textureSampler = SamplerManager.getTextureSampler((byte) 0, (byte) 0);
            bindings.get(FRAG_UBO_ID)
                    .binding(FRAG_UBO_ID)
                    .descriptorCount(INLINE_UNIFORM_SIZE)
                    .descriptorType(VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_UBO_ID, 0);

            bindings.get(VERTEX_SAMPLER_ID)
                    .binding(VERTEX_SAMPLER_ID)
                    .descriptorCount(VERT_SAMPLER_MAX_LIMIT)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(stack.longs(textureSampler, textureSampler, textureSampler, textureSampler))
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            bindingFlags.put(VERTEX_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT);


            bindings.get(FRAG_SAMPLER_ID)
                    .binding(FRAG_SAMPLER_ID)
                    .descriptorCount( semiBindless ? maxPerStageSamplers : MAX_POOL_SAMPLERS / MAX_SETS)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK12.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT);


            VkDescriptorSetLayoutBindingFlagsCreateInfo setLayoutBindingsFlags = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack)
                    .sType$Default()
                    .bindingCount(bindingFlags.capacity())
                    .pBindingFlags(bindingFlags);


            VkDescriptorSetLayoutCreateInfo vkDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(setLayoutBindingsFlags)
                    .pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            Vulkan.checkResult(vkCreateDescriptorSetLayout(DEVICE, vkDescriptorSetLayoutCreateInfo, null, pDescriptorSetLayout), "Failed to create descriptor set layout");

            descriptorSetLayout=pDescriptorSetLayout.get(0);

            globalDescriptorPoolArrayPool = createGlobalDescriptorPool();


        }
    }



    static long allocateDescriptorSet(MemoryStack stack, int samplerMaxLimitDefault) {

        if(texturePool+samplerMaxLimitDefault>MAX_POOL_SAMPLERS) throw new RuntimeException();
        VkDescriptorSetVariableDescriptorCountAllocateInfo variableDescriptorCountAllocateInfo = VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc(stack)
                .sType$Default()
                .pDescriptorCounts(stack.ints(samplerMaxLimitDefault));


        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType$Default();
        allocInfo.pNext(variableDescriptorCountAllocateInfo);
        allocInfo.descriptorPool(globalDescriptorPoolArrayPool);
        allocInfo.pSetLayouts(stack.longs(descriptorSetLayout));

        texturePool+=samplerMaxLimitDefault;

        LongBuffer dLongBuffer = stack.mallocLong(1);

        Vulkan.checkResult(vkAllocateDescriptorSets(DEVICE, allocInfo, dLongBuffer), "Failed to allocate descriptor sets");
        return dLongBuffer.get(0);
    }

    public static long createGlobalDescriptorPool()
    {
        try(MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(3, stack);
                //4 Sets on Bindless, 64 on semi-Bindless
                final int maxSets = (semiBindless ? TOTAL_SETS : MAX_SETS) * PER_SET_ALLOCS;

                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.descriptorCount(maxSets);

                VkDescriptorPoolSize uniformBufferPoolSize2 = poolSizes.get(1);
                uniformBufferPoolSize2.type(VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK);
                uniformBufferPoolSize2.descriptorCount(INLINE_UNIFORM_SIZE); //Byte Count/Size For Inline Uniform block

                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(2);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(semiBindless ? maxPerStageSamplers * PER_SET_ALLOCS * maxSets : MAX_POOL_SAMPLERS);

            VkDescriptorPoolInlineUniformBlockCreateInfo inlineUniformBlockCreateInfo = VkDescriptorPoolInlineUniformBlockCreateInfo.calloc(stack)
                    .sType$Default()
                    .maxInlineUniformBlockBindings(maxSets);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pNext(inlineUniformBlockCreateInfo);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(maxSets);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(DEVICE, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            return pDescriptorPool.get(0);


        }

    }


    public static long getDescriptorSetLayout() {
        return descriptorSetLayout;
    }




    public static void cleanup()
    {
        vkResetDescriptorPool(DEVICE, globalDescriptorPoolArrayPool, 0);
        vkDestroyDescriptorSetLayout(DEVICE, descriptorSetLayout, null);
        vkDestroyDescriptorPool(DEVICE, globalDescriptorPoolArrayPool, null);
        sets.clear();


    }

    public static void registerTexture(int setID, int imageIdx, int shaderTexture) {
        sets.get(setID).registerTexture(imageIdx, shaderTexture);


    }

    public static void BindAllSets(int currentFrame, VkCommandBuffer commandBuffer) {


            try(MemoryStack stack = MemoryStack.stackPush())
            {
                LongBuffer a = stack.mallocLong(sets.size());
                sets.forEach((key, value) -> a.put(key, value.getSet(currentFrame)));
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Renderer.getLayout(),0, a, null);
            }

    }

    public static void removeImage(int setID, int textureID) {
        sets.get(setID).removeFragImage(textureID);
    }


    public static void updateAllSets() {
        sets.forEach((integer, bindlessDescriptorSet) -> bindlessDescriptorSet.forceDescriptorUpdate());
    }


    //TODO: Texture VRAM Usage
    public static String[] getDebugInfo()
    {


        int loaded = sets.get(0).getLoadedTextures();
        int reserved = sets.get(0).getReservedTextures();


        return new String[]{"-=== Texture Stats ===-", "Loaded: "+loaded, "Reserved: "+reserved, "Max: "+MAX_POOL_SAMPLERS/MAX_SETS};

    }


    public static void addDescriptorSet(int SetID, BindlessDescriptorSet bindlessDescriptorSet) {
        if(sets.size()> MAX_SETS*PER_SET_ALLOCS) throw new RuntimeException("Too Many DescriptorSets!: "+SetID +">"+MAX_SETS/PER_SET_ALLOCS+"-1");

        sets.put(bindlessDescriptorSet.getSetID(), bindlessDescriptorSet);
    }

    public static void updateAndBindAllSets(int frame, long uniformId, VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {


            boolean hasResized = false;
            for (Int2ObjectMap.Entry<BindlessDescriptorSet> set : sets.int2ObjectEntrySet()) {
                final BindlessDescriptorSet value = set.getValue();
                hasResized |= value.checkCapacity();
            }

            if(hasResized)
            {
                resizeAllSamplerArrays();
            }

            LongBuffer updatedSets = stack.mallocLong(sets.size());

            for (BindlessDescriptorSet bindlessDescriptorSet : sets.values()) {
                updatedSets.put(bindlessDescriptorSet.updateAndBind(frame, uniformId, uniformStates));
            }




            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Renderer.getLayout(), 0, updatedSets.rewind(), null);


            //Reset Frag PushConstant range to default state
            //Set shaderColor to White first (Fixes Mojang splash visibility)
            vkCmdPushConstants(commandBuffer, Renderer.getLayout(), VK_SHADER_STAGE_FRAGMENT_BIT, 32, stack.floats(1,1,1,1));
        }


    }


    //TODO: Descriptor pool resizing if Texture count > or exceeds MAX_POOL_SAMPLERS
    private static void resizeAllSamplerArrays()
    {
        Vulkan.waitIdle();
        //Reset pool to avoid Fragmentation: (not using VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT for performance reasons)
        // requires all Sets to be reallocated but avoids Descriptor Fragmentation
        vkResetDescriptorPool(DEVICE, DescriptorManager.globalDescriptorPoolArrayPool, 0);
        texturePool=0;

        for (Int2ObjectMap.Entry<BindlessDescriptorSet> set : sets.int2ObjectEntrySet()) {

            set.getValue().resizeSamplerArrays();
        }
    }


    public static int getTexture(int setID, int imageIdx, int shaderTexture) {
        return sets.get(setID).getTexture(imageIdx, shaderTexture);
    }

    public static boolean isTexUnInitialised(int setID, int shaderTexture) {
        return sets.get(setID).isTexUnInitialised(shaderTexture);
    }

    public static void checkSubSetState(int setID) {
        if(semiBindless) sets.get(setID).bindSubSetIfNeeded();
    }
}
