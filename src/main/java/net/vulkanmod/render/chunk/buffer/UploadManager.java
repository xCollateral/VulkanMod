package net.vulkanmod.render.chunk.buffer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.queue.Queue.TransferQueue;
import static org.lwjgl.vulkan.VK10.*;

public class UploadManager {
    public static UploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new UploadManager();
    }

    private CommandPool.CommandBuffer commandBuffer;

    private final LongOpenHashSet dstBuffers = new LongOpenHashSet();

    public void submitUploads() {
        if (this.commandBuffer == null)
            return;

        TransferQueue.submitCommands(this.commandBuffer);

        Synchronization.INSTANCE.addCommandBuffer(this.commandBuffer);

        this.commandBuffer = null;
        this.dstBuffers.clear();
    }

    public void recordUpload(Buffer buffer, int dstOffset, int bufferSize, ByteBuffer src) {
        beginCommands();

        VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        stagingBuffer.copyBuffer(bufferSize, src);

        if (!this.dstBuffers.add(buffer.getId())) {
            //Use BufferBarrier + granular QueueFamilyIndex
            TransferQueue.BufferBarrier(commandBuffer,
                    buffer.getId(),
                    bufferSize, dstOffset,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT);

            this.dstBuffers.clear();
        }

        TransferQueue.uploadBufferCmd(commandBuffer, stagingBuffer.getId(), stagingBuffer.getOffset(), buffer.getId(), dstOffset, bufferSize);
    }

    public void copyBuffer(Buffer src, Buffer dst) {
        copyBuffer(src, 0, dst, 0, src.getBufferSize());
    }

    public void copyBuffer(Buffer src, int srcOffset, Buffer dst, int dstOffset, int size) {
        beginCommands();

        VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();

        TransferQueue.BufferBarrier(commandBuffer,
                src.getId(),
                -1, 0,
                VK_ACCESS_TRANSFER_WRITE_BIT,
                VK_ACCESS_TRANSFER_READ_BIT,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_TRANSFER_BIT);

        this.dstBuffers.add(dst.getId());

        TransferQueue.uploadBufferCmd(commandBuffer, src.getId(), srcOffset, dst.getId(), dstOffset, size);
    }

    public void syncUploads() {
        submitUploads();

        Synchronization.INSTANCE.waitFences();
    }

    private void beginCommands() {
        if (this.commandBuffer == null)
            this.commandBuffer = TransferQueue.beginCommands();
    }

}
