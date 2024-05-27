package net.vulkanmod.vulkan.shader.descriptor;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.system.Checks.remainingSafe;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetArray {
    private static final VkDevice DEVICE = Vulkan.getVkDevice();
    private static final int UNIFORM_POOLS = 1;
    private static final int VERT_SAMPLER_MAX_LIMIT = 4;
    private static final int SAMPLER_MAX_LIMIT_DEFAULT = 32;
    private static final int MAX_POOL_SAMPLERS = 4096; //Might change to UINT16T_MAX
    static final int VERT_UBO_ID = 0, FRAG_UBO_ID = 1, VERTEX_SAMPLER_ID = 2, FRAG_SAMPLER_ID = 3;
    private static final int bindingsSize = 4;
//    private Int2ObjectLinkedOpenHashMap<Descriptor> DescriptorTableHeap;
//    private final Int2LongArrayMap perBindingSlowLayouts = new Int2LongArrayMap(bindingsSize);
    private final DescriptorAbstractionArray initialisedFragSamplers = new DescriptorAbstractionArray(16, SAMPLER_MAX_LIMIT_DEFAULT, VK_SHADER_STAGE_FRAGMENT_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, FRAG_SAMPLER_ID);
    private final DescriptorAbstractionArray initialisedVertSamplers = new DescriptorAbstractionArray(2, VERT_SAMPLER_MAX_LIMIT, VK_SHADER_STAGE_VERTEX_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VERTEX_SAMPLER_ID);
//    private final Int2ObjectLinkedOpenHashMap<ImageDescriptor> initialisedVertSamplers = new Int2ObjectLinkedOpenHashMap<>(SAMPLER_MAX_LIMIT);

    private final long descriptorSetLayout;
    private final long globalDescriptorPoolArrayPool;
    private static final int MAX_SETS = 2;
    private final long[] descriptorSets = new long[MAX_SETS];

    private static final int MISSING_TEX_ID = 24;
    private final boolean[] isUpdated = {false, false};
    private static final InlineUniformBlock uniformStates = new InlineUniformBlock(FRAG_UBO_ID, UniformState.FogStart, UniformState.FogEnd, UniformState.GameTime, UniformState.LineWidth, UniformState.FogColor);
    private static final int INLINE_UNIFORM_SIZE = uniformStates.size_t();
    private int MissingTexID = -1;

    private final IntOpenHashSet newTex = new IntOpenHashSet(32);
    private int currentSamplerSize = SAMPLER_MAX_LIMIT_DEFAULT;
    private int texturePool = 0;

    public void registerTexture(int binding, int TextureID)
    {

//        if(!GlTexture.hasImageResource(TextureID))
//        {
//            Initializer.LOGGER.error("SKipping Image: "+TextureID);
//            return;
//        }


        boolean needsUpdate = switch (binding) {
            case 0 -> this.initialisedFragSamplers.registerTexture(TextureID);
            default -> initialisedVertSamplers.registerTexture(TextureID);
        };

        if(needsUpdate)
        {
            this.newTex.add(TextureID);
            forceDescriptorUpdate();
        }
    }

    public int getTexture(int binding, int TextureID)
    {
        return (binding == 0 ? initialisedFragSamplers : initialisedVertSamplers).TextureID2SamplerIdx(TextureID);
    }

    public DescriptorSetArray() {


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
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            bindingFlags.put(VERTEX_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT);


            bindings.get(FRAG_SAMPLER_ID)
                    .binding(FRAG_SAMPLER_ID)
                    .descriptorCount(MAX_POOL_SAMPLERS / MAX_SETS) //Try to avoid Out of Pool errors on AMD
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK12.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT);


            VkDescriptorSetLayoutBindingFlagsCreateInfo setLayoutBindingsFlags = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack)
                    .sType$Default()
                    .bindingCount(MAX_SETS)
                    .pBindingFlags(bindingFlags);


            VkDescriptorSetLayoutCreateInfo vkDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(setLayoutBindingsFlags)
                    .pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            Vulkan.checkResult(vkCreateDescriptorSetLayout(DEVICE, vkDescriptorSetLayoutCreateInfo, null, pDescriptorSetLayout), "Failed to create descriptor set layout");

            this.descriptorSetLayout=pDescriptorSetLayout.get(0);


            this.globalDescriptorPoolArrayPool = createGlobalDescriptorPool();

            this.descriptorSets[0]= allocateDescriptorSet(stack, SAMPLER_MAX_LIMIT_DEFAULT);
            this.descriptorSets[1]= allocateDescriptorSet(stack, SAMPLER_MAX_LIMIT_DEFAULT);

        }
    }



    private long allocateDescriptorSet(MemoryStack stack, int samplerMaxLimitDefault) {


        VkDescriptorSetVariableDescriptorCountAllocateInfo variableDescriptorCountAllocateInfo = VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc(stack)
                .sType$Default()
                .pDescriptorCounts(stack.ints(samplerMaxLimitDefault));


        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType$Default();
        allocInfo.pNext(variableDescriptorCountAllocateInfo);
        allocInfo.descriptorPool(this.globalDescriptorPoolArrayPool);
        allocInfo.pSetLayouts(stack.longs(this.descriptorSetLayout));

        texturePool+=samplerMaxLimitDefault;

        LongBuffer dLongBuffer = stack.mallocLong(1);

        Vulkan.checkResult(vkAllocateDescriptorSets(DEVICE, allocInfo, dLongBuffer), "Failed to allocate descriptor sets");
        return dLongBuffer.get(0);
    }

    public long createGlobalDescriptorPool()
    {
        try(MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(3, stack);


                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.descriptorCount(MAX_SETS);

                VkDescriptorPoolSize uniformBufferPoolSize2 = poolSizes.get(1);
                uniformBufferPoolSize2.type(VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK);
                uniformBufferPoolSize2.descriptorCount(INLINE_UNIFORM_SIZE*MAX_SETS); //Byte Count/Size For Inline Uniform block

                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(2);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(MAX_POOL_SAMPLERS);

            VkDescriptorPoolInlineUniformBlockCreateInfo inlineUniformBlockCreateInfo = VkDescriptorPoolInlineUniformBlockCreateInfo.calloc(stack)
                    .sType$Default()
                    .maxInlineUniformBlockBindings(MAX_SETS);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pNext(inlineUniformBlockCreateInfo);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(MAX_SETS);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(DEVICE, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            return pDescriptorPool.get(0);


        }

    }

    //TODO: Confirm if pipeline Layers are still compatibr w. DescritprSets if Vertex Format changes

    public long getDescriptorSetLayout(int bindingID) {
        return descriptorSetLayout;
    }

    public DescriptorAbstractionArray getInitialisedFragSamplers() {
        return initialisedFragSamplers;
    }

//TODO; ASync smaper streaming: use a falback missing texture palcehodler if  mod adds a texture, but it not been updated/regsitered w/the Descriptor array yet

    public void updateAndBind(int frame, long uniformId, VkCommandBuffer commandBuffer)
    {
        if(this.MissingTexID == -1) {
            setupHardcodedTextures();
        }
        try(MemoryStack stack = stackPush()) {
            checkInlineUniformState(frame, stack);
            final boolean b = !this.isUpdated[frame];
            if(b){
                if(initialisedFragSamplers.checkCapacity())
                {
                    resizeSamplerArray(this.currentSamplerSize = initialisedFragSamplers.resize());
                }
                final long currentSet = descriptorSets[frame];
                final int NUM_UBOs = 1;
                final int NUM_INLINE_UBOs = uniformStates.uniformState().length;
                final int capacity = this.initialisedVertSamplers.currentSize() + this.initialisedFragSamplers.currentSize() + NUM_UBOs + NUM_INLINE_UBOs;
                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(capacity, stack);


                //TODO + Partial Selective Sampler Updates:
                // only updating newly added/changed Image Samplers
                // instead of thw whole Array each time

                updateUBOs(uniformId, stack, 0, descriptorWrites, currentSet);
                updateInlineUniformBlocks(stack, descriptorWrites, currentSet, uniformStates);
//
                updateImageSamplers(stack, descriptorWrites, currentSet, this.initialisedVertSamplers);
                updateImageSamplers(stack, descriptorWrites, currentSet, this.initialisedFragSamplers);

                descriptorWrites.rewind();
                vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
                this.isUpdated[frame] = true;

                this.newTex.clear();
            }

            //            final LongBuffer descriptorSets = Renderer.getDescriptorSetArray().getDescriptorSets();
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Pipeline.getLayout(),
                    0, stack.longs(descriptorSets[frame]), null);

            //Reset Frag PushConstant range to default state
            //Set shaderColor to White first (Fixes Mojang splash visibility)
            vkCmdPushConstants(commandBuffer, Pipeline.pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT, 32, stack.floats(1,1,1,1));

        }
    }

    private void checkInlineUniformState(int frame, MemoryStack stack) {
        if (UniformState.FogColor.requiresUpdate()) {

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(uniformStates.uniformState().length, stack);


            updateInlineUniformBlocks(stack, descriptorWrites, this.descriptorSets[frame], uniformStates);
//
            descriptorWrites.rewind();
            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);

            UniformState.FogColor.setUpdateState(this.isUpdated[0] | this.isUpdated[1]);
        }
    }

    private void setupHardcodedTextures() {

            //TODO: make this handle Vanilla's Async texture Loader
            // so images can be loaded and register asynchronously without risk of Undefined behaviour or Uninitialised descriptors
            // + Reserving texture Slots must use resourceLocation as textureIDs are not Determinate due to the Async Texture Loading
            this.MissingTexID = MissingTextureAtlasSprite.getTexture().getId();


//            this.initialisedFragSamplers.registerTexture(this.MissingTexID);
            final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            this.initialisedFragSamplers.registerImmutableTexture(this.MissingTexID, 0);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(Sheets.BANNER_SHEET).getId(), 1);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(TextureAtlas.LOCATION_PARTICLES).getId(), 2);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(InventoryMenu.BLOCK_ATLAS).getId(), 3);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(BeaconRenderer.BEAM_LOCATION).getId(), 4);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(TheEndPortalRenderer.END_SKY_LOCATION).getId(), 5);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(TheEndPortalRenderer.END_PORTAL_LOCATION).getId(), 6);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(ItemRenderer.ENCHANTED_GLINT_ITEM).getId(), 7);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(new ResourceLocation("textures/environment/clouds.png")/*LevelRenderer.CLOUDS_LOCATION*/).getId(), 8);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(new ResourceLocation("textures/misc/shadow.png")/*EntityRenderDispatcher.SHADOW_RENDER_TYPE*/).getId(), 9);
            this.initialisedVertSamplers.registerImmutableTexture(6, 0);
            this.initialisedVertSamplers.registerImmutableTexture(VTextureSelector.getBoundId(1), 1);
    }

    private static void updateUBOs(long uniformId, MemoryStack stack, int x, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet) {


        VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.calloc(1, stack);
        bufferInfos.buffer(uniformId);
        bufferInfos.offset(x);
        bufferInfos.range(Drawer.INITIAL_UB_SIZE);  //Udescriptors seem to be untyped: reserve range, but can fit anything + within the range


        //TODO: used indexed UBOs to workaound biding for new ofstes + adding new pipeline Layouts: (as long as max bound UBO Limits is sufficient)
        VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get();
        uboDescriptorWrite.sType$Default();
        uboDescriptorWrite.dstBinding(VERT_UBO_ID);
        uboDescriptorWrite.dstArrayElement(0);
        uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
        uboDescriptorWrite.descriptorCount(1);
        uboDescriptorWrite.pBufferInfo(bufferInfos);
        uboDescriptorWrite.dstSet(currentSet);

    }

    private void updateImageSamplers(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet) {
        updateImageSamplers(stack, descriptorWrites, currentSet, this.initialisedVertSamplers);
        updateImageSamplers(stack, descriptorWrites, currentSet, this.initialisedFragSamplers);
    }

    private static void updateInlineUniformBlocks(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet, InlineUniformBlock uniformStates) {

        int offset = 0;
        final Camera playerPos = Minecraft.getInstance().gameRenderer.getMainCamera();
        final int effectiveRenderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        FogRenderer.setupFog(playerPos,
                FogRenderer.FogMode.FOG_TERRAIN,
                effectiveRenderDistance,
                false,
                1);
        //TODO: can't specify static offsets in shader without Spec constants/Stringify Macro Hacks
        for(UniformState inlineUniform : uniformStates.uniformState()) {

            final long ptr = switch (inlineUniform)
            {
                default -> inlineUniform.getMappedBufferPtr().ptr;
                case FogStart -> stack.nfloat(RenderSystem.getShaderFogStart());
                case FogEnd -> stack.nfloat(RenderSystem.getShaderFogEnd());
                case GameTime -> stack.nfloat(RenderSystem.getShaderGameTime());
                case LineWidth -> stack.nfloat(RenderSystem.getShaderLineWidth());
//                case FogColor -> VRenderSystem.getShaderFogColor().ptr;
            };

            VkWriteDescriptorSetInlineUniformBlock inlineUniformBlock = VkWriteDescriptorSetInlineUniformBlock.calloc(stack)
                    .sType$Default();
            memPutAddress(inlineUniformBlock.address() + VkWriteDescriptorSetInlineUniformBlock.PDATA, ptr);
            VkWriteDescriptorSetInlineUniformBlock.ndataSize(inlineUniformBlock.address(), inlineUniform.getByteSize());

            //TODO: used indexed UBOs to workaound biding for new ofstes + adding new pipeline Layouts: (as long as max bound UBO Limits is sufficient)
            VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get();
            uboDescriptorWrite.sType$Default();
            uboDescriptorWrite.pNext(inlineUniformBlock);
            uboDescriptorWrite.dstBinding(FRAG_UBO_ID);
            uboDescriptorWrite.dstArrayElement(offset);
            uboDescriptorWrite.descriptorType(VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK);
            uboDescriptorWrite.descriptorCount(inlineUniform.getByteSize());
            uboDescriptorWrite.dstSet(currentSet);
            offset += inlineUniform.getByteSize();
        }
        }


    private void updateImageSamplers(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet, DescriptorAbstractionArray descriptorArray) {
        //TODO: Need DstArrayIdx, ImageView, or DstArrayIdx, TextureID to enumerate/initialise the DescriptorArray
        if(descriptorArray.currentSize()==0) return;

        for (Int2IntMap.Entry texId : descriptorArray.getAlignedIDs().int2IntEntrySet()) {


            final int texId1 = texId.getIntKey();
            final int samplerIndex = texId.getIntValue();

            VulkanImage image = GlTexture.getTexture(!GlTexture.hasImage(texId1) ? MissingTexID : texId1).getVulkanImage();

            image.readOnlyLayout();


            //Can assign ANY image to a Sampler: might decouple smapler form image creation + allocifNeeded selectively If Sampler needed
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(image.getImageView())
                    .sampler(image.getSampler());

            final VkWriteDescriptorSet vkWriteDescriptorSet = descriptorWrites.get();
            vkWriteDescriptorSet.sType$Default()
                    .dstBinding(descriptorArray.getBinding())
                    .dstArrayElement(samplerIndex)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo)
                    .dstSet(currentSet);




        }
    }

    //TODO:
    // Designed to mimic PushConstants by pushing a new stack, but for Descriptor sets instead
    // A new DescriptorSet is allocated, allowing capacity to be expanded like PushConstants
    // Intended to handle Postprocess renderPasses + if UBO slots exceed capacity
    public void pushDescriptorSet(int frame, VkCommandBuffer commandBuffer)
    {

        try(MemoryStack stack = MemoryStack.stackPush())
        {

            long allocateDescriptorSet = allocateDescriptorSet(stack, currentSamplerSize);

            final long descriptorSet = this.descriptorSets[frame];

            VkCopyDescriptorSet.Buffer vkCopyDescriptorSet = VkCopyDescriptorSet.calloc(4, stack);

            VkCopyDescriptorSet uboCopy = vkCopyDescriptorSet.get(VERT_UBO_ID);
            uboCopy.sType$Default();
            uboCopy.srcSet(descriptorSet);
            uboCopy.srcBinding(VERT_UBO_ID);
            uboCopy.dstBinding(VERT_UBO_ID);
            uboCopy.dstSet(allocateDescriptorSet);
            uboCopy.descriptorCount(1);

            VkCopyDescriptorSet uboCopy2 = vkCopyDescriptorSet.get(FRAG_UBO_ID);
            uboCopy2.sType$Default();
            uboCopy2.srcSet(descriptorSet);
            uboCopy2.srcBinding(FRAG_UBO_ID);
            uboCopy2.dstBinding(FRAG_UBO_ID);
            uboCopy2.dstSet(allocateDescriptorSet);
            uboCopy2.descriptorCount(INLINE_UNIFORM_SIZE);

            VkCopyDescriptorSet vertSmplrCopy = vkCopyDescriptorSet.get(VERTEX_SAMPLER_ID);
            vertSmplrCopy.sType$Default();
            vertSmplrCopy.srcSet(descriptorSet);
            vertSmplrCopy.srcBinding(VERTEX_SAMPLER_ID);
            vertSmplrCopy.dstBinding(VERTEX_SAMPLER_ID);
            vertSmplrCopy.dstSet(allocateDescriptorSet);
            vertSmplrCopy.descriptorCount(VERT_SAMPLER_MAX_LIMIT);

            VkCopyDescriptorSet fragSmplrCopy = vkCopyDescriptorSet.get(FRAG_SAMPLER_ID);
            fragSmplrCopy.sType$Default();
            fragSmplrCopy.srcSet(descriptorSet);
            fragSmplrCopy.srcBinding(FRAG_SAMPLER_ID);
            fragSmplrCopy.dstBinding(FRAG_SAMPLER_ID); //Make this store Framebuffers instead when using PostProcess passes
            fragSmplrCopy.dstSet(allocateDescriptorSet);
            fragSmplrCopy.descriptorCount(currentSamplerSize);


            vkUpdateDescriptorSets(DEVICE, null, vkCopyDescriptorSet);

            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Pipeline.pipelineLayout, 0, stack.longs(allocateDescriptorSet), null);


        }

    }





    private VulkanImage getSamplerImage(int texId1, int samplerIndex) {

        if (!GlTexture.hasImage(texId1))
        {
            Initializer.LOGGER.error("UnInitialised Image!: "+texId1 +"+"+samplerIndex+" Skipping...");

            return GlTexture.getTexture(MissingTexID).getVulkanImage();
        }
        return GlTexture.getTexture(texId1).getVulkanImage();
    }
    public void cleanup()
    {
        vkResetDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, 0);
        vkDestroyDescriptorSetLayout(DEVICE, this.descriptorSetLayout, null);
        vkDestroyDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, null);


    }


    //TODO: Maybe add VRAM Tetxure Usage

    public void removeImage(int id) {
        this.initialisedFragSamplers.removeTexture(id);

    }

    public void forceDescriptorUpdate() {
        Arrays.fill(this.isUpdated, false);
    }

    //TODO: Descriptor pool resizing if Texture count > or exceeds MAX_POOL_SAMPLERS
    private void resizeSamplerArray(int samplerLimit)
    {
        Vulkan.waitIdle();
        vkResetDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, 0);
        this.texturePool=0;

        try(MemoryStack stack = MemoryStack.stackPush()) {

            this.descriptorSets[0]= allocateDescriptorSet(stack, samplerLimit);
            this.descriptorSets[1]= allocateDescriptorSet(stack, samplerLimit);

            Initializer.LOGGER.info("Resized to "+ samplerLimit);

        }
    }

    public boolean isTexUnInitialised(int textureID)
    {
        return this.newTex.contains(textureID);
    }


    //todo: MSAA. Anisotropic Filtering:
    // Allow Reserving ranges in Descriptor Array, to store Unsitched Textures for AF/MSAA
    // e.g. Block atlas needs a 2048 range to be reserved when using AF/MSAA mode
    // + Sampler Indices need to be provided to the Vertex Buffer UVs when

    //TODO: Texture VRAM Usage
    public String[] getDebugInfo()
    {
        return new String[]{"-=TextureArrayStats=-",
                "Loaded     :  "+this.initialisedFragSamplers.currentSize(),
                "Frag       :  "+this.initialisedFragSamplers.currentLim(),
                "Allocated  :  "+this.currentSamplerSize ,
                "SetLimit   :  "+MAX_POOL_SAMPLERS/MAX_SETS,
                "TexturePool:  "+this.texturePool+"/"+MAX_POOL_SAMPLERS};
    }
}
