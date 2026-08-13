package net.vulkanmod.vulkan.texture;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import net.vulkanmod.render.texture.ImageUploadHelper;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanImage {
    public static int DefaultFormat = VK_FORMAT_R8G8B8A8_UNORM;

    private static final VkDevice DEVICE = Vulkan.getVkDevice();

    public final String name;
    public final int format;
    public final int aspect;
    public final int arrayLayers;
    public final int mipLevels;
    public final int width;
    public final int height;
    public final int formatSize;
    public final int usage;
    public final int viewType;
    public final int size;

    private long id;
    private long allocation;
    private long mainImageView;
    private final Int2LongArrayMap imageViews = new Int2LongArrayMap(4);

    private final long[] levelImageViews;

    private long sampler;

    private int currentLayout;

    // Used for already allocated images e.g. swap chain images
    public VulkanImage(String name, long id, int format, int mipLevels, int width, int height, int formatSize, int usage, long imageView) {
        this.id = id;
        this.mainImageView = imageView;

        this.name = name;
        this.arrayLayers = 1;
        this.mipLevels = mipLevels;
        this.width = width;
        this.height = height;
        this.formatSize = formatSize;
        this.format = format;
        this.usage = usage;
        this.aspect = getAspect(this.format);
        this.viewType = VK_IMAGE_VIEW_TYPE_2D;

        this.size = width * height * formatSize;
        this.levelImageViews = new long[mipLevels];

        this.sampler = SamplerManager.getDefaultSampler();
    }

    private VulkanImage(Builder builder) {
        this.name = builder.name;
        this.mipLevels = builder.mipLevels;
        this.width = builder.width;
        this.height = builder.height;
        this.arrayLayers = builder.arrayLayers;
        this.formatSize = builder.formatSize;
        this.format = builder.format;
        this.usage = builder.usage;
        this.aspect = getAspect(this.format);
        this.viewType = builder.viewType;

        this.size = width * height * formatSize;
        this.levelImageViews = new long[builder.mipLevels];
    }

    public static VulkanImage createTextureImage(Builder builder) {
        VulkanImage image = new VulkanImage(builder);

        image.createImage();
        image.mainImageView = createImageView(image.id, image.viewType, image.format, image.aspect, image.arrayLayers, 0, image.mipLevels);

        image.sampler = SamplerManager.getSampler(builder.clamp, builder.linearFiltering, builder.mipLevels - 1);

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
            image.uploadSubTextureAsync(0, 0, image.width, image.height, 0, 0, 0, 0, image.width, buffer);
            return image;
        }
    }

    private void createImage() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pTextureImage = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(0L);

            int flags = viewType == VK_IMAGE_VIEW_TYPE_CUBE ? VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT : 0;

            MemoryManager.getInstance()
                         .createImage(width, height, arrayLayers, mipLevels,
                                      format, VK_IMAGE_TILING_OPTIMAL,
                                      usage, flags,
                                      VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                                      pTextureImage,
                                      pAllocation);

            id = pTextureImage.get(0);
            allocation = pAllocation.get(0);

            MemoryManager.addImage(this);

            if (this.name != null) {
                Vulkan.setDebugLabel(stack, VK_OBJECT_TYPE_IMAGE, pTextureImage.get(), this.name);
            }
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

    public static long createImageView(long image, int format, int aspectFlags, int arrayLayers, int mipLevels) {
        return createImageView(image, VK_IMAGE_VIEW_TYPE_2D, format, aspectFlags, arrayLayers, 0, mipLevels);
    }

    public static long createImageView(long image, int viewType, int format, int aspectFlags, int arrayLayers, int baseMipLevel, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(viewType);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(aspectFlags);
            viewInfo.subresourceRange().baseMipLevel(baseMipLevel);
            viewInfo.subresourceRange().levelCount(mipLevels);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(arrayLayers);

            LongBuffer pImageView = stack.mallocLong(1);

            if (vkCreateImageView(DEVICE, viewInfo, null, pImageView) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture image view");
            }

            return pImageView.get(0);
        }
    }

    public void uploadSubTextureAsync(int mipLevel,
                                      int width, int height,
                                      int xOffset, int yOffset,
                                      int unpackSkipRows, int unpackSkipPixels, int unpackRowLength,
                                      ByteBuffer buffer)
    {
        this.uploadSubTextureAsync(mipLevel, 0, width, height,
                                   xOffset, yOffset,
                                   unpackSkipRows, unpackSkipPixels, unpackRowLength,
                                   MemoryUtil.memAddress(buffer));
    }

    public void uploadSubTextureAsync(int mipLevel, int arrayLayer,
                                      int width, int height,
                                      int xOffset, int yOffset,
                                      int unpackSkipRows, int unpackSkipPixels, int unpackRowLength,
                                      ByteBuffer buffer)
    {
        this.uploadSubTextureAsync(mipLevel, arrayLayer, width, height,
                                   xOffset, yOffset,
                                   unpackSkipRows, unpackSkipPixels, unpackRowLength,
                                   MemoryUtil.memAddress(buffer));
    }

    public void uploadSubTextureAsync(int mipLevel, int arrayLayer,
                                      int width, int height,
                                      int xOffset, int yOffset,
                                      int unpackSkipRows, int unpackSkipPixels, int unpackRowLength,
                                      long srcPtr)
    {
        // In GL a row length of 0 means rows are tightly packed, i.e. the row stride is the region width
        final int rowLength = unpackRowLength != 0 ? unpackRowLength : width;

        long uploadSize = (long) (rowLength * (height - 1) + width) * this.formatSize;

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();

        // Use a temporary staging buffer if the upload size is greater than
        // the default staging buffer
        if (uploadSize > stagingBuffer.getBufferSize()) {
            stagingBuffer = new StagingBuffer(uploadSize);
            stagingBuffer.scheduleFree();
        }

        srcPtr += ((long) rowLength * unpackSkipRows + unpackSkipPixels) * this.formatSize;

        stagingBuffer.align(this.formatSize);
        stagingBuffer.copyBuffer((int) uploadSize, srcPtr);

        long bufferId = stagingBuffer.getId();

        VkCommandBuffer commandBuffer = ImageUploadHelper.INSTANCE.getOrStartCommandBuffer().getHandle();
        try (MemoryStack stack = stackPush()) {
            transferDstLayout(stack, commandBuffer);

            final int srcOffset = (int) (stagingBuffer.getOffset());

            ImageUtil.copyBufferToImageCmd(stack, commandBuffer, bufferId, this.id,
                                           arrayLayer, mipLevel, width, height, xOffset, yOffset,
                                           srcOffset, rowLength, height);

            ImageUtil.imageTransferMemoryBarrier(stack, commandBuffer, this, mipLevel);
        }
    }

    private void transferDstLayout(MemoryStack stack, VkCommandBuffer commandBuffer) {
        transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
    }

    public void readOnlyLayout() {
        if (this.currentLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (Renderer.getInstance().getBoundRenderPass() != null) {
                CommandPool.CommandBuffer commandBuffer = ImageUploadHelper.INSTANCE.getOrStartCommandBuffer();
                VkCommandBuffer vkCommandBuffer = commandBuffer.getHandle();

                readOnlyLayout(stack, vkCommandBuffer);
            }
            else {
                readOnlyLayout(stack, Renderer.getCommandBuffer());
            }
        }
    }

    public void readOnlyLayout(MemoryStack stack, VkCommandBuffer commandBuffer) {
        transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    }

    public void setSampler(long sampler) {
        this.sampler = sampler;
    }

    public void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int newLayout) {
        transitionImageLayout(stack, commandBuffer, this, newLayout);
    }

    public static void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, VulkanImage image, int newLayout) {
        if (image.currentLayout == newLayout) {
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
                destinationStage = VK_PIPELINE_STAGE_VERTEX_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
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
                    imageView -> {
                        if (imageView != 0L) {
                            vkDestroyImageView(Vulkan.getVkDevice(), imageView, null);
                        }
                    });

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

    public long getImageView(int format) {
        if (this.format == format) {
            return this.mainImageView;
        }

        long imageView = this.imageViews.get(format);
        if (imageView == VK_NULL_HANDLE) {
            imageView = createImageView(this.id, VK_IMAGE_VIEW_TYPE_2D, format, this.aspect, this.arrayLayers, 0, this.mipLevels);
            this.imageViews.put(format, imageView);
        }

        return imageView;
    }

    public long getLevelImageView(int i) {
        if (this.levelImageViews[i] == 0L) {
            this.levelImageViews[i] = createImageView(this.id, VK_IMAGE_VIEW_TYPE_2D, this.format, this.aspect, this.arrayLayers, i, 1);
        }
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

        String name;
        int format = VulkanImage.DefaultFormat;
        int formatSize;
        int arrayLayers = 1;
        byte mipLevels = 1;
        int usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;
        int viewType = VK_IMAGE_VIEW_TYPE_2D;

        // Sampler settings
        boolean linearFiltering = false;
        boolean clamp = false;
        int reductionMode = -1;

        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setFormat(int format) {
            this.format = format;
            return this;
        }

        public Builder setArrayLayers(int n) {
            this.arrayLayers = (byte) n;
            return this;
        }

        public Builder setMipLevels(int n) {
            this.mipLevels = (byte) n;
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

        public Builder setViewType(int viewType) {
            this.viewType = viewType;
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

        public Builder setSamplerReductionMode(int reductionMode) {
            this.reductionMode = reductionMode;
            return this;
        }

        public VulkanImage createVulkanImage() {
            this.formatSize = formatSize(this.format);

            return VulkanImage.createTextureImage(this);
        }

        private static int formatSize(int format) {
            return switch (format) {
                case VK_FORMAT_R8G8B8A8_UNORM, VK_FORMAT_R8G8B8A8_SRGB,
                     VK_FORMAT_D32_SFLOAT, VK_FORMAT_D24_UNORM_S8_UINT,
                     VK_FORMAT_R8G8B8A8_UINT, VK_FORMAT_R8G8B8A8_SINT,
                     VK_FORMAT_R32_SFLOAT -> 4;
                case VK_FORMAT_R16_SFLOAT -> 2;
                case VK_FORMAT_R8_UNORM -> 1;
                case VK_FORMAT_R16G16B16A16_SFLOAT -> 8;

                default -> throw new IllegalArgumentException(String.format("Unxepcted format: %s", format));
            };
        }
    }
}
