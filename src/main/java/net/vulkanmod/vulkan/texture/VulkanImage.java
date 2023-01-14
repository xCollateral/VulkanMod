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
import org.lwjgl.system.libc.LibCString;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanImage {
    private static final VkDevice device = Vulkan.getDevice();

    private long id;
    private long textureImageMemory;
    private long allocation;
    private long textureImageView;

    private final Byte2LongMap samplers;
    private long textureSampler;

    private final int mipLevels;
    private final int width;
    private final int height;
    private final int formatSize;

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

            MemoryManager.getInstance().createImage(width, height, miplevel,
                    VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pAllocation, VK_IMAGE_LAYOUT_UNDEFINED);

            id = pTextureImage.get(0);
            allocation = pAllocation.get(0);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadTexture(int width, int height, int formatSize, ByteBuffer buffer) {
        try(MemoryStack stack = stackPush()) {
            long imageSize = (long) width * height * formatSize;

            commandBuffer = TransferQueue.beginCommands();
            transferDstLayout();

            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());

            stagingBuffer.copyBuffer((int)imageSize, buffer);

            copyBufferToImage(stagingBuffer.getBufferId(), id, 0, width, height, 0, 0, stagingBuffer.getOffset(), 0, 0);
        }

    }

    public void uploadSubTexture(int mipLevel, int width, int height, int xOffset, int yOffset, int formatSize, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        try(MemoryStack stack = stackPush()) {

            long imageSize = buffer.limit();

            transferDstLayout();

            LongBuffer pStagingBuffer = stack.mallocLong(1);
            PointerBuffer pStagingAllocation = stack.pointers(0L);

            MemoryManager.getInstance().createBuffer(imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                    pStagingBuffer,
                    pStagingAllocation);

            MemoryManager.getInstance().MapAndCopy(pStagingAllocation.get(0), imageSize,
                    (data) -> VUtil.memcpy(data.getByteBuffer(0, (int)imageSize), buffer, imageSize)
            );

            copyBufferToImage(pStagingBuffer.get(0), id, mipLevel, width, height, xOffset, yOffset, (unpackRowLength * unpackSkipRows + unpackSkipPixels) * 4, unpackRowLength, height);

            MemoryManager.getInstance().freeBuffer(pStagingBuffer.get(0), pStagingAllocation.get(0));
//            MemoryManager.addToFreeable(pStagingBuffer.get(0), pStagingAllocation.get(0));

        }
    }

    public void uploadSubTextureAsync(int mipLevel, int width, int height, int xOffset, int yOffset, int formatSize, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        int imageSize = buffer.limit();

        commandBuffer = TransferQueue.getCommandBuffer();
        transferDstLayout();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());

        stagingBuffer.copyBuffer(imageSize, buffer);

        copyBufferToImageAsync(stagingBuffer.getBufferId(), id, mipLevel, width, height, xOffset, yOffset,
                stagingBuffer.getOffset() + (unpackRowLength * unpackSkipRows + unpackSkipPixels) * 4, unpackRowLength, height);

        long fence = TransferQueue.endIfNeeded(commandBuffer);
        if (fence != -1) Synchronization.addFence(fence);
    }

    public static void downloadTexture(int width, int height, int formatSize, ByteBuffer buffer, long image) {
        try(MemoryStack stack = stackPush()) {
            int imageSize = width * height * formatSize;

//            LongBuffer pStagingBuffer = stack.mallocLong(1);
//            PointerBuffer pStagingAllocation = stack.pointers(0L);
//            MemoryManager.getInstance().createBuffer(imageSize,
//                    VK_BUFFER_USAGE_TRANSFER_DST_BIT,
//                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
//                    pStagingBuffer,
//                    pStagingAllocation);


            PointerBuffer vmaAllocation = stack.pointers(0L);
            long pTextureImage1 = createImg(width, vmaAllocation, height).get(0);
            VkSubresourceLayout subresourceLayout = blitImage(pTextureImage1, image, width, height, stack);



//            copyImageToBuffer2(pStagingBuffer.get(0), pTextureImage1, width, height);


            long allocation1 = vmaAllocation.get(0);


                PointerBuffer data = stack.mallocPointer(1);

    //            vkMapMemory(Vulkan.getDevice(), allocation, 0, bufferSize, 0, data);
    //            consumer.accept(data);
    //            vkUnmapMemory(Vulkan.getDevice(), allocation);

            long rowPitch = subresourceLayout.rowPitch();
            long arrayPitch = subresourceLayout.arrayPitch();
            long depthPitch = subresourceLayout.depthPitch();
            long offset = subresourceLayout.offset();
            System.out.println(rowPitch);
            System.out.println(arrayPitch);
            System.out.println(depthPitch);
            System.out.println(offset);
            vmaMapMemory(Vulkan.getAllocator(), allocation1, data);
            BGR2RGB(MemoryUtil.memAddress0(buffer), width, data.get(0), height, rowPitch);
                vmaUnmapMemory(Vulkan.getAllocator(), allocation1);


            //            MemoryManager.getInstance().freeBuffer(pStagingBuffer.get(0), pStagingAllocation.get(0));
        }

    }

    private static PointerBuffer createImg(int width, PointerBuffer vmaAllocation, int height) {
        try (MemoryStack stack = stackPush()) {

            PointerBuffer vkImage = stack.mallocPointer(1);
            //LongBuffer pAllocation = stack.mallocLong(1);

            VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(VK_FORMAT_R8G8B8A8_UNORM);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.tiling(VK_IMAGE_TILING_LINEAR);
            imageInfo.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
//            imageInfo.pQueueFamilyIndices(stack.ints(0, 1));

            VmaAllocationCreateInfo allocationInfo = VmaAllocationCreateInfo.callocStack(stack);
            allocationInfo.flags(VMA_ALLOCATION_CREATE_MAPPED_BIT);
            allocationInfo.usage(VMA_MEMORY_USAGE_AUTO_PREFER_HOST);
            allocationInfo.requiredFlags(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);

            allocationInfo.pool(NULL);
//            allocationInfo.requiredFlags(memProperties);

            long allocator = Vulkan.getAllocator();

            int i = nvmaCreateImage(allocator, imageInfo.address(), allocationInfo.address(), vkImage.address0(), memAddress(vmaAllocation), NULL);
            if (i != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image: " + i);

            }
            return vkImage;
        }
    }

    private static VkSubresourceLayout blitImage(long dst, long src, int width, int height, MemoryStack stack) {

        TransferQueue.CommandBuffer commandBuffer = TransferQueue.beginCommands();

        VkImageSubresourceLayers vkImageSubresourceLayers = VkImageSubresourceLayers.callocStack(stack)
                .set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);

