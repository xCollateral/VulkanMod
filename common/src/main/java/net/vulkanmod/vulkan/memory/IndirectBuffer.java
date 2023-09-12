package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.TransferQueue;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;

public class IndirectBuffer extends Buffer {
    CommandPool.CommandBuffer commandBuffer;

    public IndirectBuffer(int size, MemoryType type) {
        super(VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT, type);
        this.createBuffer(size);
    }

    public void recordCopyCmd(ByteBuffer byteBuffer) {
        int size = byteBuffer.remaining();

        if(size > this.bufferSize - this.usedBytes) {
            resizeBuffer();
        }

        if(this.type.mappable()) {
            this.type.copyToBuffer(this, size, byteBuffer);
        }
        else {
            if(commandBuffer == null)
                commandBuffer = TransferQueue.getInstance().beginCommands();

            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Renderer.getCurrentFrame());
            stagingBuffer.copyBuffer(size, byteBuffer);

            TransferQueue.uploadBufferCmd(commandBuffer, stagingBuffer.id, stagingBuffer.offset, this.getId(), this.getUsedBytes(), size);
        }

        offset = usedBytes;
        usedBytes += size;
    }

    private void resizeBuffer() {
        MemoryManager.getInstance().addToFreeable(this);
        int newSize = this.bufferSize + (this.bufferSize >> 1);
        this.createBuffer(newSize);
        this.usedBytes = 0;
    }

    public void submitUploads() {
        if(commandBuffer == null)
            return;

        TransferQueue.getInstance().submitCommands(commandBuffer);
        Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
        commandBuffer = null;
    }

    //debug
    public ByteBuffer getByteBuffer() {
        return this.data.getByteBuffer(0, this.bufferSize);
    }
}
