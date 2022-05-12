package net.vulkanmod.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.vulkan.Vulkan.findQueueFamilies;
import static org.lwjgl.system.MemoryStack.stackPush;
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

            Vulkan.QueueFamilyIndices queueFamilyIndices = findQueueFamilies(device.getPhysicalDevice());

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }

            commandPool = pCommandPool.get(0);
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
        if (currentCmdBuffer != null) {
            return currentCmdBuffer;
        } else {
            return beginCommands();
        }
    }

    public static long endIfNeeded(CommandBuffer commandBuffer) {
        if (currentCmdBuffer != null) {
            return -1;
        } else {
            return endCommands(commandBuffer);
        }
    }

    public synchronized static CommandBuffer beginCommands() {

        try(MemoryStack stack = stackPush()) {
            final int size = 1;

            CommandBuffer commandBuffer = new CommandBuffer();

            if(current >= commandBuffers.size()) {

                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandPool(commandPool);
                allocInfo.commandBufferCount(size);

                PointerBuffer pCommandBuffer = stack.mallocPointer(size);
                vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);

                for(int i = 0; i < size; ++i) {
                    commandBuffer.handle = new VkCommandBuffer(pCommandBuffer.get(i), device);
                    commandBuffers.add(commandBuffer);
                }


                VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
                fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
                fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

                LongBuffer pFence = stack.mallocLong(size);
                vkCreateFence(device, fenceInfo, null, pFence);

                for(int i = 0; i < size; ++i) {
                    fences.add(pFence.get(0));
                    commandBuffer.fence = pFence.get(0);
                }

            }

            commandBuffer = commandBuffers.get(current);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

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

            VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.callocStack(1, stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer.handle));

            vkQueueSubmit(Vulkan.getGraphicsQueue(), submitInfo, fence);
            //vkQueueWaitIdle(graphicsQueue);

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
