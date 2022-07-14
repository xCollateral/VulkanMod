package net.vulkanmod.vulkan;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.vulkan.Vulkan.findQueueFamilies;
import static net.vulkanmod.vulkan.Vulkan.getTransferQueue;
import static net.vulkanmod.vulkan.memory.MemoryManager.doPointerAllocSafe3;
import static net.vulkanmod.vulkan.memory.MemoryManager.getPointerBuffer;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.callPPPPI;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class TransferQueue {
    private static final VkDevice device = Vulkan.getDevice();

    private static long commandPool;
    private static final List<CommandBuffer> commandBuffers = new ArrayList<>();
    private static final List<Long> fences = new ArrayList<>();
    private static int current = 0;

    private static CommandBuffer currentCmdBuffer;

    static {
        createCommandPool();
    }

    private static void createCommandPool() {

        try(MemoryStack stack = stackPush()) {

            findQueueFamilies(device.getPhysicalDevice());

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack)
                    .sType$Default()
                    .queueFamilyIndex(Vulkan.QueueFamilyIndices.transferFamily)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

//            LongBuffer pCommandPool = stack.mallocLong(1);
//
//            callPPPPI(device.address(), poolInfo.address(), NULL, memAddress0(pCommandPool), device.getCapabilities().vkCreateCommandPool);

            commandPool = doPointerAllocSafe3(poolInfo, device.getCapabilities().vkCreateCommandPool);
        }
    }

    public static void startRecording() {
        currentCmdBuffer = beginCommands();
    }

    public static void endRecording() {
        long fence = endCommands(currentCmdBuffer);
        Synchronization.addFence(fence);
        currentCmdBuffer = null;
    }

    public static CommandBuffer getCommandBuffer() {
        return currentCmdBuffer != null ? currentCmdBuffer : beginCommands();
    }

    public static long endIfNeeded(CommandBuffer commandBuffer) {
        return currentCmdBuffer != null ? -1 : endCommands(commandBuffer);
    }

    public synchronized static CommandBuffer beginCommands() {

        try(MemoryStack stack = stackPush()) {
            final int size = 1;

            CommandBuffer commandBuffer = new CommandBuffer();

            if(current >= commandBuffers.size()) {

                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack)
                        .sType$Default()
                        .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                        .commandPool(commandPool)
                        .commandBufferCount(size);

                PointerBuffer pCommandBuffer = stack.mallocPointer(size);
                vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);

                for(int i = 0; i < size; ++i) {
                    commandBuffer.handle = new VkCommandBuffer(pCommandBuffer.get(i), device);
                    commandBuffers.add(commandBuffer);
                }


                VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack)
                        .sType$Default()
                        .flags(VK_FENCE_CREATE_SIGNALED_BIT);

                long pFence = getPointerBuffer(fenceInfo);

                for(int i = 0; i < size; ++i) {
                    fences.add(pFence);
                    commandBuffer.fence = pFence;
                }

            }

            commandBuffer = commandBuffers.get(current);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack)
                    .sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer.handle, beginInfo);

            current++;

            return commandBuffer;
        }
    }

    public synchronized static long endCommands(CommandBuffer commandBuffer) {

        try(MemoryStack stack = stackPush()) {
            long fence = commandBuffer.fence;

            vkEndCommandBuffer(commandBuffer.handle);

            vkResetFences(device, commandBuffer.fence);

            VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.callocStack(1, stack)
                    .sType$Default()
                    .pCommandBuffers(stack.pointers(commandBuffer.handle));

            vkQueueSubmit(Vulkan.getTransferQueue(), submitInfo, fence);
//            vkQueueWaitIdle(getTransferQueue());

            //vkFreeCommandBuffers(device, commandPool, commandBuffer);
            return fence;
        }
    }

    public static long getCommandPool() {
        return commandPool;
    }

    public void resetCommandPool() {
        vkResetCommandPool(device, commandPool, 0);
    }

    public static void resetCurrent() {
        current = 0;
    }

    public static void cleanUp() {
        for(long fence : fences) {
            vkDestroyFence(device, fence, null);
        }
    }

    public static class CommandBuffer {
        VkCommandBuffer handle;
        long fence;

        public VkCommandBuffer getHandle() {
            return handle;
        }

        public long getFence() {
            return fence;
        }
    }
}
