package net.vulkanmod.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Matrix4f;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.shader.PushConstant;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Drawer {
    private static Drawer INSTANCE = new Drawer();

    private static VkDevice device;
    private static List<VkCommandBuffer> commandBuffers;

    private static Set<Pipeline> usedPipelines = new HashSet<>();

    private VertexBuffer[] vertexBuffers;
    private AutoIndexBuffer quadsIndexBuffer;
    private AutoIndexBuffer triangleFanIndexBuffer;
    private AutoIndexBuffer triangleStripIndexBuffer;
    private UniformBuffers uniformBuffers;

    private static int MAX_FRAMES_IN_FLIGHT;
    private ArrayList<Long> imageAvailableSemaphores;
    private ArrayList<Long> renderFinishedSemaphores;
    private ArrayList<Long> inFlightFences;

    private static int currentFrame = 0;
    private final int commandBuffersCount = getSwapChainFramebuffers().size();
    private boolean[] activeCommandBuffers = new boolean[getSwapChainFramebuffers().size()];

    private static int currentIndex = 0;

    public static Pipeline.BlendState currentBlendState = Pipeline.DEFAULT_BLEND_STATE;
    public static Pipeline.DepthState currentDepthState = Pipeline.DEFAULT_DEPTH_STATE;
    public static Pipeline.LogicOpState currentLogicOpState = Pipeline.DEFAULT_LOGICOP_STATE;
    public static Pipeline.ColorMask currentColorMask = Pipeline.DEFAULT_COLORMASK;

    private static Matrix4f projectionMatrix = new Matrix4f();
    private static Matrix4f modelViewMatrix = new Matrix4f();

    public static boolean shouldRecreate = false;
    public static boolean rebuild = false;
    public static boolean skipRendering = false;

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

        for(boolean bool : activeCommandBuffers) bool = false;

        this.allocateCommandBuffers();
    }

    public void draw(ByteBuffer buffer, int drawMode, VertexFormat vertexFormat, int vertexCount)
    {
        assertCommandBufferState();

        if(!(vertexCount > 0)) return;

        AutoIndexBuffer autoIndexBuffer;
        switch (drawMode) {
            case 7 -> autoIndexBuffer = this.quadsIndexBuffer;
            case 6 -> autoIndexBuffer = this.triangleFanIndexBuffer;
            case 5 -> autoIndexBuffer = this.triangleStripIndexBuffer;
            default -> throw new RuntimeException("unknown drawType");
        }

        autoIndexBuffer.checkCapacity(vertexCount);

        drawAutoIndexed(buffer, this.vertexBuffers[currentFrame], autoIndexBuffer.getIndexBuffer(), drawMode, vertexFormat, vertexCount);
        currentIndex++;

    }

    public void draw(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount, int drawMode)
    {
        assertCommandBufferState();

        if(!(indexCount > 0)) return;

        draw(vertexBuffer, indexBuffer, indexCount);
        currentIndex++;

    }

    public void submitDraw()
    {
        if(skipRendering) return;

        drawFrame();
        //TODO
        //if(rebuild) rebuildInstance();
    }

    public void initiateRenderPass() {

        try (MemoryStack stack = stackPush()) {

            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);

            glfwGetFramebufferSize(window, width, height);
            if (width.get(0) == 0 && height.get(0) == 0) {
                skipRendering = true;
                MinecraftClient.getInstance().skipGameRender = true;
            } else {
                skipRendering = false;
                MinecraftClient.getInstance().skipGameRender = false;
            }
        }

        if(skipRendering) return;

        vkWaitForFences(device, inFlightFences.get(currentFrame), true, VUtil.UINT64_MAX);

        CompletableFuture.runAsync(MemoryManager::freeBuffers);
