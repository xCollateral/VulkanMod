package net.vulkanmod.vulkan.texture;

import it.unimi.dsi.fastutil.bytes.Byte2LongMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongOpenHashMap;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Vulkan.*;
import static net.vulkanmod.vulkan.memory.MemoryManager.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanImage {
    private static VkDevice device = Vulkan.getDevice();

    private long id;
    private long textureImageMemory;
    private long allocation;
    private long textureImageView;

    private Byte2LongMap samplers;
    private long textureSampler;

    private int mipLevels;
    private int width;
    private int height;
    private int formatSize;

    private int currentLayout;
    private TransferQueue.CommandBuffer commandBuffer;

    public VulkanImage(int width, int height, int formatSize, boolean blur, boolean clamp, ByteBuffer buffer) {
        this(1, width, height, formatSize, blur, clamp);
        uploadTexture(width, height, formatSize, buffer);
    }

    public VulkanImage(int mipLevels, int width, int height, int formatSize, boolean blur, boolean clamp) {
        this.mipLevels = mipLevels;
        this.width = width;
        this.height = height;
        this.formatSize = formatSize;

        this.samplers = new Byte2LongOpenHashMap(8);

        createTextureImage(mipLevels, width, height);
        createTextureImageView(mipLevels);
        createTextureSampler(blur, clamp, mipLevels > 1);
    }

    public VulkanImage(int miplevel, int width, int height) {
        this(miplevel, width, height, 4, false, true);
    }

    public VulkanImage(int width, int height) {
        this(1, width, height, 4, false, false);
    }

    public static VulkanImage createWhiteTexture() {
        int i = 0xFFFFFFFF;
        ByteBuffer buffer = MemoryUtil.memAlloc(4);
        buffer.putInt(0, i);

        VulkanImage image = new VulkanImage(1, 1, 4, false, false, buffer);
        MemoryUtil.memFree(buffer);
        return image;
    }

    private void createTextureImage(int miplevel, int width, int height) {

        try(MemoryStack stack = stackPush()) {

            LongBuffer pTextureImage = stack.mallocLong(1);
            //LongBuffer pAllocation = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(0L);

            MemoryManager.createImage(width, height, miplevel,
                    VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pAllocation);

            id = pTextureImage.get(0);
            allocation = pAllocation.get(0);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadTexture(int width, int height, int formatSize, ByteBuffer buffer) {
        try(MemoryStack stack = stackPush()) {
            long imageSize = width * height * formatSize;

            commandBuffer = TransferQueue.beginCommands();
            transferDstLayout();

            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());

            stagingBuffer.copyBuffer((int)imageSize, buffer);

            copyBufferToImage(stagingBuffer.getBufferId(), id, 0, width, height, 0, 0, (int)stagingBuffer.getOffset(), 0, 0);
        }

    }

    public void uploadSubTexture(int mipLevel, int width, int height, int xOffset, int yOffset, int formatSize, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        try(MemoryStack stack = stackPush()) {

            long imageSize = buffer.limit();

            transferDstLayout();

            LongBuffer pStagingBuffer = stack.mallocLong(1);
            PointerBuffer pStagingAllocation = stack.pointers(0L);

            createBuffer(imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                    pStagingBuffer,
                    pStagingAllocation);

            MapAndCopy(pStagingAllocation.get(0), imageSize,
                    (data) -> VUtil.memcpy(data.getByteBuffer(0, (int)imageSize), buffer, imageSize)
            );

            copyBufferToImage(pStagingBuffer.get(0), id, mipLevel, width, height, xOffset, yOffset, (unpackRowLength * unpackSkipRows + unpackSkipPixels) * 4, unpackRowLength, height);

            freeBuffer(pStagingBuffer.get(0), pStagingAllocation.get(0));
//            MemoryManager.addToFreeable(pStagingBuffer.get(0), pStagingAllocation.get(0));

        }
    }

    public void uploadSubTextureAsync(int mipLevel, int width, int height, int xOffset, int yOffset, int formatSize, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        long imageSize = buffer.limit();

        commandBuffer = TransferQueue.getCommandBuffer();
        transferDstLayout();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());

        stagingBuffer.copyBuffer((int)imageSize, buffer);

        copyBufferToImageAsync(stagingBuffer.getBufferId(), id, mipLevel, width, height, xOffset, yOffset,
                (int) (stagingBuffer.getOffset() + (unpackRowLength * unpackSkipRows + unpackSkipPixels) * 4), unpackRowLength, height);

        long fence = TransferQueue.endIfNeeded(commandBuffer);
        if (fence != -1) Synchronization.addFence(fence);
    }

    public static void downloadTexture(int width, int height, int formatSize, ByteBuffer buffer, long image) {
        try(MemoryStack stack = stackPush()) {
            long imageSize = width * height * formatSize;

            LongBuffer pStagingBuffer = stack.mallocLong(1);
            PointerBuffer pStagingAllocation = stack.pointers(0L);
            createBuffer(imageSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                    pStagingBuffer,
                    pStagingAllocation);

            copyImageToBuffer(pStagingBuffer.get(0), image, 0, width, height, 0, 0, 0, 0, 0);

            MapAndCopy(pStagingAllocation.get(0), imageSize,
                    (data) -> VUtil.memcpy(buffer, data.getByteBuffer(0, (int)imageSize))
            );

            MemoryManager.addToFreeable(pStagingBuffer.get(0), pStagingAllocation.get(0));
        }

    }

    private void transferDstLayout() {
        if (this.currentLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) return;

        try(MemoryStack stack = stackPush()) {

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(this.id);

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(mipLevels);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);

            int sourceStage;
            int destinationStage;

            barrier.srcAccessMask(0);
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

            sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

            //VkCommandBuffer commandBuffer = beginImmediateCmd();

            vkCmdPipelineBarrier(commandBuffer.getHandle(),
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

        }

        currentLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
    }

    public void readOnlyLayout() {
        if (this.currentLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) return;

        TransferQueue.CommandBuffer commandBuffer = TransferQueue.beginCommands();

        try(MemoryStack stack = stackPush()) {

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(this.id);

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(mipLevels);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);

            int sourceStage;
            int destinationStage;

            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            destinationStage = VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;

            vkCmdPipelineBarrier(commandBuffer.getHandle(),
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);
        }

        long fence = TransferQueue.endCommands(commandBuffer);
        if (fence != 0) Synchronization.addFence(fence);


        this.currentLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    }

    private void createTextureImageView(int mipLevels) {
        textureImageView = createImageView(id, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_ASPECT_COLOR_BIT, mipLevels);
    }

    private void createTextureSampler(boolean blur, boolean clamp, boolean mipmap) {

        try(MemoryStack stack = stackPush()) {

            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.callocStack(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);

            if(blur) {
                samplerInfo.magFilter(VK_FILTER_LINEAR);
                samplerInfo.minFilter(VK_FILTER_LINEAR);
            } else {
                samplerInfo.magFilter(VK_FILTER_NEAREST);
                samplerInfo.minFilter(VK_FILTER_NEAREST);
            }


            if(clamp) {
                samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
                samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
                samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            } else {
                samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            }

            samplerInfo.anisotropyEnable(false);
            //samplerInfo.maxAnisotropy(16.0f);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_WHITE);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);

            if(mipmap) {
                samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
                samplerInfo.maxLod(mipLevels);
                samplerInfo.minLod(0.0F);
            } else {
                samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
                samplerInfo.maxLod(0.0F);
                samplerInfo.minLod(0.0F);
            }

            LongBuffer pTextureSampler = stack.mallocLong(1);

            if(vkCreateSampler(getDevice(), samplerInfo, null, pTextureSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }

            textureSampler = pTextureSampler.get(0);

            byte mask = (byte) ((blur ? 1 : 0) | (mipmap ? 2 : 0));
            samplers.put(mask, textureSampler);
        }
    }

    public void updateTextureSampler(boolean blur, boolean clamp, boolean mipmap) {
        byte mask = (byte) ((blur ? 1 : 0) | (mipmap ? 2 : 0));
        long sampler = this.samplers.get(mask);

        if(sampler == 0L) createTextureSampler(blur, clamp, mipmap);
        else this.textureSampler = sampler;
    }

    private void copyBufferToImage(long buffer, long image, int mipLevel, int width, int height) {
        copyBufferToImage(buffer, image, mipLevel, width, height, 0, 0);
    }

    private void copyBufferToImage(long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset){
        copyBufferToImage(buffer, image, mipLevel, width, height, xOffset, yOffset, 0, 0, 0);
    }

    private void copyBufferToImage(long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {

        try(MemoryStack stack = stackPush()) {

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
            region.bufferOffset(bufferOffset);
            region.bufferRowLength(bufferRowLenght);   // Tightly packed
            region.bufferImageHeight(bufferImageHeight);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(mipLevel);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(xOffset, yOffset, 0);
            region.imageExtent(VkExtent3D.callocStack(stack).set(width, height, 1));

            vkCmdCopyBufferToImage(commandBuffer.getHandle(), buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            long fence = TransferQueue.endCommands(commandBuffer);

            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);

        }
    }

    private void copyBufferToImageAsync(long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {

        try(MemoryStack stack = stackPush()) {

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
            region.bufferOffset(bufferOffset);
            region.bufferRowLength(bufferRowLenght);   // Tightly packed
            region.bufferImageHeight(bufferImageHeight);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(mipLevel);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(xOffset, yOffset, 0);
            region.imageExtent(VkExtent3D.callocStack(stack).set(width, height, 1));

            vkCmdCopyBufferToImage(commandBuffer.getHandle(), buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
        }
    }

    private static void copyImageToBuffer(long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {

        try(MemoryStack stack = stackPush()) {

            TransferQueue.CommandBuffer commandBuffer = TransferQueue.beginCommands();

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
            region.bufferOffset(bufferOffset);
            region.bufferRowLength(bufferRowLenght);   // Tightly packed
            region.bufferImageHeight(bufferImageHeight);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(mipLevel);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(xOffset, yOffset, 0);
            region.imageExtent(VkExtent3D.callocStack(stack).set(width, height, 1));

            vkCmdCopyImageToBuffer(commandBuffer.getHandle(), image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, buffer, region);

            long fence = TransferQueue.endCommands(commandBuffer);

            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
        }
    }

    public static void transitionImageLayout(VkCommandBuffer commandBuffer, long image, int format, int oldLayout, int newLayout, int mipLevels) {

        try(MemoryStack stack = stackPush()) {

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(mipLevels);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            if(newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);

                if(hasStencilComponent(format)) {
                    barrier.subresourceRange().aspectMask(
                            barrier.subresourceRange().aspectMask() | VK_IMAGE_ASPECT_STENCIL_BIT);
                }

            } else {
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            }

            int sourceStage;
            int destinationStage;

            if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

            } else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {

                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

            } else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

            } else {
                throw new IllegalArgumentException("Unsupported layout transition");
            }

            vkCmdPipelineBarrier(commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

        }
    }

    private static boolean hasStencilComponent(int format) {
        return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT;
    }

    public long getId() { return id;}
    public long getTextureImageView() { return textureImageView; }
    public long getTextureSampler() { return textureSampler; }
}
