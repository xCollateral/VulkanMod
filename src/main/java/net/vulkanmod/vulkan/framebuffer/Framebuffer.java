package net.vulkanmod.vulkan.framebuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2LongArrayMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.vulkan.VK10.*;

public class Framebuffer {
    public static final int DEFAULT_FORMAT = VK_FORMAT_R8G8B8A8_UNORM;

//    private long id;

    protected int format;
    protected int depthFormat;
    protected int width, height;
    protected boolean linearFiltering;
    protected int attachmentCount;

    boolean hasColorAttachment;
    boolean hasDepthAttachment;

    private VulkanImage colorAttachment;
    protected VulkanImage depthAttachment;

    private final ObjectArrayList<RenderPass> renderPasses = new ObjectArrayList<>();

    private final Reference2LongArrayMap<RenderPass> framebufferIds = new Reference2LongArrayMap<>();

    //GL compatibility
    public Framebuffer(VulkanImage colorAttachment) {
        this.width = colorAttachment.width;
        this.height = colorAttachment.height;

        this.colorAttachment = colorAttachment;

        this.depthFormat = SwapChain.getDefaultDepthFormat();
        this.depthAttachment = VulkanImage.createDepthImage(depthFormat, this.width, this.height,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                false, false);
    }

    //SwapChain
    protected Framebuffer() {}

    public Framebuffer(Builder builder) {
        this.format = builder.format;
        this.depthFormat = builder.depthFormat;
        this.width = builder.width;
        this.height = builder.height;
        this.linearFiltering = builder.linearFiltering;
        this.hasColorAttachment = builder.hasColorAttachment;
        this.hasDepthAttachment = builder.hasDepthAttachment;

        this.createImages();
    }

    public void addRenderPass(RenderPass renderPass) {
        this.renderPasses.add(renderPass);
    }

    public void createImages() {
        if(this.hasColorAttachment) {
            this.colorAttachment = VulkanImage.createTextureImage(format, 1, width, height,
                    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    0, linearFiltering, true);
        }

        if(this.hasDepthAttachment) {
            this.depthAttachment = VulkanImage.createDepthImage(depthFormat, this.width, this.height,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    linearFiltering, true);

            this.attachmentCount++;
        }
    }

    public void resize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;

        this.cleanUp();

        this.createImages();
    }

    private long createFramebuffer(RenderPass renderPass) {

        try(MemoryStack stack = MemoryStack.stackPush()) {

            LongBuffer attachments;
            if(colorAttachment != null && depthAttachment != null) {
                attachments = stack.longs(colorAttachment.getImageView(), depthAttachment.getImageView());
            } else if(colorAttachment != null) {
                attachments = stack.longs(colorAttachment.getImageView());
            } else {
                attachments = stack.longs(depthAttachment.getImageView());
            }

            LongBuffer pFramebuffer = stack.mallocLong(1);

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
            framebufferInfo.sType$Default();
            framebufferInfo.renderPass(renderPass.getId());
            framebufferInfo.width(this.width);
            framebufferInfo.height(this.height);
            framebufferInfo.layers(1);
            framebufferInfo.pAttachments(attachments);

            if(VK10.vkCreateFramebuffer(Vulkan.getDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer");
            }

            return pFramebuffer.get(0);
        }
    }

    public void beginRenderPass(VkCommandBuffer commandBuffer, RenderPass renderPass, MemoryStack stack) {
        if(!DYNAMIC_RENDERING) {
            long framebufferId = framebufferIds.computeIfAbsent(renderPass, renderPass1 -> createFramebuffer(renderPass));
            renderPass.beginRenderPass(commandBuffer, framebufferId, stack);
        }
        else
            renderPass.beginDynamicRendering(commandBuffer, stack);

        Renderer.getInstance().setBoundRenderPass(renderPass);
        Renderer.getInstance().setBoundFramebuffer(this);
    }

    public static void endRenderPass(VkCommandBuffer commandBuffer) {
        if(!DYNAMIC_RENDERING)
            Renderer.getInstance().endRenderPass();
        else
            KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);

        Renderer.getInstance().setBoundRenderPass(null);
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
        if(this.colorAttachment != null)
            this.colorAttachment.free();

        if(this.depthAttachment != null)
            this.depthAttachment.free();

        final VkDevice device = Vulkan.getDevice();

        framebufferIds.forEach((renderPass, id) -> {
            vkDestroyFramebuffer(device, id, null);
        });

        framebufferIds.clear();

    }

    public long getDepthImageView() { return depthAttachment.getImageView(); }

    public VulkanImage getDepthAttachment() { return depthAttachment; }

    public VulkanImage getColorAttachment() { return colorAttachment; }

    public int getWidth() { return this.width; }

    public int getHeight() { return this.height; }

    public int getFormat() { return this.format; }

    public int getDepthFormat() { return this.depthFormat; }

    public static class Builder {
        final int width, height;
        int format, depthFormat;

//        int colorAttachments;
        boolean hasColorAttachment;
        boolean hasDepthAttachment;

        boolean linearFiltering;
        boolean depthLinearFiltering;

        public Builder(int width, int height, int colorAttachments, boolean hasDepthAttachment) {
            Validate.isTrue(colorAttachments > 0 || hasDepthAttachment, "At least 1 attachment needed");

            //TODO multi color attachments
            Validate.isTrue(colorAttachments <= 1, "Not supported");

            this.format = DEFAULT_FORMAT;
            this.depthFormat = SwapChain.getDefaultDepthFormat();
            this.linearFiltering = true;
            this.depthLinearFiltering = false;

            this.width = width;
            this.height = height;
            this.hasColorAttachment = colorAttachments == 1;
            this.hasDepthAttachment = hasDepthAttachment;
        }

        public Framebuffer build() {
            return new Framebuffer(this);
        }

        public Builder setFormat(int format) {
            this.format = format;

            return this;
        }

        public Builder setLinearFiltering(boolean b) {
            this.linearFiltering = b;

            return this;
        }

        public Builder setDepthLinearFiltering(boolean b) {
            this.depthLinearFiltering = b;

            return this;
        }

    }
}
