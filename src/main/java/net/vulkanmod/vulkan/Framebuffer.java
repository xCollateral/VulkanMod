package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;

import static net.vulkanmod.vulkan.Vulkan.getSwapchainExtent;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.vkCmdBeginRendering;

public class Framebuffer {
    public static final int DEFAULT_FORMAT = VK_FORMAT_R8G8B8A8_UNORM;

    public int format;
    public int depthFormat;
    public int width, height;

//    private List<VulkanImage> images;
    private VulkanImage colorAttachment;
    protected VulkanImage depthAttachment;

    public Framebuffer(int width, int height, int format) {
        this(width, height, format, false);
    }

    public Framebuffer(int width, int height, int format, boolean blur) {
        this.format = format;
        this.depthFormat = Vulkan.findDepthFormat();
        this.width = width;
        this.height = height;

        this.colorAttachment = VulkanImage.createTextureImage(format, 1, width, height, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, 0, blur, true);

        createDepthResources(blur);
//        createFramebuffers(width, height);
    }

    public Framebuffer(VulkanImage colorAttachment) {
        this.width = colorAttachment.width;
        this.height = colorAttachment.height;

        this.colorAttachment = colorAttachment;

        this.depthFormat = Vulkan.findDepthFormat();
        createDepthResources(false);
    }

    protected Framebuffer() {}

//    private void createFramebuffers(int width, int height) {
//
//        //List<Long> swapChainFramebuffers = new ArrayList<Long>(Vulkan.getSwapChainImages().size());
//        framebuffers = new ArrayList<>(imagesSize);
//
//        try(MemoryStack stack = stackPush()) {
//
//            LongBuffer attachments = stack.longs(VK_NULL_HANDLE, depthImageView);
//            //attachments = stack.mallocLong(1);
//            LongBuffer pFramebuffer = stack.mallocLong(1);
//
//            // Lets allocate the create info struct once and just update the pAttachments field each iteration
//            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
//            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
//            framebufferInfo.renderPass(Vulkan.getRenderPass());
//            framebufferInfo.width(width);
//            framebufferInfo.height(height);
//            framebufferInfo.layers(1);
//
//            for(long imageView : imageViews) {
//
//                attachments.put(0, imageView);
//
//                framebufferInfo.pAttachments(attachments);
//
//                if(vkCreateFramebuffer(Vulkan.getDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
//                    throw new RuntimeException("Failed to create framebuffer");
//                }
//
//                framebuffers.add(pFramebuffer.get(0));
//            }
//        }
//    }

    protected void createDepthResources(boolean blur) {

        this.depthAttachment = VulkanImage.createDepthImage(depthFormat, this.width, this.height,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                blur, false);

        VkCommandBuffer commandBuffer = Vulkan.beginImmediateCmd();
        this.depthAttachment.transitionImageLayout(MemoryStack.stackPush(), commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        Vulkan.endImmediateCmd();

    }

    public void beginRendering(VkCommandBuffer commandBuffer, MemoryStack stack) {
        this.colorAttachment.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        this.depthAttachment.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        VkRect2D renderArea = VkRect2D.callocStack(stack);
        renderArea.offset(VkOffset2D.callocStack(stack).set(0, 0));
        renderArea.extent(VkExtent2D.calloc(stack).set(this.width, this.height));

        VkRenderingAttachmentInfo.Buffer colorAttachment = VkRenderingAttachmentInfo.calloc(1, stack);
        colorAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
        colorAttachment.imageView(this.colorAttachment.getImageView());
        colorAttachment.imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
        colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
//        colorAttachment.clearValue(clearValues.get(0));

        VkRenderingAttachmentInfo depthAttachment = VkRenderingAttachmentInfo.calloc(stack);
        depthAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
        depthAttachment.imageView(this.depthAttachment.getImageView());
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

        vkCmdBeginRendering(commandBuffer, renderingInfo);
    }

    public void bindAsTexture(VkCommandBuffer commandBuffer, MemoryStack stack) {
        this.colorAttachment.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        VTextureSelector.bindFramebufferTexture(this.colorAttachment);
    }

    public VkViewport.Buffer viewport(MemoryStack stack) {
        VkViewport.Buffer viewport = VkViewport.malloc(1, stack);
        viewport.x(0.0f);
        viewport.y(this.height);
        viewport.width(this.width);
        viewport.height(-this.height);
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        return viewport;
    }

    public VkRect2D.Buffer scissor(MemoryStack stack) {
        VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
        scissor.offset().set(0, 0);
        scissor.extent().set(this.width, this.height);

        return scissor;
    }

    public void cleanUp() {
        this.colorAttachment.free();
        this.depthAttachment.free();
    }

    public long getDepthImageView() { return depthAttachment.getImageView(); }

    public VulkanImage getDepthAttachment() { return depthAttachment; }

    public VulkanImage getColorAttachment() { return colorAttachment; }

}
