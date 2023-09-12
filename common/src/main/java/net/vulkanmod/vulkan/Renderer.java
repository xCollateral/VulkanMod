package net.vulkanmod.vulkan;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.chunk.AreaUploadManager;
import net.vulkanmod.render.profiling.Profiler2;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.passes.DefaultMainPass;
import net.vulkanmod.vulkan.passes.MainPass;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.shader.ShaderManager;
import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Renderer {
    private static Renderer INSTANCE;

    private static VkDevice device;

    private static boolean swapCahinUpdate = false;
    public static boolean skipRendering = false;

    public static void initRenderer() { INSTANCE = new Renderer(); }

    public static Renderer getInstance() { return INSTANCE; }

    public static Drawer getDrawer() { return INSTANCE.drawer; }

    public static int getCurrentFrame() { return currentFrame; }

    private final Set<Pipeline> usedPipelines = new ObjectOpenHashSet<>();

    private final Drawer drawer;

    private int framesNum;
    private List<VkCommandBuffer> commandBuffers;
    private ArrayList<Long> imageAvailableSemaphores;
    private ArrayList<Long> renderFinishedSemaphores;
    private ArrayList<Long> inFlightFences;

    private Framebuffer boundFramebuffer;
    private RenderPass boundRenderPass;

    private static int currentFrame = 0;
    private VkCommandBuffer currentCmdBuffer;

    MainPass mainPass = DefaultMainPass.PASS;

    private final List<Runnable> onResizeCallbacks = new ObjectArrayList<>();

    public Renderer() {
        device = Vulkan.getDevice();

        Uniforms.setupDefaultUniforms();
        ShaderManager.init();
        AreaUploadManager.createInstance();

        framesNum = getSwapChainImages().size();

        drawer = new Drawer();
        drawer.createResources(framesNum);

        allocateCommandBuffers();
        createSyncObjects();

        AreaUploadManager.INSTANCE.createLists(framesNum);
    }

    private void allocateCommandBuffers() {
        if(commandBuffers != null) {
            commandBuffers.forEach(commandBuffer -> vkFreeCommandBuffers(device, Vulkan.getCommandPool(), commandBuffer));
        }

        commandBuffers = new ArrayList<>(framesNum);

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(framesNum);

            PointerBuffer pCommandBuffers = stack.mallocPointer(framesNum);

            if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for (int i = 0; i < framesNum; i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
            }
        }
    }

    private void createSyncObjects() {
        imageAvailableSemaphores = new ArrayList<>(framesNum);
        renderFinishedSemaphores = new ArrayList<>(framesNum);
        inFlightFences = new ArrayList<>(framesNum);

        try(MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for(int i = 0;i < framesNum; i++) {

                if(vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                imageAvailableSemaphores.add(pImageAvailableSemaphore.get(0));
                renderFinishedSemaphores.add(pRenderFinishedSemaphore.get(0));
                inFlightFences.add(pFence.get(0));

            }

        }
    }

    public void beginFrame() {
        Profiler2 p = Profiler2.getMainProfiler();
        p.push("Frame_fence");

        if(swapCahinUpdate) {
            recreateSwapChain();
            swapCahinUpdate = false;

            if(getSwapChain().getWidth() == 0 && getSwapChain().getHeight() == 0) {
                skipRendering = true;
                Minecraft.getInstance().noRender = true;
            }
            else {
                skipRendering = false;
                Minecraft.getInstance().noRender = false;
            }
        }


        if(skipRendering)
            return;

        vkWaitForFences(device, inFlightFences.get(currentFrame), true, VUtil.UINT64_MAX);

        p.pop();
        p.start();
        p.push("Frame_ops");

        AreaUploadManager.INSTANCE.updateFrame(currentFrame);

        MemoryManager.getInstance().initFrame(currentFrame);
        drawer.setCurrentFrame(currentFrame);

        //Moved before texture updates
//        this.vertexBuffers[currentFrame].reset();
//        this.uniformBuffers.reset();
//        Vulkan.getStagingBuffer(currentFrame).reset();

        resetDescriptors();

        currentCmdBuffer = commandBuffers.get(currentFrame);
        vkResetCommandBuffer(currentCmdBuffer, 0);

        p.pop();

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VkCommandBuffer commandBuffer = currentCmdBuffer;

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer:" + err);
            }

            mainPass.begin(commandBuffer, stack);

            vkCmdSetDepthBias(commandBuffer, 0.0F, 0.0F, 0.0F);
        }
    }

    public void endFrame() {
        if(skipRendering)
            return;

        mainPass.end(currentCmdBuffer);

        submitFrame();
    }

    public void endRenderPass() {
        this.boundRenderPass.endRenderPass(currentCmdBuffer);
        this.boundRenderPass = null;
    }

    //TODO
    public void beginRendering(Framebuffer framebuffer) {
        if(skipRendering) 
            return;

        if(this.boundFramebuffer != framebuffer) {
            this.endRendering();

            try (MemoryStack stack = stackPush()) {
                //TODO
//                framebuffer.beginRenderPass(currentCmdBuffer, stack);
            }

            this.boundFramebuffer = framebuffer;
        }
    }

    public void endRendering() {
        if(skipRendering) 
            return;
        
        this.boundRenderPass.endRenderPass(currentCmdBuffer);

        this.boundFramebuffer = null;
        this.boundRenderPass = null;
    }

    public void setBoundFramebuffer(Framebuffer framebuffer) {
        this.boundFramebuffer = framebuffer;
    }

    public void resetBuffers() {
        drawer.resetBuffers(currentFrame);

        Vulkan.getStagingBuffer(currentFrame).reset();
    }

    public void addUsedPipeline(Pipeline pipeline) {
        usedPipelines.add(pipeline);
    }

    public void removeUsedPipeline(Pipeline pipeline) { usedPipelines.remove(pipeline); }

    private void resetDescriptors() {
        for(Pipeline pipeline : usedPipelines) {
            pipeline.resetDescriptorPool(currentFrame);
        }

        usedPipelines.clear();
    }

    private void submitFrame() {
        if(swapCahinUpdate)
            return;

        try(MemoryStack stack = stackPush()) {

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(device, Vulkan.getSwapChain().getId(), VUtil.UINT64_MAX,
                    imageAvailableSemaphores.get(currentFrame), VK_NULL_HANDLE, pImageIndex);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || swapCahinUpdate) {
                swapCahinUpdate = true;
//                shouldRecreate = false;
//                waitForSwapChain();
//                recreateSwapChain();
//                shouldRecreate = true;
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image: " + vkResult);
            }

            final int imageIndex = pImageIndex.get(0);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stackGet().longs(imageAvailableSemaphores.get(currentFrame)));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(stackGet().longs(renderFinishedSemaphores.get(currentFrame)));

            submitInfo.pCommandBuffers(stack.pointers(currentCmdBuffer));

            vkResetFences(device, stackGet().longs(inFlightFences.get(currentFrame)));

            Synchronization.INSTANCE.waitFences();

            if((vkResult = vkQueueSubmit(getGraphicsQueue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, stackGet().longs(inFlightFences.get(currentFrame)));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(stackGet().longs(renderFinishedSemaphores.get(currentFrame)));

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(Vulkan.getSwapChain().getId()));

            presentInfo.pImageIndices(pImageIndex);

            vkResult = vkQueuePresentKHR(getPresentQueue(), presentInfo);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || swapCahinUpdate) {
                swapCahinUpdate = true;
//                shouldRecreate = false;
//                recreateSwapChain();
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

            currentFrame = (currentFrame + 1) % framesNum;
        }
    }

    void waitForSwapChain()
    {
        vkResetFences(device, inFlightFences.get(currentFrame));

//        constexpr VkPipelineStageFlags t=VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            //Empty Submit
            VkSubmitInfo info = VkSubmitInfo.calloc(stack)
                    .sType$Default()
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphores.get(currentFrame)))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT));

            vkQueueSubmit(getGraphicsQueue(), info, inFlightFences.get(currentFrame));
            vkWaitForFences(device, inFlightFences.get(currentFrame),  true, -1);
        }
    }

    private void recreateSwapChain() {
//        for(Long fence : inFlightFences) {
//            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
//        }

//        waitForSwapChain();
        Vulkan.waitIdle();

//        for(int i = 0; i < getSwapChainImages().size(); ++i) {
//            vkDestroyFence(device, inFlightFences.get(i), null);
//            vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
//            vkDestroySemaphore(device, renderFinishedSemaphores.get(i), null);
//        }

        commandBuffers.forEach(commandBuffer -> vkResetCommandBuffer(commandBuffer, 0));

        Vulkan.recreateSwapChain();

        int newFramesNum = getSwapChain().getFramesNum();

        if(framesNum != newFramesNum) {
            AreaUploadManager.INSTANCE.waitAllUploads();
            destroySyncObjects();

            framesNum = newFramesNum;
            createSyncObjects();
            allocateCommandBuffers();

            Pipeline.recreateDescriptorSets(framesNum);

            drawer.createResources(framesNum);
            AreaUploadManager.INSTANCE.createLists(framesNum);
        }

        this.onResizeCallbacks.forEach(Runnable::run);

        currentFrame = 0;
    }

    public void cleanUpResources() {
        destroySyncObjects();

        drawer.cleanUpResources();

        ShaderManager.getInstance().destroyPipelines();
        VTextureSelector.getWhiteTexture().free();
    }

    private void destroySyncObjects() {
        for (int i = 0; i < framesNum; ++i) {
            vkDestroyFence(device, inFlightFences.get(i), null);
            vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
            vkDestroySemaphore(device, renderFinishedSemaphores.get(i), null);
        }
    }

    public void setBoundRenderPass(RenderPass boundRenderPass) {
        this.boundRenderPass = boundRenderPass;
    }

    public RenderPass getBoundRenderPass() {
        return boundRenderPass;
    }

    public void setMainPass(MainPass mainPass) { this.mainPass = mainPass; }

    public void addOnResizeCallback(Runnable runnable) {
        this.onResizeCallbacks.add(runnable);
    }

    public void bindPipeline(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

//        PipelineState currentState = new PipelineState(currentBlendState, currentDepthState, currentLogicOpState, currentColorMask, boundRenderPass);
        PipelineState currentState = PipelineState.getCurrentPipelineState(boundRenderPass);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle(currentState));

        addUsedPipeline(pipeline);
    }

    public void uploadAndBindUBOs(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;
        pipeline.bindDescriptorSets(commandBuffer, currentFrame);
    }

    public void pushConstants(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        PushConstants pushConstants = pipeline.getPushConstants();

        try (MemoryStack stack = stackPush()) {
            ByteBuffer buffer = stack.malloc(pushConstants.getSize());
            long ptr = MemoryUtil.memAddress0(buffer);
            pushConstants.update(ptr);

            nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants.getSize(), ptr);
        }

    }

    public static void setDepthBias(float units, float factor) {
        VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

        vkCmdSetDepthBias(commandBuffer, units, 0.0f, factor);
    }

    public static void clearAttachments(int v) {
        Framebuffer framebuffer = Renderer.getInstance().boundFramebuffer;
        if(framebuffer == null)
            return;

        clearAttachments(v, framebuffer.getWidth(), framebuffer.getHeight());
    }

    public static void clearAttachments(int v, int width, int height) {
        if(skipRendering)
            return;

        VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

        try(MemoryStack stack = stackPush()) {
            //ClearValues have to be different for each attachment to clear, it seems it works like a buffer: color and depth attributes override themselves
            VkClearValue colorValue = VkClearValue.calloc(stack);
            colorValue.color().float32(VRenderSystem.clearColor);

            VkClearValue depthValue = VkClearValue.calloc(stack);
            depthValue.depthStencil().depth(VRenderSystem.clearDepth);

            int attachmentsCount;
            VkClearAttachment.Buffer pAttachments;
            if (v == 0x100) {
                attachmentsCount = 1;

                pAttachments = VkClearAttachment.calloc(attachmentsCount, stack);

                VkClearAttachment clearDepth = pAttachments.get(0);
                clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                clearDepth.clearValue(depthValue);
            } else if (v == 0x4000) {
                attachmentsCount = 1;

                pAttachments = VkClearAttachment.calloc(attachmentsCount, stack);

                VkClearAttachment clearColor = pAttachments.get(0);
                clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                clearColor.colorAttachment(0);
                clearColor.clearValue(colorValue);
            } else if (v == 0x4100) {
                attachmentsCount = 2;

                pAttachments = VkClearAttachment.calloc(attachmentsCount, stack);

                VkClearAttachment clearColor = pAttachments.get(0);
                clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                clearColor.clearValue(colorValue);

                VkClearAttachment clearDepth = pAttachments.get(1);
                clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                clearDepth.clearValue(depthValue);
            } else {
                throw new RuntimeException("unexpected value");
            }

            //Rect to clear
            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(VkExtent2D.calloc(stack).set(width, height));

            VkClearRect.Buffer pRect = VkClearRect.calloc(1, stack);
            pRect.get(0).rect(renderArea);
            pRect.get(0).layerCount(1);

            vkCmdClearAttachments(commandBuffer, pAttachments, pRect);
        }
    }

    public static void setViewport(int x, int y, int width, int height) {
        try(MemoryStack stack = stackPush()) {
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(x);
            viewport.y(height + y);
            viewport.width(width);
            viewport.height(-height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
            scissor.offset(VkOffset2D.malloc(stack).set(0, 0));
            scissor.extent(VkExtent2D.malloc(stack).set(width, height));

            vkCmdSetViewport(INSTANCE.currentCmdBuffer, 0, viewport);
            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    public static void setScissor(int x, int y, int width, int height) {
        try(MemoryStack stack = stackPush()) {
            int framebufferHeight = Renderer.getInstance().boundFramebuffer.getHeight();

            VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
            scissor.offset(VkOffset2D.malloc(stack).set(x, framebufferHeight - (y + height)));
            scissor.extent(VkExtent2D.malloc(stack).set(width, height));

            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    public static void resetScissor() {
        try(MemoryStack stack = stackPush()) {
            VkRect2D.Buffer scissor = Renderer.getInstance().boundFramebuffer.scissor(stack);
            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    public static void pushDebugSection(String s) {
        if(Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

            try(MemoryStack stack = stackPush()) {
                VkDebugUtilsLabelEXT markerInfo = VkDebugUtilsLabelEXT.calloc(stack);
                markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                ByteBuffer string = stack.UTF8(s);
                markerInfo.pLabelName(string);
                vkCmdBeginDebugUtilsLabelEXT(commandBuffer, markerInfo);
            }
        }
    }

    public static void popDebugSection() {
        if(Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

            vkCmdEndDebugUtilsLabelEXT(commandBuffer);
        }
    }

    public static void popPushDebugSection(String s) {
        popDebugSection();
        pushDebugSection(s);
    }

    public static int getFramesNum() { return INSTANCE.framesNum; }

    public static VkCommandBuffer getCommandBuffer() { return INSTANCE.currentCmdBuffer; }

    public static void scheduleSwapChainUpdate() { swapCahinUpdate = true; }
}
