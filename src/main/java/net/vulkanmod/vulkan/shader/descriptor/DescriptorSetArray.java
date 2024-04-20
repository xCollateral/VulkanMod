package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.inventory.InventoryMenu;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.DeviceManager;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.system.Checks.remainingSafe;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAddressSafe;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetArray {
    private static final VkDevice DEVICE = Vulkan.getDevice();
    private static final int UNIFORM_POOLS = 1;
    private static final int SAMPLER_MAX_LIMIT = 512;
    static final int VERT_UBO_ID = 0, FRAG_UBO_ID = 1, VERTEX_SAMPLER_ID = 2, FRAG_SAMPLER_ID = 3;
    private static final int bindingsSize = 4;
//    private Int2ObjectLinkedOpenHashMap<Descriptor> DescriptorTableHeap;
//    private final Int2LongArrayMap perBindingSlowLayouts = new Int2LongArrayMap(bindingsSize);
    private final DescriptorAbstractionArray initialisedFragSamplers = new DescriptorAbstractionArray(0, 512, VK_SHADER_STAGE_FRAGMENT_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, FRAG_SAMPLER_ID);
    private final DescriptorAbstractionArray initialisedVertSamplers = new DescriptorAbstractionArray(0, 512, VK_SHADER_STAGE_VERTEX_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VERTEX_SAMPLER_ID);
//    private final Int2ObjectLinkedOpenHashMap<ImageDescriptor> initialisedVertSamplers = new Int2ObjectLinkedOpenHashMap<>(SAMPLER_MAX_LIMIT);

    private final long descriptorSetLayout;
    private final long globalDescriptorPoolArrayPool;
    private final LongBuffer descriptorSets;
    private static final int value = 2;

    private static final int MISSING_TEX_ID = 24;

    private final long defFragSampler;
    private final boolean[] isUpdated = {false, false};
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
            Arrays.fill(this.isUpdated, false);
        }
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
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_UBO_ID, 0);

            bindings.get(VERTEX_SAMPLER_ID)
                    .binding(VERTEX_SAMPLER_ID)
                    .descriptorCount(SAMPLER_MAX_LIMIT)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            bindingFlags.put(VERTEX_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT);


            bindings.get(FRAG_SAMPLER_ID)
                    .binding(FRAG_SAMPLER_ID)
                    .descriptorCount(SAMPLER_MAX_LIMIT)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT);


            VkDescriptorSetLayoutBindingFlagsCreateInfo setLayoutBindingsFlags = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack)
                    .sType$Default()
                    .bindingCount(4)
                    .pBindingFlags(bindingFlags);


            VkDescriptorSetLayoutCreateInfo vkDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType$Default()
//                    .pNext(setLayoutBindingsFlags)
                    .pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(DeviceManager.device, vkDescriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }

            this.descriptorSetLayout=pDescriptorSetLayout.get(0);


            this.globalDescriptorPoolArrayPool = createGlobalDescriptorPool();

            this.descriptorSets = allocateDescriptorSets(stack);

        }
        defFragSampler = SamplerManager.getTextureSampler((byte) 1, (byte) 0);
    }



    private LongBuffer allocateDescriptorSets(MemoryStack stack) {

        final LongBuffer pSetLayouts = stack.callocLong(value);

        for(int i = 0 ; i < value; i++)
        {
            pSetLayouts.put(i, this.descriptorSetLayout);
        }


        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType$Default();
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
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);


                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
//                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.descriptorCount(2);

                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            final int VERTEX_SAMPLER_MAX = 2;
            textureSamplerPoolSize.descriptorCount(VERTEX_SAMPLER_MAX + SAMPLER_MAX_LIMIT);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
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
            this.initialisedFragSamplers.registerTexture(Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).getId());
            this.initialisedFragSamplers.registerTexture(Minecraft.getInstance().getTextureManager().getTexture(Sheets.BANNER_SHEET).getId());
            this.initialisedFragSamplers.registerTexture(Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES).getId());
            this.initialisedVertSamplers.registerTexture(6);
        }
        try(MemoryStack stack = stackPush()) {
            final long currentSet = descriptorSets.get(frame);
            if(!this.isUpdated[frame]) {
                final int NUM_UBOs = 2;
                final int capacity = this.initialisedVertSamplers.currentSize() + this.initialisedFragSamplers.currentSize() + NUM_UBOs;
                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(capacity, stack);

                int x = 0;
                ;           // 0 : reserved for constant Mat4s for Chunk Translations + PoV
                //1 : Light_DIR_ vectors
                //2: Inv Dir Vectors
                //3: Global mob Rot + MAt4


                // 8+ Scratch Spaces + Bump Linear Allocator w/ 256 byte chunks.sections .blocks (i..e PushDescriptors)

                //Fog Parameters = ColorModulator -> PushConstants

                //Move relagularly chnaging + hard to LLOCTe unifiorms to OPushConstant to help keep Unforms alloc mroe considtent + reduce Fragmenttaion + Variabels offsets e.g.
                int currentBinding = 0;


                for (int currentWriteIndex = 0; currentWriteIndex < 2; currentWriteIndex++) {


                    VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.calloc(1, stack);
                    bufferInfos.buffer(uniformId);
                    bufferInfos.offset(x);
                    bufferInfos.range(1024);  //Udescriptors seem to be untyped: reserve range, but can fit anything + within the range
                    x += 1024;

                    //TODO: used indexed UBOs to workaound biding for new ofstes + adding new pipeline Layouts: (as long as max bound UBO Limits is sufficient)
                    VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get();
                    uboDescriptorWrite.sType$Default();
                    uboDescriptorWrite.dstBinding(currentBinding);
                    uboDescriptorWrite.dstArrayElement(0);
                    uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                    uboDescriptorWrite.descriptorCount(1);
                    uboDescriptorWrite.pBufferInfo(bufferInfos);
                    uboDescriptorWrite.dstSet(currentSet);
                    currentBinding++;
                }


                //                final int[] texArray = {6, MissingTexID, BlocksID, BannerID, MissingTexID};
//
                isInvalidImages(stack, descriptorWrites, currentSet, this.initialisedVertSamplers);
                isInvalidImages(stack, descriptorWrites, currentSet, this.initialisedFragSamplers);

                descriptorWrites.rewind();
                vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
                this.isUpdated[frame] = true;
            }

            //            final LongBuffer descriptorSets = Renderer.getDescriptorSetArray().getDescriptorSets();
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Pipeline.getLayout(),
                    0, stack.longs(currentSet), null);

        }
    }

    private void isInvalidImages(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet, DescriptorAbstractionArray descriptorArray) {
        //TODO: Need DstArrayIdx, ImageView, or DstArrayIdx, TextureID to enumerate/initialise the DescriptorArray
        if(descriptorArray.currentSize()==0) return;

        for (Int2IntMap.Entry texId : descriptorArray.getAlignedIDs().int2IntEntrySet()) {


            final int texId1 = texId.getIntKey();
            final int samplerIndex = texId.getIntValue();

            final VulkanImage image = getSamplerImage(texId1);

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


    public void cleanup()
    {
        vkResetDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, 0);
        vkDestroyDescriptorSetLayout(DEVICE, this.descriptorSetLayout, null);
        vkDestroyDescriptorPool(DEVICE, this.globalDescriptorPoolArrayPool, null);
        vkDestroySampler(DEVICE, this.defFragSampler, null);
        MemoryUtil.memFree(descriptorSets);
    }
}