//        MemoryManager.freeBuffers();

        resetDescriptors();

        vkResetCommandBuffer(commandBuffers.get(currentFrame), 0);

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            renderPassInfo.renderPass(getRenderPass());

            VkRect2D renderArea = VkRect2D.callocStack(stack);
            renderArea.offset(VkOffset2D.callocStack(stack).set(0, 0));
            renderArea.extent(getSwapchainExtent());
            renderPassInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.callocStack(2, stack);
            clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
            clearValues.get(1).depthStencil().set(1.0f, 0);
            renderPassInfo.pClearValues(clearValues);

            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer:" + err);
            }

            renderPassInfo.framebuffer(getSwapChainFramebuffers().get(currentFrame));

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            VkViewport.Buffer pViewport = Vulkan.viewport(stack);
            vkCmdSetViewport(commandBuffer, 0, pViewport);

            VkRect2D.Buffer pScissor = Vulkan.scissor(stack);
            vkCmdSetScissor(commandBuffer, 0, pScissor);

            vkCmdSetDepthBias(commandBuffer, 0.0F, 0.0F, 0.0F);

            activeCommandBuffers[currentFrame] = true;
        }
    }

    public void endRenderPass() {
        if(skipRendering) return;

        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        vkCmdEndRenderPass(commandBuffer);

        int result = vkEndCommandBuffer(commandBuffer);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }

        activeCommandBuffers[currentFrame] = false;
        currentIndex = 0;

        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers.reset();
        Vulkan.getStagingBuffer(currentFrame).reset();
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

    private static void resetDescriptors() {
        for(Pipeline pipeline : usedPipelines) {
            pipeline.resetDescriptorPool(currentFrame);
        }

        usedPipelines.clear();
    }

    private void assertCommandBufferState() {
        if(!activeCommandBuffers[currentFrame]) throw new RuntimeException("CommandBuffer not active.");
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

            int vkResult = vkAcquireNextImageKHR(device, Vulkan.getSwapChain(), VUtil.UINT64_MAX,
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

            submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex)));

            vkResetFences(device, stackGet().longs(inFlightFences.get(currentFrame)));

            Synchronization.waitFences();

            if((vkResult = vkQueueSubmit(getGraphicsQueue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, stackGet().longs(inFlightFences.get(currentFrame)));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(stackGet().longs(renderFinishedSemaphores.get(currentFrame)));

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(getSwapChain()));

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

        createSyncObjects();

        Vulkan.recreateSwapChain();
        currentFrame = 0;
    }

    public void rebuildInstance() {
        vkDeviceWaitIdle(device);

        Vulkan.recreateSwapChain();
        INSTANCE = new Drawer();
    }

    public static void setProjectionMatrix(Matrix4f mat4) {
        projectionMatrix = mat4;
    }

    public static void setModelViewMatrix(Matrix4f mat4) {
        modelViewMatrix = mat4;
    }

    public static Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public static Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }

    public AutoIndexBuffer getQuadsIndexBuffer() {
        return quadsIndexBuffer;
    }

    public AutoIndexBuffer getTriangleFanIndexBuffer() {
        return triangleFanIndexBuffer;
    }

    private void draw(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {

        Pipeline boundPipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
        usedPipelines.add(boundPipeline);
        bindPipeline(boundPipeline);

        uploadAndBindUBOs(boundPipeline);

        drawIndexed(vertexBuffer, indexBuffer, indexCount);
    }

    private void drawAutoIndexed(ByteBuffer buffer, VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int drawMode, VertexFormat vertexFormat, int vertexCount) {
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        Pipeline boundPipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
        usedPipelines.add(boundPipeline);
        bindPipeline(boundPipeline);

        uploadAndBindUBOs(boundPipeline);

        int indexCount;
        if(drawMode == 7) indexCount = vertexCount * 3 / 2;
        else if(drawMode == 6 || drawMode == 5) indexCount = (vertexCount - 2) * 3;
        else throw new RuntimeException("unknown drawMode: " + drawMode);

        drawIndexed(vertexBuffer, indexBuffer, indexCount);
    }

    public void uploadAndBindUBOs(Pipeline pipeline) {

        try(MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            //long uniformBufferOffset = this.uniformBuffers.getUsedBytes();
            long descriptorSet = pipeline.createDescriptorSets(commandBuffer, currentFrame, uniformBuffers);

            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipeline.getLayout() , 0, stack.longs(descriptorSet), null);
        }
    }

    public void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {

        try(MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            LongBuffer vertexBuffers = stack.longs(vertexBuffer.getId());
            LongBuffer offsets = stack.longs(vertexBuffer.getOffset());
//            Profiler.Push("bindVertex");
            vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

            vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
//            Profiler.Push("draw");
            vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
        }
    }

    public void bindPipeline(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        currentDepthState = VRenderSystem.getDepthState();
        currentColorMask = new Pipeline.ColorMask(VRenderSystem.getColorMask());
        Pipeline.PipelineState currentState = new Pipeline.PipelineState(currentBlendState, currentDepthState, currentLogicOpState, currentColorMask);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle(currentState));

        usedPipelines.add(pipeline);
    }

    public void pushConstants(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        PushConstant pushConstant = pipeline.getPushConstant();
        pushConstant.update();
        vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstant.getBuffer());
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

    public static void pushDebugSection(String s) {
//        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
//
//        try(MemoryStack stack = stackPush()) {
//            VkDebugUtilsLabelEXT markerInfo = VkDebugUtilsLabelEXT.callocStack(stack);
//            markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
//            ByteBuffer string = stack.UTF8(s);
//            markerInfo.pLabelName(string);
//            vkCmdBeginDebugUtilsLabelEXT(commandBuffer, markerInfo);
//        }
    }

    public static void popDebugSection() {
//        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
//
//        vkCmdEndDebugUtilsLabelEXT(commandBuffer);
    }

    public static void popPushDebugSection(String s) {
        popDebugSection();
        pushDebugSection(s);
    }
}
