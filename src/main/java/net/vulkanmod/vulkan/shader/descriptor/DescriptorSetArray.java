package net.vulkanmod.vulkan.shader.descriptor;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.inventory.InventoryMenu;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.system.Checks.remainingSafe;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetArray {
    private static final VkDevice DEVICE = Vulkan.getVkDevice();
    private static final int UNIFORM_POOLS = 1;
    private static final int VERT_SAMPLER_MAX_LIMIT = 8;
    private static final int SAMPLER_MAX_LIMIT_DEFAULT = 32;
    private static final int MAX_POOL_SAMPLERS = 4096;
    static final int VERT_UBO_ID = 0, FRAG_UBO_ID = 1, VERTEX_SAMPLER_ID = 2, FRAG_SAMPLER_ID = 3;
    private static final int bindingsSize = 4;
//    private Int2ObjectLinkedOpenHashMap<Descriptor> DescriptorTableHeap;
//    private final Int2LongArrayMap perBindingSlowLayouts = new Int2LongArrayMap(bindingsSize);
    private final DescriptorAbstractionArray initialisedFragSamplers = new DescriptorAbstractionArray(0, SAMPLER_MAX_LIMIT_DEFAULT, VK_SHADER_STAGE_FRAGMENT_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, FRAG_SAMPLER_ID);
    private final DescriptorAbstractionArray initialisedVertSamplers = new DescriptorAbstractionArray(0, VERT_SAMPLER_MAX_LIMIT, VK_SHADER_STAGE_VERTEX_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VERTEX_SAMPLER_ID);
//    private final Int2ObjectLinkedOpenHashMap<ImageDescriptor> initialisedVertSamplers = new Int2ObjectLinkedOpenHashMap<>(SAMPLER_MAX_LIMIT);

    private final long descriptorSetLayout;
    private final long globalDescriptorPoolArrayPool;
    private final LongBuffer descriptorSets;
    private static final int value = 2;

    private static final int MISSING_TEX_ID = 24;

    private long defFragSampler;
    private final boolean[] isUpdated = {false, false};
    private static final int INLINE_UNIFORM_SIZE = 16 + 16+ 4;
    private int MissingTexID = -1;

    static
    {
/*
            [21:43:12] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> 4! -> minecraft:missing/0
            [21:43:31] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/shulker_boxes.png
            [21:43:37] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/blocks.png
            [21:43:38] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/armor_trims.png
            [21:43:39] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/signs.png
            [21:43:40] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/chest.png
            [21:43:41] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/banner_patterns.png
            [21:43:44] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/shield_patterns.png
            [21:43:44] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/beds.png
            [21:43:48] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/decorated_pot.png
            [21:45:16] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> 6! -> minecraft:dynamic/light_map_1
            [21:45:48] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/particles.png
            [21:45:55] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/paintings.png
            [21:45:56] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/mob_effects.png
            [21:45:56] [Render thread/INFO] (VulkanMod) Registered texture: 0 <-> -1 <-> -1! -> minecraft:textures/atlas/gui.png
*/

    }

    private final IntOpenHashSet newTex = new IntOpenHashSet(32);
    private int currentSamplerSize;

    public void addTexture(int binding, ImageDescriptor vulkanImage, long sampler)
    {
        //Smaplers are not exclusively boudn to a [arotucla rimages: can be boud  to any image which allwos for abstration +smaplfictaion of Descriptor Slots/Sets.indicies e.g. e.tc i.e..elemes
        final int stages = vulkanImage.getStages();
        switch (stages)
        {
//            case VK_SHADER_STAGE_FRAGMENT_BIT -> this.initialisedFragSamplers.addSampler(vulkanImage);
//            case VK_SHADER_STAGE_VERTEX_BIT -> initialisedVertSamplers.put(binding, vulkanImage);
        }
    }

    public void registerTexture(int binding, int TextureID, VulkanImage vulkanImage)
    {
        //TODO:maybe make textureID table global, then asign Ids+SampelrIndicies to DescriptorSetsBindinss
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
        return binding == 0 ? initialisedFragSamplers.TextureID2SamplerIdx(TextureID) : initialisedVertSamplers.TextureID2SamplerIdx(TextureID);
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

            bindingFlags.put(VERTEX_SAMPLER_ID, 0);


            bindings.get(FRAG_SAMPLER_ID)
                    .binding(FRAG_SAMPLER_ID)
                    .descriptorCount(MAX_POOL_SAMPLERS)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT);


            VkDescriptorSetLayoutBindingFlagsCreateInfo setLayoutBindingsFlags = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack)
                    .sType$Default()
                    .bindingCount(4)
                    .pBindingFlags(bindingFlags);


            VkDescriptorSetLayoutCreateInfo vkDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(setLayoutBindingsFlags)
                    .pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(DEVICE, vkDescriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }

            this.descriptorSetLayout=pDescriptorSetLayout.get(0);


            this.globalDescriptorPoolArrayPool = createGlobalDescriptorPool();

            this.descriptorSets = allocateDescriptorSets(stack);

        }
        final byte i = Options.getMipmaps();
        defFragSampler = SamplerManager.getTextureSampler(i, i>1?SamplerManager.USE_MIPMAPS_BIT:0);
    }



    private LongBuffer allocateDescriptorSets(MemoryStack stack) {

        final LongBuffer pSetLayouts = stack.callocLong(value);

        for(int i = 0 ; i < value; i++)
        {
            pSetLayouts.put(i, this.descriptorSetLayout);
        }

        VkDescriptorSetVariableDescriptorCountAllocateInfo variableDescriptorCountAllocateInfo = VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc(stack)
                .sType$Default()
                .pDescriptorCounts(stack.ints(SAMPLER_MAX_LIMIT_DEFAULT, SAMPLER_MAX_LIMIT_DEFAULT));


        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType$Default();
        allocInfo.pNext(variableDescriptorCountAllocateInfo);
        allocInfo.descriptorPool(this.globalDescriptorPoolArrayPool);
        allocInfo.pSetLayouts(pSetLayouts);

        LongBuffer dLongBuffer = MemoryUtil.memAllocLong(value);

        int result = vkAllocateDescriptorSets(DEVICE, allocInfo, dLongBuffer);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
        }
        return dLongBuffer;
    }

    public long createGlobalDescriptorPool()
    {
        try(MemoryStack stack = stackPush()) {
//            int size = DescriptorTableHeap.size();
            //TODO: Separate descriptorSet for each type: allows for the ability to selectively update+bind DescriptorSets
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(3, stack);


                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
//                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.descriptorCount(1);

                VkDescriptorPoolSize uniformBufferPoolSize2 = poolSizes.get(1);
//                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize2.type(VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK);
                uniformBufferPoolSize2.descriptorCount(INLINE_UNIFORM_SIZE); //Byte Count/Size For Inline Uniform block

                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(2);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(MAX_POOL_SAMPLERS);

            VkDescriptorPoolInlineUniformBlockCreateInfo inlineUniformBlockCreateInfo = VkDescriptorPoolInlineUniformBlockCreateInfo.calloc(stack)
                    .sType$Default()
                    .maxInlineUniformBlockBindings(4);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pNext(inlineUniformBlockCreateInfo);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(value); //One DSet for each binding

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(DEVICE, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            return pDescriptorPool.get(0);


        }

    }

    //TODO: Confirm if pipeline Layers are still compatibr w. DescritprSets if Vertex Format changes

    public long getDescriptorSet(int index) {
        return descriptorSets.get(index);
    }

    public long getDescriptorSetLayout(int bindingID) {
        return descriptorSetLayout;
    }

    public long getGlobalDescriptorPoolArrayPool() {
        return globalDescriptorPoolArrayPool;
    }

    public LongBuffer getDescriptorSets() {
        return descriptorSets;
    }

    public DescriptorAbstractionArray getInitialisedFragSamplers() {
        return initialisedFragSamplers;
    }

//TODO; ASync smaper streaming: use a falback missing texture palcehodler if  mod adds a texture, but it not been updated/regsitered w/the Descriptor array yet

    public void updateAndBind(int frame, long uniformId, VkCommandBuffer commandBuffer)
    {
        if(this.MissingTexID == -1)
        {
            this.MissingTexID = MissingTextureAtlasSprite.getTexture().getId();


//            this.initialisedFragSamplers.registerTexture(this.MissingTexID);
            final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            this.initialisedFragSamplers.registerTexture(this.MissingTexID);
            this.initialisedFragSamplers.registerTexture(textureManager.getTexture(Sheets.BANNER_SHEET).getId());
            this.initialisedFragSamplers.registerTexture(textureManager.getTexture(TextureAtlas.LOCATION_PARTICLES).getId());
            this.initialisedFragSamplers.registerTexture(textureManager.getTexture(InventoryMenu.BLOCK_ATLAS).getId());
            this.initialisedFragSamplers.registerTexture(textureManager.getTexture(BeaconRenderer.BEAM_LOCATION).getId());
            this.initialisedFragSamplers.registerTexture(textureManager.getTexture(TheEndPortalRenderer.END_SKY_LOCATION).getId());
            this.initialisedFragSamplers.registerTexture(textureManager.getTexture(TheEndPortalRenderer.END_PORTAL_LOCATION).getId());
            this.initialisedFragSamplers.registerTexture(textureManager.getTexture(ItemRenderer.ENCHANTED_GLINT_ITEM).getId());
            this.initialisedVertSamplers.registerTexture(6);
            this.initialisedVertSamplers.registerTexture(VTextureSelector.getBoundId(1));
        }
        try(MemoryStack stack = stackPush()) {
            final boolean b = !this.isUpdated[frame];
            if(b){
                if(initialisedFragSamplers.checkCapacity())
                {
                    resizeSamplerArray(frame, this.currentSamplerSize = initialisedFragSamplers.resize());
                }
                final long currentSet = descriptorSets.get(frame);
                final int NUM_UBOs = 1;
                final int NUM_INLINE_UBOs = 3;
                final int capacity = this.initialisedVertSamplers.currentSize() + this.initialisedFragSamplers.currentSize() + NUM_UBOs + NUM_INLINE_UBOs;
                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(capacity, stack);

                int x = 0;
                // 0 : reserved for constant Mat4s for Chunk Translations + PoV
                //1 : Light_DIR_ vectors
                //2: Inv Dir Vectors
                //3: Global mob Rot + MAt4


                // 8+ Scratch Spaces + Bump Linear Allocator w/ 256 byte chunks.sections .blocks (i..e PushDescriptors)

                //Fog Parameters = ColorModulator -> PushConstants

                //Move relagularly chnaging + hard to LLOCTe unifiorms to OPushConstant to help keep Unforms alloc mroe considtent + reduce Fragmenttaion + Variabels offsets e.g.


                updateUBOs(uniformId, stack, x, descriptorWrites, currentSet);
                updateInlineUniformBlocks(stack, descriptorWrites, currentSet);


                //                final int[] texArray = {6, MissingTexID, BlocksID, BannerID, MissingTexID};
//
                updateImageSamplers(stack, descriptorWrites, currentSet);

                descriptorWrites.rewind();
                vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
                this.isUpdated[frame] = true;
                
                this.newTex.clear();
            }

            //            final LongBuffer descriptorSets = Renderer.getDescriptorSetArray().getDescriptorSets();
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Pipeline.getLayout(),
                    0, stack.longs(descriptorSets.get(frame)), null);

//            vkCmdPushConstants(commandBuffer, Pipeline.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, UniformState.Light0_Direction.buffer());
//            vkCmdPushConstants(commandBuffer, Pipeline.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 16, UniformState.Light1_Direction.buffer());

            //Reset Frag PushConstant range to default state
            //Set shaderColor to White first (Fixes Mojang splash visibility)
            vkCmdPushConstants(commandBuffer, Pipeline.pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT, 32, stack.floats(1,1,1,1));

        }
    }

    private static void updateUBOs(long uniformId, MemoryStack stack, int x, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet) {


        VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.calloc(1, stack);
        bufferInfos.buffer(uniformId);
        bufferInfos.offset(x);
        bufferInfos.range(1024);  //Udescriptors seem to be untyped: reserve range, but can fit anything + within the range


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
        isInvalidImages(stack, descriptorWrites, currentSet, this.initialisedVertSamplers);
        isInvalidImages(stack, descriptorWrites, currentSet, this.initialisedFragSamplers);
    }

    private static void updateInlineUniformBlocks(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet) {

        int offset = 0;

        final UniformState[] uniformStates = {UniformState.ColorModulator, UniformState.SkyColor, UniformState.GameTime};
        for(UniformState uniformState : uniformStates) {
            final long ptr = switch (uniformState)
            {
                default -> uniformState.getMappedBufferPtr().ptr;
                case GameTime -> stack.nfloat(RenderSystem.getShaderGameTime());
                case FogStart -> stack.nfloat(RenderSystem.getShaderFogStart());
                case FogEnd -> stack.nfloat(RenderSystem.getShaderFogEnd());
            };

            VkWriteDescriptorSetInlineUniformBlock bufferInfos = VkWriteDescriptorSetInlineUniformBlock.calloc(stack)
                    .sType$Default();
            memPutAddress(bufferInfos.address() + VkWriteDescriptorSetInlineUniformBlock.PDATA, ptr);
            VkWriteDescriptorSetInlineUniformBlock.ndataSize(bufferInfos.address(), uniformState.size * 4);

            //TODO: used indexed UBOs to workaound biding for new ofstes + adding new pipeline Layouts: (as long as max bound UBO Limits is sufficient)
            VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get();
            uboDescriptorWrite.sType$Default();
            uboDescriptorWrite.pNext(bufferInfos);
            uboDescriptorWrite.dstBinding(FRAG_UBO_ID);
            uboDescriptorWrite.dstArrayElement(offset);
            uboDescriptorWrite.descriptorType(VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK);
            uboDescriptorWrite.descriptorCount(uniformState.size*4);
            uboDescriptorWrite.dstSet(currentSet);
            offset += uniformState.size*4;
        }
        }


    private void isInvalidImages(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet, DescriptorAbstractionArray descriptorArray) {
        //TODO: Need DstArrayIdx, ImageView, or DstArrayIdx, TextureID to enumerate/initialise the DescriptorArray
        if(descriptorArray.currentSize()==0) return;

        for (Int2IntMap.Entry texId : descriptorArray.getAlignedIDs().int2IntEntrySet()) {


            final int texId1 = texId.getIntKey();
            final int samplerIndex = texId.getIntValue();

            final VulkanImage image = getSamplerImage(texId1);
            if(image==null)
            {
                Initializer.LOGGER.error(texId1);
                continue;
            }
            image.readOnlyLayout();


            //Can assign ANY image to a Sampler: might decouple smapler form image creation + allocifNeeded selectively If Sampler needed
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(image.getImageView())
                    .sampler(defFragSampler);

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





    private VulkanImage getSamplerImage(int texId1) {
        final GlTexture vulkanImage = GlTexture.getTexture(texId1);
        VulkanImage image = vulkanImage != null ? vulkanImage.getVulkanImage() : null; //TODO: Not aligned to SmaplerBindindSlot: unintuitive usage atm
        if (image == null) {
            image = GlTexture.getTexture(MissingTexID).getVulkanImage();
        }
        return image;
    }

    public void setSampler(int miplevels)
    {
        this.defFragSampler = SamplerManager.getTextureSampler((byte) miplevels, miplevels>1 ? SamplerManager.USE_MIPMAPS_BIT : 0);
    }
    public void cleanup()
    {
        vkResetDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, 0);
        vkDestroyDescriptorSetLayout(DEVICE, this.descriptorSetLayout, null);
        vkDestroyDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, null);
        vkDestroySampler(DEVICE, this.defFragSampler, null);
        MemoryUtil.memFree(descriptorSets);
    }

    //TODO: Peristent Tetxure: i.e. skip GUi tetxiures beign stored peristently withint the DescriptorArray
    // Fix ImageView free during DescripotorSet usage
    // VRAM Tetxure Usage


    public void removeImage(int id) {
        if(this.initialisedFragSamplers.removeTexture(id))
            forceDescriptorUpdate();
    }

    public void forceDescriptorUpdate() {
        Arrays.fill(this.isUpdated, false);
    }


    private void resizeSamplerArray(int frame, int samplerLimit)
    {
        Vulkan.waitIdle();
        vkResetDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, 0);
        try(MemoryStack stack = MemoryStack.stackPush()) {

            VkDescriptorSetVariableDescriptorCountAllocateInfo variableDescriptorCountAllocateInfo = VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc(stack)
                    .sType$Default()
                    .pDescriptorCounts(stack.ints(samplerLimit,samplerLimit));


            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType$Default();
            allocInfo.pNext(variableDescriptorCountAllocateInfo);
            allocInfo.descriptorPool(this.globalDescriptorPoolArrayPool);
            allocInfo.pSetLayouts(stack.longs(this.descriptorSetLayout,this.descriptorSetLayout));

            Vulkan.checkResult(vkAllocateDescriptorSets(DEVICE, allocInfo, descriptorSets),"");

            Initializer.LOGGER.info("Resized to "+ samplerLimit);

        }
    }



    public boolean needsUpdate(int frame)
    {
        return !this.isUpdated[frame];
    }

    public boolean isTexUnInitialised(int textureID)
    {
        return this.newTex.contains(textureID);
    }

    //todo: Allow Reserving ranges in Descriptor Array, so a approx 2048 range can be reserved.allocated for AF/MSAA mode + to supply Indices to the VertexBuilder/BuildTask
    public String getDebugInfo()
    {
        return"textures[Loaded="+this.initialisedFragSamplers.currentSize()+"Frag="+this.initialisedFragSamplers.currentLim()+"PoolRange="+ this.currentSamplerSize +"]";
    }
}
