package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class TransferQueue extends Queue {
    private static final VkDevice DEVICE = Vulkan.getDevice();

    public static TransferQueue INSTANCE;

    public static void createInstance() {
        INSTANCE = new TransferQueue();
    }

    public static TransferQueue getInstance() {
        return INSTANCE;
    }

    protected TransferQueue() {
        commandPool = new CommandPool(getQueueFamilies().transferFamily);
    }

    public long copyBufferCmd(long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

        try(MemoryStack stack = stackPush()) {

            CommandPool.CommandBuffer commandBuffer = beginCommands();

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.size(size);
            copyRegion.srcOffset(srcOffset);
            copyRegion.dstOffset(dstOffset);

            vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);

            this.submitCommands(commandBuffer);
            Synchronization.INSTANCE.addCommandBuffer(commandBuffer);

            return commandBuffer.fence;
        }
    }

    public void uploadBufferImmediate(long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

        try(MemoryStack stack = stackPush()) {
            CommandPool.CommandBuffer commandBuffer = this.beginCommands();

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.size(size);
            copyRegion.srcOffset(srcOffset);
            copyRegion.dstOffset(dstOffset);

            vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);

            this.submitCommands(commandBuffer);
            vkWaitForFences(DEVICE, commandBuffer.fence, true, VUtil.UINT64_MAX);
            commandBuffer.reset();
        }
    }

    public synchronized long submitCommands(CommandPool.CommandBuffer commandBuffer) {

        return this.commandPool.submitCommands(commandBuffer, Vulkan.getTransferQueue());
    }

    public static void uploadBufferCmd(CommandPool.CommandBuffer commandBuffer, long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

        try(MemoryStack stack = stackPush()) {

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.size(size);
            copyRegion.srcOffset(srcOffset);
            copyRegion.dstOffset(dstOffset);

            vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);
        }
    }

    public static void waitIdle() {
        vkQueueWaitIdle(Vulkan.getTransferQueue());
    }
    
}
