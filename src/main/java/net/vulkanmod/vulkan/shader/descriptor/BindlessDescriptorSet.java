package net.vulkanmod.vulkan.shader.descriptor;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
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
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.Arrays;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;

public class BindlessDescriptorSet {

    private static final VkDevice DEVICE = Vulkan.getVkDevice();

    static final int VERT_UBO_ID = 0, FRAG_UBO_ID = 1, VERTEX_SAMPLER_ID = 2, FRAG_SAMPLER_ID = 3;


    private final DescriptorAbstractionArray initialisedFragSamplers;
    private final DescriptorAbstractionArray initialisedVertSamplers;
    private final ObjectArrayList<SubSet> DescriptorStack = new ObjectArrayList<>(16);

    private final boolean[] needsUpdate = {true, true};

    private final IntOpenHashSet newTex = new IntOpenHashSet(32);
    private final int setID;
    private final int vertTextureLimit;
    private int MissingTexID = -1;

    //TODO: PushDescriptorStes size may get too big if maxPerStageDescriptorSamplers > 1024 but < 65536
    static final int maxPerStageDescriptorSamplers = /*32;*/Math.min(DescriptorManager.MAX_POOL_SAMPLERS, DeviceManager.deviceProperties.limits().maxPerStageDescriptorSamplers());
    static final boolean semiBindless = maxPerStageDescriptorSamplers < DescriptorManager.MAX_POOL_SAMPLERS;
    private int subSetIndex;
    private int boundSubSet;//,  subSetIndex;


    public BindlessDescriptorSet(int setID, int vertTextureLimit, int fragTextureLimit) {
        this.setID = setID;
        this.vertTextureLimit = vertTextureLimit;


        initialisedVertSamplers = new DescriptorAbstractionArray(vertTextureLimit, VK_SHADER_STAGE_VERTEX_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VERTEX_SAMPLER_ID);
        initialisedFragSamplers = new DescriptorAbstractionArray(fragTextureLimit, VK_SHADER_STAGE_FRAGMENT_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, FRAG_SAMPLER_ID);

        DescriptorStack.add(new SubSet(0, vertTextureLimit, fragTextureLimit));
    }


    public void registerTexture(int binding, int TextureID)
    {


        final boolean isNewTex = (binding == 0 ? this.initialisedFragSamplers
         : this.initialisedVertSamplers).registerTexture(TextureID);

        if(binding!=0) return; //Ignore the immutable Vertex Samplers


        final int samplerIndex = this.initialisedFragSamplers.TextureID2SamplerIdx(TextureID);
        int subSetIndex = getSubSetIndex(samplerIndex);
        if(isNewTex)
        {
            // Push New descriptorSet onto the DescriptorStack if textureCount > maxPerStageDescriptorSamplers
            // (Only applicable to semi-Bindless Devices)
            if(semiBindless && subSetIndex>=DescriptorStack.size())
            {
                this.DescriptorStack.add(subSetIndex, pushDescriptorSet());
            }

            this.DescriptorStack.get(subSetIndex).addTexture(TextureID, samplerIndex);
            newTex.add(TextureID);

            //TODO: selective updates instead of the full set
            this.forceDescriptorUpdate();
            return;

        }
        this.subSetIndex=subSetIndex;
        if(semiBindless) bindSubSetIfNeeded();

    }
    //Change SuBSet if the texture range exceeds the max of the currently bound SuBSet (i.e. maxPerStageDescriptorSamplers)
    void bindSubSetIfNeeded() {

        if(this.subSetIndex!=boundSubSet && this.subSetIndex<DescriptorStack.size()) {
            boundSubSet = this.subSetIndex;


            try (MemoryStack stack = MemoryStack.stackPush()) {
                vkCmdBindDescriptorSets(Renderer.getCommandBuffer(),
                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                        Renderer.getLayout(), //Don't need the Full layout for Blocks Set (set = 1): can just bind this set only
                        0,
                        stack.longs(this.DescriptorStack.get(this.subSetIndex).getSetHandle(Renderer.getCurrentFrame())),
                        null);
            }
        }
    }


    public int getTexture(int binding, int TextureID)
    {
//        if(binding!=0) return 0;
        final int i = (binding == 0 ? initialisedFragSamplers : initialisedVertSamplers).TextureID2SamplerIdx(TextureID);
//        return DescriptorStack.get(getSubSetIndex(i)).getAlignedIDs().get(TextureID);
        return i - getSubSet(i).getBaseIndex();
    }

    private SubSet getSubSet(int i) {
        return this.DescriptorStack.get(getSubSetIndex(i));
    }

