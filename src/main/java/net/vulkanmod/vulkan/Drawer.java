package net.vulkanmod.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.chunk.AreaUploadManager;
import net.vulkanmod.render.profiling.Profiler2;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.shader.ShaderManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Drawer {
    private static Drawer INSTANCE;

    public static void initDrawer() { INSTANCE = new Drawer(); }

    private static VkDevice device;
    private static List<VkCommandBuffer> commandBuffers;

    private final Set<Pipeline> usedPipelines = new HashSet<>();

    private VertexBuffer[] vertexBuffers;
    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer triangleStripIndexBuffer;
    private UniformBuffers uniformBuffers;

    private static int MAX_FRAMES_IN_FLIGHT;
    private ArrayList<Long> imageAvailableSemaphores;
    private ArrayList<Long> renderFinishedSemaphores;
    private ArrayList<Long> inFlightFences;

    private Framebuffer boundFramebuffer;

    private static int currentFrame = 0;
    private final int commandBuffersCount = getSwapChainImages().size();

    public static PipelineState.BlendInfo blendInfo = PipelineState.defaultBlendInfo();
    public static PipelineState.BlendState currentBlendState;
    public static PipelineState.DepthState currentDepthState = PipelineState.DEFAULT_DEPTH_STATE;
    public static PipelineState.LogicOpState currentLogicOpState = PipelineState.DEFAULT_LOGICOP_STATE;
    public static PipelineState.ColorMask currentColorMask = PipelineState.DEFAULT_COLORMASK;

    public static boolean shouldRecreate = false;
    public static boolean skipRendering = false;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);


    public Drawer()
    {
        this(2000000, 200000);
    }

    public Drawer(int VBOSize, int UBOSize) {
        device = Vulkan.getDevice();
        MAX_FRAMES_IN_FLIGHT = getSwapChainImages().size();
        vertexBuffers = new VertexBuffer[MAX_FRAMES_IN_FLIGHT];
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
            vertexBuffers[i] = new VertexBuffer(VBOSize, MemoryTypes.HOST_MEM);
        }

        uniformBuffers = new UniformBuffers(UBOSize);
        quadsIndexBuffer = new AutoIndexBuffer(100000, AutoIndexBuffer.DrawType.QUADS);
        triangleFanIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        triangleStripIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_STRIP);

        createSyncObjects();

        this.allocateCommandBuffers();

        ShaderManager.initShaderManager();
        AreaUploadManager.createInstance(MAX_FRAMES_IN_FLIGHT);
    }

    public void draw(ByteBuffer buffer, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount)
    {
        if(!(vertexCount > 0)) return;

        AutoIndexBuffer autoIndexBuffer;
        int indexCount;

        VertexBuffer vertexBuffer = this.vertexBuffers[currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        Pipeline pipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
        bindPipeline(pipeline);
        uploadAndBindUBOs(pipeline);

        switch (mode) {
            case QUADS, LINES -> {
                autoIndexBuffer = this.quadsIndexBuffer;
                indexCount = vertexCount * 3 / 2;
            }
            case TRIANGLE_FAN -> {
                autoIndexBuffer = this.triangleFanIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case TRIANGLE_STRIP, LINE_STRIP -> {
                autoIndexBuffer = this.triangleStripIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case TRIANGLES -> {
                draw(vertexBuffer, vertexCount);
                return;
            }
            default -> throw new RuntimeException(String.format("unknown drawMode: %s", mode));
        }

        autoIndexBuffer.checkCapacity(vertexCount);

        drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
    }

    public void submitDraw()
    {
        if(skipRendering) return;

        drawFrame();
    }

    public void initiateRenderPass() {

        Profiler2 p = Profiler2.getMainProfiler();
        p.push("Frame_fence");
        try (MemoryStack stack = stackPush()) {

            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);

            glfwGetFramebufferSize(window, width, height);
            if (width.get(0) == 0 && height.get(0) == 0) {
                skipRendering = true;
                Minecraft.getInstance().noRender = true;
            } else {
                skipRendering = false;
                Minecraft.getInstance().noRender = false;
            }
        }

        if(skipRendering) return;

        vkWaitForFences(device, inFlightFences.get(currentFrame), true, VUtil.UINT64_MAX);

        p.pop();
        p.start();
        p.push("Frame_ops");

        AreaUploadManager.INSTANCE.updateFrame(currentFrame);

        MemoryManager.getInstance().initFrame(currentFrame);

//        this.vertexBuffers[currentFrame].reset();
//        this.uniformBuffers.reset();
//        Vulkan.getStagingBuffer(currentFrame).reset();

        resetDescriptors();

        vkResetCommandBuffer(commandBuffers.get(currentFrame), 0);

        p.pop();

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

//            VkRect2D renderArea = VkRect2D.callocStack(stack);
//            renderArea.offset(VkOffset2D.callocStack(stack).set(0, 0));
//            renderArea.extent(getSwapchainExtent());
//
//            VkClearValue.Buffer clearValues = VkClearValue.callocStack(2, stack);
//            clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
//            clearValues.get(1).depthStencil().set(1.0f, 0);

            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer:" + err);
            }

            //dyn-rendering

            getSwapChain().colorAttachmentLayout(stack, commandBuffer, currentFrame);

            Framebuffer framebuffer = Vulkan.getSwapChain();

            framebuffer.beginRendering(commandBuffer, stack);
            this.boundFramebuffer = framebuffer;

//            renderPassInfo.framebuffer(getSwapChainFramebuffers().get(currentFrame));
//
//            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            VkViewport.Buffer pViewport = framebuffer.viewport(stack);
            vkCmdSetViewport(commandBuffer, 0, pViewport);

            VkRect2D.Buffer pScissor = framebuffer.scissor(stack);
            vkCmdSetScissor(commandBuffer, 0, pScissor);

            vkCmdSetDepthBias(commandBuffer, 0.0F, 0.0F, 0.0F);
        }
    }

    public void endRenderPass() {
        if(skipRendering) return;

        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

//        vkCmdEndRenderPass(commandBuffer);
//        vkCmdEndRendering(commandBuffer);
        KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);
        this.boundFramebuffer = null;

        try(MemoryStack stack = MemoryStack.stackPush()) {
            getSwapChain().presentLayout(stack, commandBuffer, currentFrame);
        }

        int result = vkEndCommandBuffer(commandBuffer);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }
    }

    public void beginRendering(Framebuffer framebuffer) {
        if(skipRendering) return;

        if(this.boundFramebuffer != framebuffer) {
            this.endRendering();

            try (MemoryStack stack = stackPush()) {
                framebuffer.beginRendering(commandBuffers.get(currentFrame), stack);
            }

            this.boundFramebuffer = framebuffer;
        }
    }

    public void endRendering() {
        if(skipRendering) return;

        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

//        vkCmdEndRenderPass(commandBuffer);
//        vkCmdEndRendering(commandBuffer);
        KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);

        this.boundFramebuffer = null;
    }

    private void allocateCommandBuffers() {
        commandBuffers = new ArrayList<>(commandBuffersCount);

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

            if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for (int i = 0; i < commandBuffersCount; i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
            }
        }
    }

    public void resetBuffers() {
        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers.reset();
        Vulkan.getStagingBuffer(currentFrame).reset();
    }

    private void resetDescriptors() {
        for(Pipeline pipeline : usedPipelines) {
            pipeline.resetDescriptorPool(currentFrame);
        }

        usedPipelines.clear();
    }

    private void createSyncObjects() {

        final int frameNum = getSwapChainImages().size();

        imageAvailableSemaphores = new ArrayList<>(frameNum);
        renderFinishedSemaphores = new ArrayList<>(frameNum);
        inFlightFences = new ArrayList<>(frameNum);

        try(MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for(int i = 0;i < frameNum;i++) {

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


    public static Drawer getInstance() { return INSTANCE; }

    public static int getCurrentFrame() { return currentFrame; }

    private void drawFrame() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(device, Vulkan.getSwapChain().getId(), VUtil.UINT64_MAX,
                    imageAvailableSemaphores.get(currentFrame), VK_NULL_HANDLE, pImageIndex);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || shouldRecreate) {
                shouldRecreate = false;
                recreateSwapChain();
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image: " + vkResult);
            }

            final int imageIndex = pImageIndex.get(0);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stackGet().longs(imageAvailableSemaphores.get(currentFrame)));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(stackGet().longs(renderFinishedSemaphores.get(currentFrame)));

            submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(currentFrame)));

            vkResetFences(device, stackGet().longs(inFlightFences.get(currentFrame)));

            Synchronization.INSTANCE.waitFences();

            if((vkResult = vkQueueSubmit(getGraphicsQueue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, stackGet().longs(inFlightFences.get(currentFrame)));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(stackGet().longs(renderFinishedSemaphores.get(currentFrame)));

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(Vulkan.getSwapChain().getId()));

            presentInfo.pImageIndices(pImageIndex);

            vkResult = vkQueuePresentKHR(getPresentQueue(), presentInfo);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || shouldRecreate) {
                shouldRecreate = false;
                recreateSwapChain();
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    private void recreateSwapChain() {
//        for(Long fence : inFlightFences) {
//            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
//        }

        vkDeviceWaitIdle(device);

        for(int i = 0; i < getSwapChainImages().size(); ++i) {
            vkDestroyFence(device, inFlightFences.get(i), null);
            vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
            vkDestroySemaphore(device, renderFinishedSemaphores.get(i), null);
        }

        commandBuffers.forEach(commandBuffer -> vkResetCommandBuffer(commandBuffer, 0));

        Vulkan.recreateSwapChain();

        createSyncObjects();

        if(MAX_FRAMES_IN_FLIGHT != getSwapChainImages().size()) {
            MAX_FRAMES_IN_FLIGHT = getSwapChainImages().size();

            Vulkan.createStagingBuffers();

//            Arrays.stream(this.vertexBuffers).iterator().forEachRemaining(
//                    Buffer::freeBuffer
//            );
            this.vertexBuffers = new VertexBuffer[MAX_FRAMES_IN_FLIGHT];
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
                this.vertexBuffers[i] = new VertexBuffer(2000000, MemoryTypes.HOST_MEM);
            }

//            this.uniformBuffers.free();
            this.uniformBuffers = new UniformBuffers(200000);

            AreaUploadManager.INSTANCE.waitAllUploads();
            AreaUploadManager.createInstance(MAX_FRAMES_IN_FLIGHT);
        }

        currentFrame = 0;
    }

    public void cleanUpResources() {
        MemoryManager memoryManager = MemoryManager.getInstance();

        Buffer buffer;
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
            buffer = this.vertexBuffers[i];
            memoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            buffer = this.uniformBuffers.getUniformBuffer(i);
            memoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            vkDestroyFence(device, inFlightFences.get(i), null);
            vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
            vkDestroySemaphore(device, renderFinishedSemaphores.get(i), null);
        }

        buffer = this.quadsIndexBuffer.getIndexBuffer();
        memoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
        buffer = this.triangleFanIndexBuffer.getIndexBuffer();
        memoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
        buffer = this.triangleStripIndexBuffer.getIndexBuffer();
        memoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

        ShaderManager.getInstance().destroyPipelines();
        VTextureSelector.getWhiteTexture().free();
    }

    public AutoIndexBuffer getQuadsIndexBuffer() {
        return quadsIndexBuffer;
    }

    public AutoIndexBuffer getTriangleFanIndexBuffer() {
        return triangleFanIndexBuffer;
    }

    public UniformBuffers getUniformBuffers() { return this.uniformBuffers; }

    public static VkCommandBuffer getCommandBuffer() { return commandBuffers.get(currentFrame); }

    public void uploadAndBindUBOs(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
        pipeline.bindDescriptorSets(commandBuffer, currentFrame);
    }

    public void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
    }

    public void draw(VertexBuffer vertexBuffer, int vertexCount) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
    }

    public void bindPipeline(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        currentDepthState = VRenderSystem.getDepthState();
        currentColorMask = new PipelineState.ColorMask(VRenderSystem.getColorMask());
        currentBlendState = blendInfo.createBlendState();
        PipelineState currentState = new PipelineState(currentBlendState, currentDepthState, currentLogicOpState, currentColorMask);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle(currentState));

        usedPipelines.add(pipeline);
    }

    public void bindAutoIndexBuffer(VkCommandBuffer commandBuffer, int drawMode) {
        AutoIndexBuffer autoIndexBuffer;
        switch (drawMode) {
            case 7 -> autoIndexBuffer = this.quadsIndexBuffer;
            case 6 -> autoIndexBuffer = this.triangleFanIndexBuffer;
            case 5 -> autoIndexBuffer = this.triangleStripIndexBuffer;
            default -> throw new RuntimeException("unknown drawType");
        }
        IndexBuffer indexBuffer = autoIndexBuffer.getIndexBuffer();

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
    }

    public void pushConstants(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        PushConstants pushConstants = pipeline.getPushConstants();

        try (MemoryStack stack = stackPush()) {
            ByteBuffer buffer = stack.malloc(pushConstants.getSize());
            long ptr = MemoryUtil.memAddress0(buffer);
            pushConstants.update(ptr);

            nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants.getSize(), ptr);
        }

    }

    public AutoIndexBuffer getQuadIndexBuffer() {
        return this.quadsIndexBuffer;
    }

    public static void setDepthBias(float units, float factor) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        vkCmdSetDepthBias(commandBuffer, units, 0.0f, factor);
    }

    public static void clearAttachments(int v) {
        if(skipRendering) return;

        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        try(MemoryStack stack = stackPush()) {
            //ClearValues have to be different for each attachment to clear, it seems it works like a buffer: color and depth attributes override themselves
            VkClearValue colorValue = VkClearValue.callocStack(stack);
            colorValue.color().float32(VRenderSystem.clearColor);

            VkClearValue depthValue = VkClearValue.callocStack(stack);
            depthValue.depthStencil().depth(VRenderSystem.clearDepth);

            int attachmentsCount;
            VkClearAttachment.Buffer pAttachments;
            if (v == 0x100) {
                attachmentsCount = 1;

                pAttachments = VkClearAttachment.callocStack(attachmentsCount, stack);

                VkClearAttachment clearDepth = pAttachments.get(0);
                clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                clearDepth.clearValue(depthValue);
            } else if (v == 0x4000) {
                attachmentsCount = 1;

                pAttachments = VkClearAttachment.callocStack(attachmentsCount, stack);

                VkClearAttachment clearColor = pAttachments.get(0);
                clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                clearColor.colorAttachment(0);
                clearColor.clearValue(colorValue);
            } else if (v == 0x4100) {
                attachmentsCount = 2;

                pAttachments = VkClearAttachment.callocStack(attachmentsCount, stack);

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
            VkRect2D renderArea = VkRect2D.callocStack(stack);
            renderArea.offset(VkOffset2D.callocStack(stack).set(0, 0));
            renderArea.extent(getSwapchainExtent());

            VkClearRect.Buffer pRect = VkClearRect.callocStack(1, stack);
            pRect.get(0).rect(renderArea);
            pRect.get(0).layerCount(1);

            vkCmdClearAttachments(commandBuffer, pAttachments, pRect);
        }
    }

    public static void setViewport(int x, int y, int width, int height) {
        try(MemoryStack stack = stackPush()) {
            VkViewport.Buffer viewport = VkViewport.callocStack(1, stack);
            viewport.x(x);
            viewport.y(height + y);
            viewport.width(width);
            viewport.height(-height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            vkCmdSetViewport(commandBuffers.get(currentFrame), 0, viewport);
        }
    }

    public static void setScissor(int x, int y, int width, int height) {
        try(MemoryStack stack = stackPush()) {
            int framebufferHeight = Drawer.getInstance().boundFramebuffer.height;

            VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
            scissor.offset().set(x, framebufferHeight - (y + height));
            scissor.extent().set(width, height);

            vkCmdSetScissor(commandBuffers.get(currentFrame), 0, scissor);
        }
    }

    public static void resetScissor() {
        try(MemoryStack stack = stackPush()) {
            VkRect2D.Buffer scissor = Drawer.getInstance().boundFramebuffer.scissor(stack);
            vkCmdSetScissor(commandBuffers.get(currentFrame), 0, scissor);
        }
    }

    public static void pushDebugSection(String s) {
        if(Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            try(MemoryStack stack = stackPush()) {
                VkDebugUtilsLabelEXT markerInfo = VkDebugUtilsLabelEXT.callocStack(stack);
                markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                ByteBuffer string = stack.UTF8(s);
                markerInfo.pLabelName(string);
                vkCmdBeginDebugUtilsLabelEXT(commandBuffer, markerInfo);
            }
        }
    }

    public static void popDebugSection() {
        if(Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            vkCmdEndDebugUtilsLabelEXT(commandBuffer);
        }
    }

    public static void popPushDebugSection(String s) {
        popDebugSection();
        pushDebugSection(s);
    }
}
