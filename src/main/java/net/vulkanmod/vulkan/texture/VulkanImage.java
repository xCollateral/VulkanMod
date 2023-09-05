package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.bytes.Byte2LongArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongMap;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.GraphicsQueue;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Objects;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanImage {
    public static int DefaultFormat = VK_FORMAT_R8G8B8A8_UNORM;

    private static VkDevice device = Vulkan.getDevice();

    private long id;
    private long allocation;
    private long imageView;

    private final Byte2LongMap samplers = new Byte2LongArrayMap();
    private Sampler textureSampler;

    public final int format;
    public final int mipLevels;
    public final int width;
    public final int height;
    public final int formatSize;

    private int usage;

    private int currentLayout;

    public VulkanImage(long id, int format, int mipLevels, int width, int height, int formatSize, int usage, long imageView) {
        this.id = id;
        this.imageView = imageView;

        this.mipLevels = mipLevels;
        this.width = width;
        this.height = height;
        this.formatSize = formatSize;
        this.format = format;
        this.usage = usage;
    }

    private VulkanImage(int format, int mipLevels, int width, int height, int usage, int formatSize) {
        this.mipLevels = mipLevels;
        this.width = width;
        this.height = height;
        this.formatSize = formatSize;
        this.format = format;
        this.usage = usage;
    }

    public static VulkanImage createTextureImage(int format, int mipLevels, int width, int height, int usage, int formatSize, boolean blur, boolean clamp) {
        VulkanImage image = new VulkanImage(format, mipLevels, width, height, usage, formatSize);

        image.createImage(mipLevels, width, height, format, usage);
        image.imageView = createImageView(image.id, format, VK_IMAGE_ASPECT_COLOR_BIT, mipLevels);
        image.createTextureSampler(blur, clamp, mipLevels > 1);

        return image;
    }

    public static VulkanImage createDepthImage(int format, int width, int height, int usage, boolean blur, boolean clamp) {
        VulkanImage image = new VulkanImage(format, 1, width, height, usage, 0);

        image.createImage(1, width, height, format, usage);
        image.imageView = createImageView(image.id, format, VK_IMAGE_ASPECT_DEPTH_BIT, 1);
        image.createTextureSampler(blur, clamp, false);

        return image;
    }

    public static VulkanImage createWhiteTexture() {
        try(MemoryStack stack = stackPush()) {
            int i = 0xFFFFFFFF;
            ByteBuffer buffer = stack.malloc(4);
            buffer.putInt(0, i);

            VulkanImage image = createTextureImage(DefaultFormat, 1, 1, 1, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, 4, false, false);
            image.uploadSubTextureAsync(0,image.width, image.height, 0, 0, 0, 0, 0, buffer);
            return image;
//            return createTextureImage(1, 1, 4, false, false, buffer);
        }
    }

    private void createImage(int mipLevels, int width, int height, int format, int usage) {

        try(MemoryStack stack = stackPush()) {

            LongBuffer pTextureImage = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(0L);

            MemoryManager.getInstance().createImage(width, height, mipLevels,
                    format, VK_IMAGE_TILING_OPTIMAL,
                    usage,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pAllocation);

            id = pTextureImage.get(0);
            allocation = pAllocation.get(0);

            MemoryManager.getInstance().addImage(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long createImageView(long image, int format, int aspectFlags, int mipLevels) {

        try(MemoryStack stack = stackPush()) {

            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.callocStack(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(aspectFlags);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(mipLevels);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);

            if(vkCreateImageView(device, viewInfo, null, pImageView) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture image view");
            }

            return pImageView.get(0);
        }
    }

    public void uploadSubTextureAsync(int mipLevel, int width, int height, int xOffset, int yOffset, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        long imageSize = buffer.limit();

        CommandPool.CommandBuffer commandBuffer = GraphicsQueue.getInstance().getCommandBuffer();
        transferDstLayout(commandBuffer);

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Renderer.getCurrentFrame());
        stagingBuffer.align(this.formatSize);

        stagingBuffer.copyBuffer((int)imageSize, buffer);

        copyBufferToImageCmd(commandBuffer, stagingBuffer.getId(), id, mipLevel, width, height, xOffset, yOffset,
                (int) (stagingBuffer.getOffset() + (unpackRowLength * unpackSkipRows + unpackSkipPixels) * this.formatSize), unpackRowLength, height);

        long fence = GraphicsQueue.getInstance().endIfNeeded(commandBuffer);
        if (fence != VK_NULL_HANDLE)
//            Synchronization.INSTANCE.addFence(fence);
            Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
    }

    public static void downloadTexture(int width, int height, int formatSize, ByteBuffer buffer, long image) {
        try(MemoryStack stack = stackPush()) {
            long imageSize = width * height * formatSize;

            LongBuffer pStagingBuffer = stack.mallocLong(1);
            PointerBuffer pStagingAllocation = stack.pointers(0L);
            MemoryManager.getInstance().createBuffer(imageSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
                    pStagingBuffer,
                    pStagingAllocation);

            copyImageToBuffer(pStagingBuffer.get(0), image, 0, width, height, 0, 0, 0, 0, 0);

            MemoryManager.getInstance().MapAndCopy(pStagingAllocation.get(0), imageSize,
                    (data) -> VUtil.memcpy(data.getByteBuffer(0, (int)imageSize), buffer)
            );

            MemoryManager.getInstance().freeBuffer(pStagingBuffer.get(0), pStagingAllocation.get(0));
        }

    }

    private void transferDstLayout(CommandPool.CommandBuffer commandBuffer) {
        if (this.currentLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
            return;

        try(MemoryStack stack = stackPush()) {

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(this.currentLayout);
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
        if (this.currentLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            return;

        CommandPool.CommandBuffer commandBuffer = GraphicsQueue.getInstance().getCommandBuffer();
        readOnlyLayout(commandBuffer);
        GraphicsQueue.getInstance().submitCommands(commandBuffer);
        Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
    }

    public void readOnlyLayout(CommandPool.CommandBuffer commandBuffer) {
        if (this.currentLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            return;

        try(MemoryStack stack = stackPush()) {

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(this.currentLayout);
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

        this.currentLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
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
//                samplerInfo.mipLodBias(-1.0F);
            } else {
                samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
                samplerInfo.maxLod(0.0F);
                samplerInfo.minLod(0.0F);
            }

            LongBuffer pTextureSampler = stack.mallocLong(1);

            if(vkCreateSampler(getDevice(), samplerInfo, null, pTextureSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }

            textureSampler = new Sampler(this, pTextureSampler.get(0));

            byte mask = (byte) ((blur ? 1 : 0) | (mipmap ? 2 : 0));
            this.samplers.put(mask, textureSampler.sampler);
        }
    }

    public void updateTextureSampler(boolean blur, boolean clamp, boolean mipmap) {
        byte mask = (byte) ((blur ? 1 : 0) | (mipmap ? 2 : 0));
        long sampler = this.samplers.get(mask);

        if(sampler == 0L)
            createTextureSampler(blur, clamp, mipmap);
        else
            this.textureSampler = new Sampler(this, sampler);
    }

    private void copyBufferToImageCmd(CommandPool.CommandBuffer commandBuffer, long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {

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

            CommandPool.CommandBuffer commandBuffer = GraphicsQueue.getInstance().beginCommands();

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

            long fence = GraphicsQueue.getInstance().submitCommands(commandBuffer);

            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
        }
    }

    public void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int newLayout) {
        transitionImageLayout(stack, commandBuffer, this.id, this.format, this.currentLayout, newLayout, this.mipLevels);

        this.currentLayout = newLayout;
    }

    public static void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, long image, int format, int oldLayout, int newLayout, int mipLevels) {

        if(oldLayout == newLayout) {
//            System.out.println("new layout is equal to current layout");
            return;
        }

        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout);
        barrier.newLayout(newLayout);
//        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
//        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image);

        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(mipLevels);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);

        if(format == VK_FORMAT_D32_SFLOAT) {
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
        } else {
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        }

        int sourceStage;
        int destinationStage;

        switch (oldLayout) {
            case VK_IMAGE_LAYOUT_UNDEFINED -> {
//                barrier.srcAccessMask(0);
                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            }
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> {
                barrier.srcAccessMask(VK_ACCESS_SHADER_READ_BIT);
                sourceStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> {
                barrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                sourceStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            }
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> {
                barrier.srcAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                sourceStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            }
            default -> throw new RuntimeException("Unexpected value");
        }

        switch (newLayout) {
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> {
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> {
                barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                destinationStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            }
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> {
                barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            }
            default -> throw new RuntimeException("Unexpected value");
        }

        vkCmdPipelineBarrier(commandBuffer,
                sourceStage, destinationStage,
                0,
                null,
                null,
                barrier);
    }

    private static boolean hasStencilComponent(int format) {
        return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT;
    }

    public void free() {
        MemoryManager.getInstance().addToFreeable(this);
    }

    public void doFree(MemoryManager memoryManager) {
        memoryManager.freeImage(this.id, this.allocation);

        vkDestroyImageView(Vulkan.getDevice(), this.imageView, null);

        for (long sampler : samplers.values()) {
            vkDestroySampler(Vulkan.getDevice(), sampler, null);
        }
    }

    public int getCurrentLayout() {
        return currentLayout;
    }

    public void setCurrentLayout(int currentLayout) {
        this.currentLayout = currentLayout;
    }

    public long getId() { return id;}
    public long getAllocation() { return allocation;}
    public long getImageView() { return imageView; }
    public Sampler getTextureSampler() { return textureSampler; }

    public static class Builder {
        final int width;
        final int height;

        int format = VulkanImage.DefaultFormat;
        int formatSize;
        int mipLevels = 1;
        int usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;

        boolean linearFiltering = false;
        boolean clamp = false;

        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public Builder setFormat(int format) {
            this.format = format;
            return this;
        }

        public Builder setFormat(NativeImage.InternalGlFormat format) {
            this.format = convertFormat(format);
            return this;
        }

        public Builder setMipLevels(int n) {
            this.mipLevels = n;
            return this;
        }

        public Builder setUsage(int usage) {
            this.usage = usage;
            return this;
        }

        public Builder setLinearFiltering(boolean b) {
            this.linearFiltering = b;
            return this;
        }

        public Builder setClamp(boolean b) {
            this.clamp = b;
            return this;
        }

        public VulkanImage createVulkanImage() {
            this.formatSize = formatSize(this.format);

            return VulkanImage.createTextureImage(this.format, this.mipLevels, this.width, this.height, this.usage, this.formatSize, this.linearFiltering, this.clamp);
        }

        private static int convertFormat(NativeImage.InternalGlFormat format) {
            return switch (format) {
                case RGBA -> VK_FORMAT_R8G8B8A8_UNORM;
                case RED -> VK_FORMAT_R8_UNORM;
                default -> throw new IllegalArgumentException(String.format("Unxepcted format: %s", format));
            };
        }

        private static int formatSize(int format) {
            return switch (format) {
                case VK_FORMAT_R8G8B8A8_UNORM, VK_FORMAT_R8G8B8A8_SRGB -> 4;
                case VK_FORMAT_R8_UNORM -> 1;
                default -> throw new IllegalArgumentException(String.format("Unxepcted format: %s", format));
            };
        }
    }

    public record Sampler(VulkanImage image, long sampler) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Sampler sampler1 = (Sampler) o;

            if (sampler != sampler1.sampler) return false;
            return Objects.equals(image, sampler1.image);
        }

    }
}