    //TODO: may remove this, as using Non-hardcoded Frag Sampler Indices with texture broadcast seems to have the same performance
    private void setupHardcodedTextures() {

        this.MissingTexID = MissingTextureAtlasSprite.getTexture().getId();

        final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

        if(this.setID==0) {
//            this.initialisedFragSamplers.registerTexture(this.MissingTexID);
            this.initialisedFragSamplers.registerImmutableTexture(this.MissingTexID, 0);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(Sheets.BANNER_SHEET).getId(), 1);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(TextureAtlas.LOCATION_PARTICLES).getId(), 2);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(BeaconRenderer.BEAM_LOCATION).getId(), 4);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(TheEndPortalRenderer.END_SKY_LOCATION).getId(), 5);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(TheEndPortalRenderer.END_PORTAL_LOCATION).getId(), 6);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(ItemRenderer.ENCHANTED_GLINT_ITEM).getId(), 7);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(new ResourceLocation("textures/environment/clouds.png")/*LevelRenderer.CLOUDS_LOCATION*/).getId(), 8);
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(new ResourceLocation("textures/misc/shadow.png")/*EntityRenderDispatcher.SHADOW_RENDER_TYPE*/).getId(), 9);
            this.initialisedVertSamplers.registerImmutableTexture(6, 0);
            this.initialisedVertSamplers.registerImmutableTexture(8, 1);
        }
        else {
            this.initialisedFragSamplers.registerImmutableTexture(textureManager.getTexture(InventoryMenu.BLOCK_ATLAS).getId(), 0);
            this.initialisedVertSamplers.registerImmutableTexture(6, 0);
        }

        this.DescriptorStack.get(0).getAlignedIDs().putAll(this.initialisedFragSamplers.getAlignedIDs());


    }


    public long updateAndBind(int frame, long uniformId, InlineUniformBlock uniformStates)
    {
        boundSubSet=subSetIndex=0; //reset subsetindex balc to the default baseSubset

        if (this.MissingTexID == -1) {
            setupHardcodedTextures();
        }
        try(MemoryStack stack = stackPush()) {
            checkInlineUniformState(frame, stack, uniformStates);
            if(this.needsUpdate[frame]){

                //TODO: may use an array of DescriptorAbstractionArrays instead, w/ each having a internal pool of new textures/SmaplerIndices
                for(SubSet currentSet : this.DescriptorStack) {

                    final long setID = currentSet.getSetHandle(frame);

                    final int NUM_UBOs = 1;
                    final int NUM_INLINE_UBOs = uniformStates.uniformState().length;
                    final int fragSize = currentSet.getAlignedIDs().size();
                    final int capacity = fragSize + initialisedVertSamplers.currentSize() + NUM_UBOs + NUM_INLINE_UBOs;
                    VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(capacity, stack);
//                    final long currentSet = descriptorSets[frame];


                    //TODO + Partial Selective Sampler Updates:
                    // only updating newly added/changed Image Samplers
                    // instead of thw whole Array each time

                    updateUBOs(uniformId, stack, 0, descriptorWrites, setID);
                    updateInlineUniformBlocks(stack, descriptorWrites, setID, uniformStates);
                    //TODO: May remove VertxSampler updates, as they are mostly immutable textures

                    updateImageSamplers2(stack, descriptorWrites, setID, this.initialisedVertSamplers.getAlignedIDs(), this.initialisedVertSamplers.getBinding());
                    updateImageSamplers2(stack, descriptorWrites, setID, currentSet.getAlignedIDs(), this.initialisedFragSamplers.getBinding());




                    vkUpdateDescriptorSets(DEVICE, descriptorWrites.rewind(), null);
                }
                this.needsUpdate[frame] = false;

                this.newTex.clear();

            }
//            if(this.initialisedFragSamplers.currentSize()==0)
//            {
//                forceDescriptorUpdate();
//            }
            //Only get Base set for Binding: i.e. th default 0th DescriptorSet handle
            return this.DescriptorStack.get(0).getSetHandle(frame);

        }
    }

    private void checkInlineUniformState(int frame, MemoryStack stack, InlineUniformBlock uniformStates) {
        //Don't update Inlines twice if update is pending]
        if (!this.needsUpdate[frame] && (UniformState.FogColor.requiresUpdate()||UniformState.FogEnd.requiresUpdate())) {

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(uniformStates.uniformState().length, stack);

            //TODO; Propagating Inline Uniform Updates
            updateInlineUniformBlocks(stack, descriptorWrites, this.DescriptorStack.get(0).getSetHandle(frame), uniformStates);
//
            descriptorWrites.rewind();
            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);

            UniformState.FogColor.setUpdateState(false);
            UniformState.FogStart.setUpdateState(false);
            UniformState.FogEnd.setUpdateState(false);
            UniformState.GlintAlpha.setUpdateState(false);
            //TODO: Don't update sets twice if only Inlines require update
            DescriptorManager.updateAllSets();
        }
    }


    void updateUBOs(long uniformId, MemoryStack stack, int x, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet) {


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

    private static void updateInlineUniformBlocks(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long currentSet, InlineUniformBlock uniformStates) {

        int offset = 0;
        //TODO: can't specify static offsets in shader without Spec constants/Stringify Macro Hacks
        for(UniformState inlineUniform : uniformStates.uniformState()) {

            final long ptr = switch (inlineUniform)
            {
                default -> inlineUniform.getMappedBufferPtr().ptr;
                case GameTime -> stack.nfloat(RenderSystem.getShaderGameTime());
                case LineWidth -> stack.nfloat(RenderSystem.getShaderLineWidth());
                case GlintAlpha -> stack.nfloat(RenderSystem.getShaderGlintAlpha());
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


    private void updateImageSamplers2(MemoryStack stack, VkWriteDescriptorSet.Buffer descriptorWrites, long setHandle, Int2IntMap alignedIDs, int binding) {

        for (Int2IntMap.Entry texId : alignedIDs.int2IntEntrySet()) {
            final int texId1 = texId.getIntKey();
            final int samplerIndex = Math.max(0, texId.getIntValue());


            boolean b = !GlTexture.hasImageResource(texId1);
            if (!b) b = !GlTexture.hasImage(texId1);
            VulkanImage image = GlTexture.getTexture(b ? MissingTexID : texId1).getVulkanImage();

            image.readOnlyLayout();


            //Can assign ANY image to a Sampler: might decouple smapler form image creation + allocifNeeded selectively If Sampler needed
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(image.getImageView())
                    .sampler(image.getSampler());

            final VkWriteDescriptorSet vkWriteDescriptorSet = descriptorWrites.get();
            vkWriteDescriptorSet.sType$Default()
                    .dstBinding(binding)
                    .dstArrayElement(samplerIndex)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo)
                    .dstSet(setHandle);


        }
    }

    //Remove a sampler descriptor from the array: based on the given TextureID
    public void removeFragImage(int id) {
        int samplerIndex = initialisedFragSamplers.getAlignedIDs().get(id);

        this.initialisedFragSamplers.removeTexture(id);

        this.DescriptorStack.get(getSubSetIndex(samplerIndex)).removeTexture(id); //TODO: make subsets unsized to simplify + streamline management


    }

    private static int getSubSetIndex(int samplerIndex) {
        return samplerIndex / maxPerStageDescriptorSamplers;
    }

    //TODO:
    // Designed to mimic PushConstants by pushing a new stack, but for Descriptor sets instead
    // A new DescriptorSet is allocated, allowing capacity to be expanded like PushConstants
    // Intended for Semi-Bindless systems w/ very low texture limits (i.e. Intel iGPus, MoltenVK e.g.)
    public SubSet pushDescriptorSet()
    {

//        try(MemoryStack stack = MemoryStack.stackPush())
        {

            SubSet allocateDescriptorSet = new SubSet(DescriptorStack.size() * maxPerStageDescriptorSamplers, this.vertTextureLimit, maxPerStageDescriptorSamplers);


            Initializer.LOGGER.info("Pushed SetID {} + SubSet {} textureRange: {} -> {}", this.setID, DescriptorStack.size(), DescriptorStack.size()*maxPerStageDescriptorSamplers, DescriptorStack.size()*maxPerStageDescriptorSamplers+maxPerStageDescriptorSamplers);

            return allocateDescriptorSet;
        }

    }

    //Force unconditional update of all descriptor bindings for this Set; is a selective update: does not effect other sets
    public void forceDescriptorUpdate() {
        Arrays.fill(this.needsUpdate, true);
    }


    public boolean checkCapacity() {
        return DescriptorStack.get(0).needsResize();
    }


    //Only used when in Bindless Mode: Semi-bindless used fixed set ranges instead


    //TODO: should nyl be used in fully bindless mode w/ one global subset  (to avoid updating baseOffsets when resizing)
    void resizeSamplerArrays()
    {
        for(SubSet baseSubSet : this.DescriptorStack) {
            int newLimit = baseSubSet.needsResize() ? baseSubSet.resize() : baseSubSet.currentSize();
            try (MemoryStack stack = MemoryStack.stackPush()) {

                baseSubSet.allocSets(newLimit, stack);

                Initializer.LOGGER.info("Resized SetID {} to {}", this.setID, newLimit);

            }
        }
//        this.currentSamplerSize = newLimit;
        forceDescriptorUpdate();
    }

    public long getSet(int currentFrame) {
        //TODO!
        return this.DescriptorStack.get(subSetIndex).getSetHandle(currentFrame);
    }

    public boolean needsUpdate(int frame) {
        return this.needsUpdate[frame];
    }

    public boolean isTexUnInitialised(int shaderTexture) {
        return this.newTex.contains(shaderTexture);
    }

    public int getSetID() {
        return this.setID;
    }

    public void checkSubSets() {
//        subSetIndex=boundSubSet=0;
        final int samplerCount = this.initialisedFragSamplers.currentSize();
        int requiredSubsets = samplerCount / maxPerStageDescriptorSamplers;
        if(requiredSubsets>this.DescriptorStack.size()-1) {
            for (int i = 0; i < requiredSubsets; i++) {

//                int fragCount = Math.min(samplerCount, maxPerStageDescriptorSamplers);


                {
                    this.DescriptorStack.add(pushDescriptorSet());
                }
//                samplerCount-=maxPerStageDescriptorSamplers;

            }
        }

    }

    public int getLoadedTextures() {
        return this.initialisedFragSamplers.currentSize();
    }

    public int getReservedTextures() {
        return this.initialisedFragSamplers.currentLim();
    }
}
