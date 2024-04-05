package net.vulkanmod.render.chunk.buffer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.Queue;
import static net.vulkanmod.vulkan.queue.Queue.TransferQueue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryBarrier;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;

public class UploadManager {
    public static UploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new UploadManager();
    }

    CommandPool.CommandBuffer commandBuffer;

    LongOpenHashSet dstBuffers = new LongOpenHashSet();

    public void submitUploads() {
        if (this.commandBuffer == null)
            return;

        TransferQueue.submitCommands(this.commandBuffer);
    }

    public void recordUpload(long bufferId, long dstOffset, long bufferSize, ByteBuffer src) {
        if (this.commandBuffer == null)
            this.commandBuffer = TransferQueue.beginCommands();

        VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        stagingBuffer.copyBuffer((int) bufferSize, src);

        if (!this.dstBuffers.add(bufferId)) {
            TransferQueue.MemoryBarrier(commandBuffer,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT);

            this.dstBuffers.clear();
        }

        TransferQueue.uploadBufferCmd(commandBuffer, stagingBuffer.getId(), stagingBuffer.getOffset(), bufferId, dstOffset, bufferSize);
    }

    public void copyBuffer(Buffer src, Buffer dst) {
        copyBuffer(src, 0, dst, 0, src.getBufferSize());
    }

    public void copyBuffer(Buffer src, int srcOffset, Buffer dst, int dstOffset, int size) {
        if (this.commandBuffer == null)
            this.commandBuffer = TransferQueue.beginCommands();

        VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();

        TransferQueue.MemoryBarrier(commandBuffer,
                VK_ACCESS_TRANSFER_WRITE_BIT,
                VK_ACCESS_TRANSFER_WRITE_BIT,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_TRANSFER_BIT);

        this.dstBuffers.clear();
        this.dstBuffers.add(dst.getId());

        TransferQueue.uploadBufferCmd(commandBuffer, src.getId(), srcOffset, dst.getId(), dstOffset, size);
    }

    public void waitUploads() {
        if (this.commandBuffer == null)
            return;

        Synchronization.INSTANCE.addCommandBuffer(this.commandBuffer);

        this.commandBuffer = null;
        this.dstBuffers.clear();
    }

}
