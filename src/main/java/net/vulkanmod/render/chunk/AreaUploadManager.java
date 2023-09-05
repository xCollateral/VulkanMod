package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.TransferQueue;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.vkWaitForFences;

public class AreaUploadManager {
    public static AreaUploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new AreaUploadManager();
    }

    ObjectArrayList<AreaBuffer.Segment>[] recordedUploads;
    ObjectArrayList<DrawBuffers.ParametersUpdate>[] updatedParameters;
    ObjectArrayList<Runnable>[] frameOps;
    CommandPool.CommandBuffer[] commandBuffers;

    int currentFrame;

    public void createLists(int frames) {
        this.commandBuffers = new CommandPool.CommandBuffer[frames];
        this.recordedUploads = new ObjectArrayList[frames];
        this.updatedParameters = new ObjectArrayList[frames];
        this.frameOps = new ObjectArrayList[frames];

        for (int i = 0; i < frames; i++) {
            this.recordedUploads[i] = new ObjectArrayList<>();
            this.updatedParameters[i] = new ObjectArrayList<>();
            this.frameOps[i] = new ObjectArrayList<>();
        }
    }

    public synchronized void submitUploads() {
        Validate.isTrue(currentFrame == Renderer.getCurrentFrame());

        if(this.recordedUploads[this.currentFrame].isEmpty())
            return;

        TransferQueue.getInstance().submitCommands(this.commandBuffers[currentFrame]);
    }

    public void uploadAsync(AreaBuffer.Segment uploadSegment, long bufferId, long dstOffset, long bufferSize, ByteBuffer src) {
        Validate.isTrue(currentFrame == Renderer.getCurrentFrame());

        if(commandBuffers[currentFrame] == null)
            this.commandBuffers[currentFrame] = TransferQueue.getInstance().beginCommands();
//            this.commandBuffers[currentFrame] = GraphicsQueue.getInstance().beginCommands();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(this.currentFrame);
        stagingBuffer.copyBuffer((int) bufferSize, src);

        TransferQueue.uploadBufferCmd(this.commandBuffers[currentFrame], stagingBuffer.getId(), stagingBuffer.getOffset(), bufferId, dstOffset, bufferSize);

        this.recordedUploads[this.currentFrame].add(uploadSegment);
    }

    public void enqueueParameterUpdate(DrawBuffers.ParametersUpdate parametersUpdate) {
        this.updatedParameters[this.currentFrame].add(parametersUpdate);
    }

    public void enqueueFrameOp(Runnable runnable) {
        this.frameOps[this.currentFrame].add(runnable);
    }

    public void copy(Buffer src, Buffer dst) {
        if(dst.getBufferSize() < src.getBufferSize()) {
            throw new IllegalArgumentException("dst buffer is smaller than src buffer.");
        }

        if(commandBuffers[currentFrame] == null)
            this.commandBuffers[currentFrame] = TransferQueue.getInstance().beginCommands();

        TransferQueue.uploadBufferCmd(this.commandBuffers[currentFrame], src.getId(), 0, dst.getId(), 0, src.getBufferSize());
    }

    public void updateFrame(int frame) {
        this.currentFrame = frame;
        waitUploads(this.currentFrame);
        executeFrameOps(frame);
    }

    private void executeFrameOps(int frame) {
        for(DrawBuffers.ParametersUpdate parametersUpdate : this.updatedParameters[frame]) {
            parametersUpdate.setDrawParameters();
        }

        for(Runnable runnable : this.frameOps[frame]) {
            runnable.run();
        }

        this.updatedParameters[frame].clear();
        this.frameOps[frame].clear();
    }

    private void waitUploads(int frame) {
        CommandPool.CommandBuffer commandBuffer = commandBuffers[frame];
        if(commandBuffer == null)
            return;
        Synchronization.waitFence(commandBuffers[frame].getFence());

        for(AreaBuffer.Segment uploadSegment : this.recordedUploads[frame]) {
            uploadSegment.setReady();
        }

        for(DrawBuffers.ParametersUpdate parametersUpdate : this.updatedParameters[frame]) {
            parametersUpdate.setDrawParameters();
        }

        this.commandBuffers[frame].reset();
        this.commandBuffers[frame] = null;
        this.recordedUploads[frame].clear();
    }

    public synchronized void waitAllUploads() {
        for(int i = 0; i < this.commandBuffers.length; ++i) {
            waitUploads(i);
        }
    }

}