//        VkOffset3D.Buffer set = VkOffset3D.callocStack(2, stack);
//        set.get(0).set(0,0,0);
//        set.get(1).set(width, height, 1);


        VkImageCopy.Buffer imgBlt = VkImageCopy.callocStack(1, stack)
                .srcSubresource(vkImageSubresourceLayers)
                .dstSubresource(vkImageSubresourceLayers)
                .extent(VkExtent3D.malloc().set(width, height, 1));


        transitionImageLayout(commandBuffer.getHandle(), src, VK_FORMAT_B8G8R8A8_UNORM,
                VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, 1);
//
        transitionImageLayout(commandBuffer.getHandle(), dst, VK_FORMAT_R8G8B8A8_UNORM,
                VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1);


        vkCmdCopyImage(commandBuffer.getHandle(), src, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, dst, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imgBlt);

//        transitionImageLayout(commandBuffer.getHandle(), dst, VK_FORMAT_R8G8B8A8_UNORM,
//                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL, 1);

        VkImageSubresource subResource = VkImageSubresource.callocStack(stack)
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        VkSubresourceLayout subResourceLayout = VkSubresourceLayout.mallocStack(stack);

        vkGetImageSubresourceLayout(device, dst, subResource, subResourceLayout);

        long fence = TransferQueue.endCommands(commandBuffer);

        vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);

        return subResourceLayout;
    }

    private static void BGR2RGB(long srcDst, int width, long src, long height, long rowPitch) {
        LibCString.nmemcpy(srcDst, src, (width*height*4));
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

    private static void copyImageToBuffer2(long buffer, long image, int width, int height) {

        try(MemoryStack stack = stackPush()) {

            TransferQueue.CommandBuffer commandBuffer = TransferQueue.beginCommands();

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);   // Tightly packed
            region.bufferImageHeight(0);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.callocStack(stack).set(width, height, 1));
            //Chek SrcLayout
//            transitionImageLayout(commandBuffer.getHandle(), image, VK_FORMAT_B8G8R8A8_UNORM, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, 0);

            vkCmdCopyImageToBuffer(commandBuffer.getHandle(), image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, buffer, region);

            long fence = TransferQueue.endCommands(commandBuffer);

            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
        }
    }
    private static void copyImageToBuffer(long buffer, long image, int width, int height) {

        try(MemoryStack stack = stackPush()) {

            TransferQueue.CommandBuffer commandBuffer = TransferQueue.beginCommands();

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);   // Tightly packed
            region.bufferImageHeight(0);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.callocStack(stack).set(width, height, 1));

            vkCmdCopyImageToBuffer(commandBuffer.getHandle(), image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, buffer, region);

            long fence = TransferQueue.endCommands(commandBuffer);

            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
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

                barrier.srcAccessMask(VK13.VK_ACCESS_NONE);
                barrier.dstAccessMask(VK13.VK_ACCESS_NONE);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;

            } else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {

                barrier.srcAccessMask(VK13.VK_ACCESS_NONE);
                barrier.dstAccessMask(VK13.VK_ACCESS_NONE);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT ;
                destinationStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;

            }  else if(oldLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR && newLayout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {

                barrier.srcAccessMask(VK13.VK_ACCESS_NONE);
                barrier.dstAccessMask(VK13.VK_ACCESS_NONE);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;

            }else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_GENERAL) {

                barrier.srcAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT ;
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
