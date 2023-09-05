package net.vulkanmod.vulkan.framebuffer;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.util.MathUtil;
import net.vulkanmod.vulkan.Device;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.vulkanmod.vulkan.Vulkan.*;
import static net.vulkanmod.vulkan.util.VUtil.UINT32_MAX;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class SwapChain extends Framebuffer {
    private static int DEFAULT_DEPTH_FORMAT = 0;

    public static int getDefaultDepthFormat() {
        return DEFAULT_DEPTH_FORMAT;
    }

    private RenderPass renderPass;
    private long[] framebuffers;

    private long swapChain = VK_NULL_HANDLE;
    private List<VulkanImage> swapChainImages;
    private VkExtent2D extent2D;
    public boolean isBGRAformat;
    private boolean vsync = false;

    private int[] currentLayout;

    public SwapChain() {
        DEFAULT_DEPTH_FORMAT = Device.findDepthFormat();

        this.attachmentCount = 2;

        this.depthFormat = DEFAULT_DEPTH_FORMAT;
        createSwapChain();

    }

    public int recreateSwapChain() {
        Synchronization.INSTANCE.waitFences();

        if(this.depthAttachment != null) {
            this.depthAttachment.free();
            this.depthAttachment = null;
        }

        if(!DYNAMIC_RENDERING && framebuffers != null) {
//            this.renderPass.cleanUp();
            Arrays.stream(framebuffers).forEach(id -> vkDestroyFramebuffer(getDevice(), id, null));
            framebuffers = null;
        }

        createSwapChain();

        return this.swapChainImages.size();
    }

    public void createSwapChain() {
        int requestedFrames = Initializer.CONFIG.frameQueueSize;

        try(MemoryStack stack = stackPush()) {
            VkDevice device = Vulkan.getDevice();
            Device.SurfaceProperties surfaceProperties = Device.querySurfaceProperties(device.getPhysicalDevice(), stack);

            VkSurfaceFormatKHR surfaceFormat = getFormat(surfaceProperties.formats);
            int presentMode = getPresentMode(surfaceProperties.presentModes);
            VkExtent2D extent = getExtent(surfaceProperties.capabilities);

            if(extent.width() == 0 && extent.height() == 0) {
                if(swapChain != VK_NULL_HANDLE) {
                    this.swapChainImages.forEach(image -> vkDestroyImageView(device, image.getImageView(), null));
                    vkDestroySwapchainKHR(device, swapChain, null);
                    swapChain = VK_NULL_HANDLE;
                }

                this.width = 0;
                this.height = 0;
                return;
            }

            //Workaround for Mesa
            IntBuffer imageCount = stack.ints(requestedFrames);
//            IntBuffer imageCount = stack.ints(Math.max(surfaceProperties.capabilities.minImageCount(), preferredImageCount));

            if(surfaceProperties.capabilities.maxImageCount() > 0 && imageCount.get(0) > surfaceProperties.capabilities.maxImageCount()) {
                imageCount.put(0, surfaceProperties.capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(Vulkan.getSurface());

            // Image settings
            this.format = surfaceFormat.format();
            this.extent2D = VkExtent2D.create().set(extent);

            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(this.format);
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            Queue.QueueFamilyIndices indices = Queue.getQueueFamilies();

            if(!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(surfaceProperties.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(swapChain);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if(vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            if(swapChain != VK_NULL_HANDLE) {
                this.swapChainImages.forEach(iamge -> vkDestroyImageView(device, iamge.getImageView(), null));
                vkDestroySwapchainKHR(device, swapChain, null);
            }

            swapChain = pSwapChain.get(0);

            vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(device, swapChain, imageCount, pSwapchainImages);

            swapChainImages = new ArrayList<>(imageCount.get(0));

            this.width = extent2D.width();
            this.height = extent2D.height();

            for(int i = 0;i < pSwapchainImages.capacity(); i++) {
                long imageId = pSwapchainImages.get(i);
                long imageView = VulkanImage.createImageView(imageId, this.format, VK_IMAGE_ASPECT_COLOR_BIT, 1);

                swapChainImages.add(new VulkanImage(imageId, this.format, 1, this.width, this.height, 4, 0, imageView));
            }
            currentLayout = new int[this.swapChainImages.size()];

            createDepthResources();

            //RenderPass
            if(this.renderPass == null)
                createRenderPass();

            if(!DYNAMIC_RENDERING)
                createFramebuffers();

        }
    }

    private void createRenderPass() {
        this.hasColorAttachment = true;
        this.hasDepthAttachment = true;

        this.renderPass = new RenderPass.Builder(this).build();
    }

    private void createFramebuffers() {

        try(MemoryStack stack = MemoryStack.stackPush()) {

            framebuffers = new long[swapChainImages.size()];

            for(int i = 0; i < swapChainImages.size(); ++i) {

//                LongBuffer attachments = stack.longs(imageViews.get(i), depthAttachment.getImageView());
                LongBuffer attachments = stack.longs(this.swapChainImages.get(i).getImageView(), depthAttachment.getImageView());

                //attachments = stack.mallocLong(1);
                LongBuffer pFramebuffer = stack.mallocLong(1);

                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
                framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(this.renderPass.getId());
                framebufferInfo.width(this.width);
                framebufferInfo.height(this.height);
                framebufferInfo.layers(1);
                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(Vulkan.getDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                this.framebuffers[i] = pFramebuffer.get(0);
            }
        }
    }

    public void beginRenderPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if(DYNAMIC_RENDERING) {
//            this.colorAttachmentLayout(stack, commandBuffer, Drawer.getCurrentFrame());
//            beginDynamicRendering(commandBuffer, stack);

            this.renderPass.beginDynamicRendering(commandBuffer, stack);
        }
        else {
            this.renderPass.beginRenderPass(commandBuffer, this.framebuffers[Renderer.getCurrentFrame()], stack);
        }

        Renderer.getInstance().setBoundRenderPass(renderPass);
        Renderer.getInstance().setBoundFramebuffer(this);
    }

    public void colorAttachmentLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int frame) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            barrier.oldLayout(this.currentLayout[frame]);
//            barrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            barrier.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            barrier.image(this.swapChainImages.get(frame).getId());
//            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(1);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);

            vkCmdPipelineBarrier(commandBuffer,
                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,  // srcStageMask
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, // dstStageMask
                    0,
                    null,
                    null,
                    barrier// pImageMemoryBarriers
            );

            this.currentLayout[frame] = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
    }

    public void presentLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int frame) {

        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        barrier.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        barrier.newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        barrier.image(this.swapChainImages.get(frame).getId());

        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);

        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);

        vkCmdPipelineBarrier(commandBuffer,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,  // srcStageMask
                VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, // dstStageMask
                0,
                null,
                null,
                barrier// pImageMemoryBarriers
        );

        this.currentLayout[frame] = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    }

    public void cleanUp() {
        VkDevice device = Vulkan.getDevice();

//        framebuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));

        if(!DYNAMIC_RENDERING) {
            Arrays.stream(framebuffers).forEach(id -> vkDestroyFramebuffer(device, id, null));
        }

        vkDestroySwapchainKHR(device, this.swapChain, null);
        swapChainImages.forEach(image -> vkDestroyImageView(device, image.getImageView(), null));

        this.depthAttachment.free();
    }

    private void createDepthResources() {
        this.depthAttachment = VulkanImage.createDepthImage(depthFormat, this.width, this.height,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                false, false);
    }

    public long getId() {
        return swapChain;
    }

    public List<VulkanImage> getImages() {
        return swapChainImages;
    }

    public long getImageId(int i) {
        return swapChainImages.get(i).getId();
    }

    public VkExtent2D getExtent() {
        return extent2D;
    }

    public VulkanImage getColorAttachment() {
        return this.swapChainImages.get(Renderer.getCurrentFrame());
    }

    public long getImageView(int i) { return this.swapChainImages.get(i).getImageView(); }

    private VkSurfaceFormatKHR getFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        List<VkSurfaceFormatKHR> list = availableFormats.stream().toList();

        VkSurfaceFormatKHR format = list.get(0);

        for (VkSurfaceFormatKHR availableFormat : list) {
            if (availableFormat.format() == VK_FORMAT_R8G8B8A8_UNORM && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                return availableFormat;

            if (availableFormat.format() == VK_FORMAT_B8G8R8A8_UNORM && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                format = availableFormat;
            }
        }

        if(format.format() == VK_FORMAT_B8G8R8A8_UNORM)
            isBGRAformat = true;
        return format;
    }

    private int getPresentMode(IntBuffer availablePresentModes) {
        int requestedMode = vsync ? VK_PRESENT_MODE_FIFO_KHR : VK_PRESENT_MODE_IMMEDIATE_KHR;

        //fifo mode is the only mode that has to be supported
        if(requestedMode == VK_PRESENT_MODE_FIFO_KHR)
            return VK_PRESENT_MODE_FIFO_KHR;

        for(int i = 0;i < availablePresentModes.capacity();i++) {
            if(availablePresentModes.get(i) == requestedMode) {
                return requestedMode;
            }
        }

        Initializer.LOGGER.warn("Requested mode not supported: using fallback VK_PRESENT_MODE_FIFO_KHR");
        return VK_PRESENT_MODE_FIFO_KHR;

    }

    private static VkExtent2D getExtent(VkSurfaceCapabilitiesKHR capabilities) {

        if(capabilities.currentExtent().width() != UINT32_MAX) {
            return capabilities.currentExtent();
        }

        //Fallback
        IntBuffer width = stackGet().ints(0);
        IntBuffer height = stackGet().ints(0);

        glfwGetFramebufferSize(window, width, height);

        VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(MathUtil.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(MathUtil.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
    }

    public RenderPass getRenderPass() {
        return renderPass;
    }

    public int getFramesNum() { return this.swapChainImages.size(); }
}
