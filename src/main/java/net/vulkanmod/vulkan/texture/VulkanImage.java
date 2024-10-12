package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static net.vulkanmod.vulkan.texture.SamplerManager.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanImage {
    public static int DefaultFormat = VK_FORMAT_R8G8B8A8_UNORM;

    private static final VkDevice DEVICE = Vulkan.getVkDevice();

    private long id;
    private long allocation;
    private long mainImageView;

    private long[] levelImageViews;

    private long sampler;

    public final int format;
    public final int aspect;
    public final int mipLevels;
    public final int width;
    public final int height;
    public final int formatSize;
    private final int usage;

    private int currentLayout;

    //Used for swap chain images
    public VulkanImage(long id, int format, int mipLevels, int width, int height, int formatSize, int usage, long imageView) {
        this.id = id;
        this.mainImageView = imageView;

        this.mipLevels = mipLevels;
        this.width = width;
        this.height = height;
        this.formatSize = formatSize;
        this.format = format;
        this.usage = usage;
        this.aspect = getAspect(this.format);

        this.sampler = SamplerManager.getTextureSampler((byte) this.mipLevels, (byte) 0);
    }

    private VulkanImage(Builder builder) {
        this.mipLevels = builder.mipLevels;
        this.width = builder.width;
        this.height = builder.height;
        this.formatSize = builder.formatSize;
        this.format = builder.format;
        this.usage = builder.usage;
        this.aspect = getAspect(this.format);
    }

    public static VulkanImage createTextureImage(Builder builder) {
        VulkanImage image = new VulkanImage(builder);

        image.createImage(builder.mipLevels, builder.width, builder.height, builder.format, builder.usage);
        image.mainImageView = createImageView(image.id, builder.format, image.aspect, builder.mipLevels);

        image.sampler = SamplerManager.getTextureSampler(builder.mipLevels, builder.samplerFlags);

        if (builder.levelViews) {
            image.levelImageViews = new long[builder.mipLevels];

            for (int i = 0; i < builder.mipLevels; ++i) {
                image.levelImageViews[i] = createImageView(image.id, image.format, image.aspect, i, 1);
            }
        }

        return image;
    }

    public static VulkanImage createDepthImage(int format, int width, int height, int usage, boolean blur, boolean clamp) {
        VulkanImage image = VulkanImage.builder(width, height)
                .setFormat(format)
                .setUsage(usage)
                .setLinearFiltering(blur)
                .setClamp(clamp)
                .createVulkanImage();

        return image;
    }

    public static VulkanImage createWhiteTexture() {
        try (MemoryStack stack = stackPush()) {
            int i = 0xFFFFFFFF;
            ByteBuffer buffer = stack.malloc(4);
            buffer.putInt(0, i);

            VulkanImage image = VulkanImage.builder(1, 1)
                    .setFormat(DefaultFormat)
                    .setUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .setLinearFiltering(false)
                    .setClamp(false)
                    .createVulkanImage();
            image.uploadSubTextureAsync(0, image.width, image.height, 0, 0, 0, 0, 0, buffer);
            return image;
//            return createTextureImage(1, 1, 4, false, false, buffer);
        }
    }

    private void createImage(int mipLevels, int width, int height, int format, int usage) {

        try (MemoryStack stack = stackPush()) {

            LongBuffer pTextureImage = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(0L);

            MemoryManager.createImage(width, height, mipLevels,
                    format, VK_IMAGE_TILING_OPTIMAL,
                    usage,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pAllocation);

            id = pTextureImage.get(0);
            allocation = pAllocation.get(0);

            MemoryManager.addImage(this);
        }
    }

    public static int getAspect(int format) {
        return switch (format) {
            case VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT_S8_UINT ->
                    VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT;

            case VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D32_SFLOAT,
                 VK_FORMAT_D16_UNORM -> VK_IMAGE_ASPECT_DEPTH_BIT;

            default -> VK_IMAGE_ASPECT_COLOR_BIT;
        };
    }

    public static boolean isDepthFormat(int format) {
        return switch (format) {
            case VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D24_UNORM_S8_UINT,
                 VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT,
                 VK_FORMAT_D16_UNORM -> true;
            default -> false;
        };
    }

    public static long createImageView(long image, int format, int aspectFlags, int mipLevels) {
        return createImageView(image, format, aspectFlags, 0, mipLevels);
    }

    public static long createImageView(long image, int format, int aspectFlags, int baseMipLevel, int mipLevels) {

        try (MemoryStack stack = stackPush()) {

            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(aspectFlags);
            viewInfo.subresourceRange().baseMipLevel(baseMipLevel);
            viewInfo.subresourceRange().levelCount(mipLevels);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);

            if (vkCreateImageView(DEVICE, viewInfo, null, pImageView) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture image view");
            }

            return pImageView.get(0);
        }
    }

    public void uploadSubTextureAsync(int mipLevel, int width, int height, int xOffset, int yOffset, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        long imageSize = buffer.limit();

        CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().getCommandBuffer();
        try (MemoryStack stack = stackPush()) {
            transferDstLayout(stack, commandBuffer.getHandle());
        }

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        stagingBuffer.align(this.formatSize);

        stagingBuffer.copyBuffer((int) imageSize, buffer);

        ImageUtil.copyBufferToImageCmd(commandBuffer.getHandle(), stagingBuffer.getId(), id, mipLevel, width, height, xOffset, yOffset,
                (int) (stagingBuffer.getOffset() + (unpackRowLength * unpackSkipRows + unpackSkipPixels) * this.formatSize), unpackRowLength, height);

        long fence = DeviceManager.getGraphicsQueue().endIfNeeded(commandBuffer);
        if (fence != VK_NULL_HANDLE)
//            Synchronization.INSTANCE.addFence(fence);
            Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
    }

    private void transferDstLayout(MemoryStack stack, VkCommandBuffer commandBuffer) {
        transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
    }

    public void readOnlyLayout() {
        if (this.currentLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            return;

        CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().getCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            readOnlyLayout(stack, commandBuffer.getHandle());
        }
        DeviceManager.getGraphicsQueue().submitCommands(commandBuffer);
        Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
    }

    public void readOnlyLayout(MemoryStack stack, VkCommandBuffer commandBuffer) {
        transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    }

    public void updateTextureSampler(boolean blur, boolean clamp, boolean mipmaps) {
        byte flags = blur ? LINEAR_FILTERING_BIT : 0;
        flags |= clamp ? CLAMP_BIT : 0;
        flags |= (byte) (mipmaps ? USE_MIPMAPS_BIT | MIPMAP_LINEAR_FILTERING_BIT : 0);

        this.updateTextureSampler(flags);
    }

    public void updateTextureSampler(byte flags) {
        updateTextureSampler(this.mipLevels - 1, flags);
    }

    public void updateTextureSampler(int maxLod, byte flags) {
        this.sampler = SamplerManager.getTextureSampler((byte) maxLod, flags);
    }

    public void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int newLayout) {
        transitionImageLayout(stack, commandBuffer, this, newLayout);
    }

    public static void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, VulkanImage image, int newLayout) {
        if (image.currentLayout == newLayout) {
//            System.out.println("new layout is equal to current layout");
            return;
        }

        int sourceStage, srcAccessMask, destinationStage, dstAccessMask = 0;

        switch (image.currentLayout) {
            case VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR -> {
                srcAccessMask = 0;
                sourceStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
            }
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL -> {
                srcAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> {
                srcAccessMask = VK_ACCESS_SHADER_READ_BIT;
                sourceStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> {
                srcAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
                sourceStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            }
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> {
                srcAccessMask = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
                sourceStage = VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
            }
            default -> throw new RuntimeException("Unexpected value:" + image.currentLayout);
        }

        switch (newLayout) {
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL -> {
                dstAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> {
                dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> {
                dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_COLOR_ATTACHMENT_READ_BIT;
                destinationStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            }
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> {
                dstAccessMask = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
                destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            }
            case VK_IMAGE_LAYOUT_PRESENT_SRC_KHR -> {
                destinationStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
            }
            default -> throw new RuntimeException("Unexpected value:" + newLayout);
        }

        transitionLayout(stack, commandBuffer, image, image.currentLayout, newLayout,
                sourceStage, srcAccessMask, destinationStage, dstAccessMask);
    }

    public static void transitionLayout(MemoryStack stack, VkCommandBuffer commandBuffer, VulkanImage image, int oldLayout, int newLayout,
                                        int sourceStage, int srcAccessMask, int destinationStage, int dstAccessMask) {
        transitionLayout(stack, commandBuffer, image, 0, oldLayout, newLayout,
                sourceStage, srcAccessMask, destinationStage, dstAccessMask);
    }

    public static void transitionLayout(MemoryStack stack, VkCommandBuffer commandBuffer, VulkanImage image, int baseLevel, int oldLayout, int newLayout,
                                        int sourceStage, int srcAccessMask, int destinationStage, int dstAccessMask) {

        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(image.currentLayout);
        barrier.newLayout(newLayout);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image.getId());

        barrier.subresourceRange().baseMipLevel(baseLevel);
        barrier.subresourceRange().levelCount(VK_REMAINING_MIP_LEVELS);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(VK_REMAINING_ARRAY_LAYERS);

        barrier.subresourceRange().aspectMask(image.aspect);

        barrier.srcAccessMask(srcAccessMask);
        barrier.dstAccessMask(dstAccessMask);

        vkCmdPipelineBarrier(commandBuffer,
                sourceStage, destinationStage,
                0,
                null,
                null,
                barrier);

        image.currentLayout = newLayout;
    }

    private static boolean hasStencilComponent(int format) {
        return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT;
    }

    public void free() {
        MemoryManager.getInstance().addToFreeable(this);
    }

    public void doFree() {
        if (this.id == 0L)
            return;

        MemoryManager.freeImage(this.id, this.allocation);

        vkDestroyImageView(Vulkan.getVkDevice(), this.mainImageView, null);

        if (this.levelImageViews != null)
            Arrays.stream(this.levelImageViews).forEach(
                    imageView -> vkDestroyImageView(Vulkan.getVkDevice(), this.mainImageView, null));

        this.id = 0L;
    }

    public int getCurrentLayout() {
        return currentLayout;
    }

    public void setCurrentLayout(int currentLayout) {
        this.currentLayout = currentLayout;
    }

    public long getId() {
        return id;
    }

    public long getAllocation() {
        return allocation;
    }

    public long getImageView() {
        return mainImageView;
    }

    public long getLevelImageView(int i) {
        return levelImageViews[i];
    }

    public long[] getLevelImageViews() {
        return levelImageViews;
    }

    public long getSampler() {
        return sampler;
    }

    public static Builder builder(int width, int height) {
        return new Builder(width, height);
    }

    public static class Builder {
        final int width;
        final int height;

        int format = VulkanImage.DefaultFormat;
        int formatSize;
        byte mipLevels = 1;
        int usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;

        byte samplerFlags = 0;

        boolean levelViews = false;

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
            this.mipLevels = (byte) n;

            if (n > 1)
                this.samplerFlags |= USE_MIPMAPS_BIT | MIPMAP_LINEAR_FILTERING_BIT;

            return this;
        }

        public Builder setUsage(int usage) {
            this.usage = usage;
            return this;
        }

        public Builder addUsage(int usage) {
            this.usage |= usage;
            return this;
        }

        public Builder setLinearFiltering(boolean b) {
            this.samplerFlags |= b ? LINEAR_FILTERING_BIT : 0;
            return this;
        }

        public Builder setClamp(boolean b) {
            this.samplerFlags |= b ? CLAMP_BIT : 0;
            return this;
        }

        public Builder setSamplerReductionModeMin() {
            this.samplerFlags = REDUCTION_MIN_BIT | LINEAR_FILTERING_BIT;
            return this;
        }

        public Builder setLevelViews(boolean b) {
            this.levelViews = b;
            return this;
        }

        public VulkanImage createVulkanImage() {
            this.formatSize = formatSize(this.format);

            return VulkanImage.createTextureImage(this);
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
                case VK_FORMAT_R8G8B8A8_UNORM, VK_FORMAT_R8G8B8A8_SRGB,
                     VK_FORMAT_D32_SFLOAT, VK_FORMAT_D24_UNORM_S8_UINT,
                     VK_FORMAT_R8G8B8A8_UINT, VK_FORMAT_R8G8B8A8_SINT -> 4;
                case VK_FORMAT_R8_UNORM -> 1;

//                default -> throw new IllegalArgumentException(String.format("Unxepcted format: %s", format));
                default -> 0;
            };
        }
    }
}
