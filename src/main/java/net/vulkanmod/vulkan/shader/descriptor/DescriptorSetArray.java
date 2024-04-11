package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.vulkan.DeviceManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddressSafe;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetArray {
    private static final VkDevice DEVICE = Vulkan.getDevice();
    private static final int UNIFORM_POOLS = 1;
    private static final int SAMPLER_MAX_LIMIT = 2;/*16*/;
    static final int VERT_UBO_ID = 0;
    static final int FRAG_UBO_ID = 1;
    static final int VERTEX_SAMPLER_ID = 3;
    static final int FRAG_SAMPLER_ID = 2;
    private static final int bindingsSize = 4;
//    private Int2ObjectLinkedOpenHashMap<Descriptor> DescriptorTableHeap;
//    private final Int2LongArrayMap perBindingSlowLayouts = new Int2LongArrayMap(bindingsSize);
    private final long descriptorSetLayout;
    private final long globalDescriptorPoolArrayPool;
    private final LongBuffer descriptorSets;

    public void addTexture(int binding, VulkanImage vulkanImage)
    {

    }

    public DescriptorSetArray() {




        // *createAndAllocateDescriptorSets*//

        try (MemoryStack stack = stackPush()) {


            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindingsSize, stack);
            IntBuffer bindingFlags = stack.callocInt(bindingsSize);






            bindings.get(VERT_UBO_ID)
                    .binding(VERT_UBO_ID)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            bindingFlags.put(VERT_UBO_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT);

            bindings.get(FRAG_UBO_ID)
                    .binding(FRAG_UBO_ID)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_UBO_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT);

            bindings.get(VERTEX_SAMPLER_ID)
                    .binding(VERTEX_SAMPLER_ID)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            bindingFlags.put(VERTEX_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK12.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT);


            bindings.get(FRAG_SAMPLER_ID)
                    .binding(FRAG_SAMPLER_ID)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImmutableSamplers(null)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            bindingFlags.put(FRAG_SAMPLER_ID, VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK12.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT);






            {
                VkDescriptorSetLayoutBindingFlagsCreateInfo setLayoutBindingsFlags = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack)
                        .sType$Default()
                        .bindingCount(4)
                        .pBindingFlags(bindingFlags);


                VkDescriptorSetLayoutCreateInfo vkDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                        .sType$Default()
                        .pNext(setLayoutBindingsFlags)
                        .pBindings(bindings)
                        .flags(VK12.VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT);

                LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

                if (vkCreateDescriptorSetLayout(DeviceManager.device, vkDescriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor set layout");
                }

                this.descriptorSetLayout=pDescriptorSetLayout.get(0);
            }




            this.globalDescriptorPoolArrayPool = createGlobalDescriptorPool();

            this.descriptorSets = allocateDescriptorSets(stack);

        }
    }


    private LongBuffer allocateDescriptorSets(MemoryStack stack) {



        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType$Default();
        allocInfo.descriptorPool(this.globalDescriptorPoolArrayPool);
        allocInfo.pSetLayouts(stack.longs(descriptorSetLayout, descriptorSetLayout));

        LongBuffer dLongBuffer = MemoryUtil.memAllocLong(2);

        int result = vkAllocateDescriptorSets(DEVICE, allocInfo, dLongBuffer);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
        }
        return dLongBuffer;
    }

    public long createGlobalDescriptorPool()
    {
        try(MemoryStack stack = MemoryStack.stackPush()) {
//            int size = DescriptorTableHeap.size();
            //TODO: Separate descriptorSet for each type: allows for the ability to selectively update+bind DescriptorSets
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);


                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
//                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.descriptorCount(2);

//                VkDescriptorPoolSize uniformBufferPoolSize2 = poolSizes.get(FRAG_UBO_ID);
////                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
//                uniformBufferPoolSize2.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
//                uniformBufferPoolSize2.descriptorCount(UNIFORM_POOLS);



                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(2);

//                VkDescriptorPoolSize textureSamplerPoolSize2 = poolSizes.get(FRAG_SAMPLER_ID);
//                textureSamplerPoolSize2.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
//                textureSamplerPoolSize2.descriptorCount(SAMPLER_MAX_LIMIT);


            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.flags(VK12.VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(2); //One DSet for each binding

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






    public void updateAndBind(int frame, VkCommandBuffer commandBuffer)
    {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            long uniformBufferId = Renderer.getDrawer().getUniformBuffers().getId(frame);
            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(bindingsSize, stack);
            VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[2];
            VkDescriptorImageInfo.Buffer[] imageInfo = new VkDescriptorImageInfo.Buffer[2];
            int x = 0;
;


            int currentBinding = 0;

            for (int i = 0; i < bufferInfos.length; i++) {



                bufferInfos[i] = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfos[i].buffer(uniformBufferId);
                bufferInfos[i].offset(x);
                bufferInfos[i].range(256);
                x += 256; //TODO: TEMP!  //UniformBuffers.getAlignedSize(ubo.getSize());


                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(currentBinding);
                uboDescriptorWrite.sType$Default();
                uboDescriptorWrite.dstBinding(currentBinding);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfos[i]);
                uboDescriptorWrite.dstSet(this.getDescriptorSet(frame));
                currentBinding++;
            }

            for(int imageSamplerIdx = 0; imageSamplerIdx < bufferInfos.length; imageSamplerIdx++) {
                //Use Global Sampler Table Array
                VulkanImage image = VTextureSelector.getImage(imageSamplerIdx==0 ? 0 : 1); //TODO: Not aligned to SmaplerBindindSlot: unintuitive usage atm


                long view = image.getImageView();
                long sampler = image.getSampler();


                long currentSet = this.getDescriptorSet(frame);


                    image.readOnlyLayout();

                imageInfo[imageSamplerIdx] = VkDescriptorImageInfo.calloc(1, stack);
                imageInfo[imageSamplerIdx].imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo[imageSamplerIdx].imageView(view);


                    imageInfo[imageSamplerIdx].sampler(sampler);


                VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(currentBinding);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(currentBinding);
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo[imageSamplerIdx]);
                samplerDescriptorWrite.dstSet(currentSet);
                currentBinding++;
            }

            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);

//            final LongBuffer descriptorSets = Renderer.getDescriptorSetArray().getDescriptorSets();
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, PipelineManager.getTerrainDirectShader().getLayout(),
                    0, stack.longs(descriptorSets.get(frame)), null);

        }
    }
}
