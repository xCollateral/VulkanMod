package net.vulkanmod.vulkan.queue;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public enum Queue {
    GraphicsQueue(QueueFamilyIndices.graphicsFamily, true),
    TransferQueue(QueueFamilyIndices.transferFamily, true),
    PresentQueue(QueueFamilyIndices.presentFamily, false);
    private final int familyIndex;
    private CommandPool.CommandBuffer currentCmdBuffer;
    private final CommandPool commandPool;
    private final VkQueue queue;

    public CommandPool.CommandBuffer beginCommands() {
        return this.commandPool.beginCommands();
    }
    //We don't use the Compute Queue: will skip creating it to save driver resources
    Queue(int familyIndex, boolean initCommandPool) {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            PointerBuffer pQueue = stack.mallocPointer(1);
            this.familyIndex = familyIndex;
            vkGetDeviceQueue(DeviceManager.vkDevice, this.familyIndex, 0, pQueue);
            this.queue = new VkQueue(pQueue.get(0), DeviceManager.vkDevice);

            this.commandPool = initCommandPool ? new CommandPool(this.familyIndex) : null;
        }
    }

    public long submitCommands(CommandPool.CommandBuffer commandBuffer) {
        return this.commandPool.submitCommands(commandBuffer, queue);
    }

    public VkQueue queue() { return this.queue; }

    public void cleanUp() {
        if(commandPool != null)
            commandPool.cleanUp();
    }

    public void waitIdle() {
        vkQueueWaitIdle(queue);
    }


    public long copyBufferCmd(long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

        try(MemoryStack stack = stackPush()) {

            CommandPool.CommandBuffer commandBuffer = beginCommands();

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.malloc(1, stack);
            copyRegion.size(size);
            copyRegion.srcOffset(srcOffset);
            copyRegion.dstOffset(dstOffset);

            vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);

            long a = this.submitCommands(commandBuffer);
            Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
            return a;

        }
    }

    public void uploadBufferImmediate(long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

        try(MemoryStack stack = stackPush()) {
            CommandPool.CommandBuffer commandBuffer = this.beginCommands();

//            if(Initializer.CONFIG.useGigaBarriers) this.GigaBarrier(commandBuffer.getHandle());
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.malloc(1, stack);
            copyRegion.size(size);
            copyRegion.srcOffset(srcOffset);
            copyRegion.dstOffset(dstOffset);
//            this.BufferBarrier(commandBuffer.getHandle(), srcBuffer, ~0,VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT/*|VK_ACCESS_TRANSFER_WRITE_BIT*/,  VK_ACCESS_TRANSFER_READ_BIT, VK_PIPELINE_STAGE_VERTEX_INPUT_BIT/*|VK_PIPELINE_STAGE_TRANSFER_BIT*/, VK_PIPELINE_STAGE_TRANSFER_BIT);
            vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);

            this.submitCommands(commandBuffer);
            vkWaitForFences(DeviceManager.vkDevice, commandBuffer.fence, true, VUtil.UINT64_MAX);
            commandBuffer.reset();
        }
    }

    public void uploadBufferCmd(VkCommandBuffer commandBuffer, long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

        try(MemoryStack stack = stackPush()) {

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.malloc(1, stack);
            copyRegion.size(size);
            copyRegion.srcOffset(srcOffset);
            copyRegion.dstOffset(dstOffset);

            vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);
        }
    }

    public void startRecording() {
        currentCmdBuffer = beginCommands();
    }

    public void endRecordingAndSubmit() {
        long fence = submitCommands(currentCmdBuffer);
        Synchronization.INSTANCE.addCommandBuffer(currentCmdBuffer);

        currentCmdBuffer = null;
    }

    public CommandPool.CommandBuffer getCommandBuffer() {
        return currentCmdBuffer != null ? currentCmdBuffer : beginCommands();
    }

    public long endIfNeeded(CommandPool.CommandBuffer commandBuffer) {
        return currentCmdBuffer != null ? VK_NULL_HANDLE : submitCommands(commandBuffer);
    }

    public void trimCmdPool()
    {
        if(commandPool==null) return;
        VK11.vkTrimCommandPool(DeviceManager.vkDevice, this.commandPool.id, 0);
    }

    public static void trimCmdPools()
    {
        for(var queue : Queue.values()) {
            queue.trimCmdPool();
        }
    }

    public void fillBuffer(long id, int bufferSize, int qNaN) {
        vkCmdFillBuffer(this.getCommandBuffer().getHandle(), id, 0, bufferSize, qNaN);
    }

    public void BufferBarrier(VkCommandBuffer commandBuffer, long bufferhdle, int size_t, int srcAccess, int dstAccess, int srcStage, int dstStage) {

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferMemoryBarrier.Buffer memBarrier = VkBufferMemoryBarrier.calloc(1, stack)
                    .sType$Default()
                    .buffer(bufferhdle)
                    .srcQueueFamilyIndex(this.familyIndex)
                    .dstQueueFamilyIndex(this.familyIndex)
                    .srcAccessMask(srcAccess)
                    .dstAccessMask(dstAccess)
                    .size(size_t);

            vkCmdPipelineBarrier(commandBuffer,
                    srcStage, dstStage,
                    0,
                    null,
                    memBarrier,
                    null);

        }
    }

    public void MemoryBarrier(VkCommandBuffer commandBuffer, int srcAccess, int dstAccess, int srcStage, int dstStage) {

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryBarrier.Buffer memBarrier = VkMemoryBarrier.calloc(1, stack)
                    .sType$Default()
                    .srcAccessMask(srcAccess)
                    .dstAccessMask(dstAccess);

            vkCmdPipelineBarrier(commandBuffer,
                    srcStage, dstStage,
                    0,
                    memBarrier,
                    null,
                    null);

        }
    }


    //Using barrier batching to allow Driver optimisations
    public void MultiBufferBarriers(VkCommandBuffer commandBuffer, LongSet bufferhdles, int srcAccess, int dstAccess, int srcStage, int dstStage) {

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferMemoryBarrier.Buffer memBarriers = VkBufferMemoryBarrier.malloc(bufferhdles.size(), stack);
                    int i = 0;
            for (var a : bufferhdles) {

                memBarriers.get(i).sType$Default()
                    .buffer(a)
                    .pNext(0)
                    .offset(0)
                    .srcQueueFamilyIndex(this.familyIndex)
                    .dstQueueFamilyIndex(this.familyIndex)
                    .srcAccessMask(srcAccess) //Not sure if VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT or VK_ACCESS_INDEX_READ_BIT is Faster
                    .dstAccessMask(dstAccess)
                    .size(~0 /*VK_WHOLE_SIZE*/);
                i++;
            }

            vkCmdPipelineBarrier(commandBuffer,
                    srcStage, dstStage,
                    0,
                    null,
                    memBarriers,
                    null);

        }
    }

    public void GigaBarrier(VkCommandBuffer commandBuffer, int srcStage, int dstStage, boolean flushReads) {

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryBarrier.Buffer memBarrier = VkMemoryBarrier.calloc(1, stack);
            memBarrier.sType$Default();
            memBarrier.srcAccessMask(flushReads ? VK_ACCESS_MEMORY_WRITE_BIT|VK_ACCESS_MEMORY_READ_BIT : 0);
            memBarrier.dstAccessMask(flushReads ? VK_ACCESS_MEMORY_WRITE_BIT|VK_ACCESS_MEMORY_READ_BIT : VK_ACCESS_MEMORY_WRITE_BIT);

            vkCmdPipelineBarrier(commandBuffer,
                    srcStage, dstStage,
                    0,
                    memBarrier,
                    null,
                    null);
        }
    }

    public void updateBuffer(CommandPool.CommandBuffer commandBuffer, long id, int baseOffset, long bufferPtr, int sizeT) {

        nvkCmdUpdateBuffer(commandBuffer.getHandle(), id, baseOffset, sizeT, bufferPtr);

    }
}

