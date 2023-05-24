package net.vulkanmod.vulkan;

import net.minecraft.util.Mth;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.vulkan.Vulkan.getSwapchainExtent;
import static net.vulkanmod.vulkan.Vulkan.window;
import static net.vulkanmod.vulkan.util.VUtil.UINT32_MAX;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class SwapChain extends Framebuffer {

    private long swapChain = VK_NULL_HANDLE;
    private List<Long> swapChainImages;
    private VkExtent2D extent2D;
    private List<Long> imageViews;
    public boolean isBGRAformat;
    private boolean vsync = false;

    private final int framesNum;

    private int[] currentLayout;

    public SwapChain() {
        this.framesNum = Initializer.CONFIG.frameQueueSize;
        createSwapChain(this.framesNum);

        MemoryManager.createInstance(this.swapChainImages.size());

        this.depthFormat = Vulkan.findDepthFormat();
        createDepthResources(false);
    }

    public void recreateSwapChain() {
        Synchronization.INSTANCE.waitFences();

        createSwapChain(this.framesNum);

        int framesNum = this.swapChainImages.size();

        if (MemoryManager.getFrames() != framesNum) {
            MemoryManager.createInstance(framesNum);
        }

        this.depthAttachment.free();
        createDepthResources(false);
    }

    private void createSwapChain(int preferredImageCount) {

        try(MemoryStack stack = stackPush()) {
            VkDevice device = Vulkan.getDevice();
            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device.getPhysicalDevice(), stack);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);

            //Workaround for Mesa
            IntBuffer imageCount = stack.ints(preferredImageCount);
//            IntBuffer imageCount = stack.ints(Math.max(swapChainSupport.capabilities.minImageCount(), preferredImageCount));

            if(swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
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

            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(swapChain);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if(vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            if(swapChain != VK_NULL_HANDLE) {
                this.imageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));
                vkDestroySwapchainKHR(device, swapChain, null);
            }

            swapChain = pSwapChain.get(0);

            vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(device, swapChain, imageCount, pSwapchainImages);

            swapChainImages = new ArrayList<>(imageCount.get(0));

            for(int i = 0;i < pSwapchainImages.capacity();i++) {
                swapChainImages.add(pSwapchainImages.get(i));
            }

            createImageViews(this.format);

            this.width = extent2D.width();
            this.height = extent2D.height();
            currentLayout = new int[this.swapChainImages.size()];
        }
    }

    public void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int newLayout, int frame) {
        VulkanImage.transitionImageLayout(stack, commandBuffer, this.getImageId(frame), this.format, this.currentLayout[frame], newLayout, 1);
        this.currentLayout[frame] = newLayout;
    }

    public void beginRendering(VkCommandBuffer commandBuffer, MemoryStack stack) {
        VkRect2D renderArea = VkRect2D.callocStack(stack);
        renderArea.offset(VkOffset2D.callocStack(stack).set(0, 0));
        renderArea.extent(getSwapchainExtent());

        VkRenderingAttachmentInfo.Buffer colorAttachment = VkRenderingAttachmentInfo.calloc(1, stack);
        colorAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
        colorAttachment.imageView(this.getImageView(Drawer.getCurrentFrame()));
        colorAttachment.imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
        colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
//        colorAttachment.clearValue(clearValues.get(0));

        VkRenderingAttachmentInfo depthAttachment = VkRenderingAttachmentInfo.calloc(stack);
        depthAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
        depthAttachment.imageView(this.getDepthImageView());
        depthAttachment.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
        depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
//        depthAttachment.clearValue(clearValues.get(1));

        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack);
        renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_INFO_KHR);
        renderingInfo.renderArea(renderArea);
        renderingInfo.layerCount(1);
        renderingInfo.pColorAttachments(colorAttachment);
        renderingInfo.pDepthAttachment(depthAttachment);

//        vkCmdBeginRendering(commandBuffer, renderingInfo);
        KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffer, renderingInfo);
    }

    public void colorAttachmentLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int frame) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            barrier.oldLayout(this.currentLayout[frame]);
//            barrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            barrier.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            barrier.image(this.swapChainImages.get(frame));
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
        barrier.image(this.swapChainImages.get(frame));

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
        vkDestroySwapchainKHR(device, this.swapChain, null);
        imageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));

        this.depthAttachment.free();
    }

    private void createImageViews(int format) {
        imageViews = new ArrayList<>(swapChainImages.size());

        for(long swapChainImage : swapChainImages) {
            imageViews.add(VulkanImage.createImageView(swapChainImage, format, VK_IMAGE_ASPECT_COLOR_BIT, 1));
        }

    }

    public long getId() {
        return swapChain;
    }

    public List<Long> getImages() {
        return swapChainImages;
    }

    public long getImageId(int i) {
        return swapChainImages.get(i);
    }

    public VkExtent2D getExtent() {
        return extent2D;
    }

//    public List<Long> getFramebuffers() {
//        return swapChainFramebuffers.getFramebuffers();
//    }

    public List<Long> getImageViews() {
        return this.imageViews;
    }

    public long getImageView(int i) { return this.imageViews.get(i); }

    static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {

        long surface = Vulkan.getSurface();
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if(count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.mallocStack(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

        if(count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes);
        }

        return details;
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        List<VkSurfaceFormatKHR> list = availableFormats.stream().toList();

        VkSurfaceFormatKHR format = list.get(0);
        boolean flag = true;

        for (VkSurfaceFormatKHR availableFormat : list) {
            if (availableFormat.format() == VK_FORMAT_R8G8B8A8_UNORM && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                return availableFormat;

            if (availableFormat.format() == VK_FORMAT_B8G8R8A8_UNORM && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                format = availableFormat;
                flag = false;
            }
        }

        if(format.format() == VK_FORMAT_B8G8R8A8_UNORM) isBGRAformat = true;
        return format;
    }

    private int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        int requestedMode = vsync ? VK_PRESENT_MODE_FIFO_KHR : VK_PRESENT_MODE_IMMEDIATE_KHR;

        //fifo mode is the only mode that has to be supported
        if(requestedMode == VK_PRESENT_MODE_FIFO_KHR) return VK_PRESENT_MODE_FIFO_KHR;

        for(int i = 0;i < availablePresentModes.capacity();i++) {
            if(availablePresentModes.get(i) == requestedMode) {
                return requestedMode;
            }
        }

        Initializer.LOGGER.warn("Requested mode not supported: using fallback VK_PRESENT_MODE_FIFO_KHR");
        return VK_PRESENT_MODE_FIFO_KHR;

    }

    private static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {

        if(capabilities.currentExtent().width() != UINT32_MAX) {
            return capabilities.currentExtent();
        }

        IntBuffer width = stackGet().ints(0);
        IntBuffer height = stackGet().ints(0);

        glfwGetFramebufferSize(window, width, height);

        VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(Mth.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(Mth.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    static class SwapChainSupportDetails {

        VkSurfaceCapabilitiesKHR capabilities;
        VkSurfaceFormatKHR.Buffer formats;
        IntBuffer presentModes;

    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
    }
}
