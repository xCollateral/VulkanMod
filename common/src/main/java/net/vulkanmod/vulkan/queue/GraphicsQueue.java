package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class GraphicsQueue extends Queue {
    private static final VkDevice DEVICE = Vulkan.getDevice();

    public static GraphicsQueue INSTANCE;

    public static void createInstance() {
        INSTANCE = new GraphicsQueue();
    }

    public static GraphicsQueue getInstance() {
        return INSTANCE;
    }

    private static CommandPool.CommandBuffer currentCmdBuffer;

    protected GraphicsQueue() {
        this.commandPool = new CommandPool(getQueueFamilies().graphicsFamily);
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
        if (currentCmdBuffer != null) {
            return currentCmdBuffer;
        } else {
            return beginCommands();
        }
    }

    public long endIfNeeded(CommandPool.CommandBuffer commandBuffer) {
        if (currentCmdBuffer != null) {
            return VK_NULL_HANDLE;
        } else {
            return submitCommands(commandBuffer);
        }
    }

    public synchronized long submitCommands(CommandPool.CommandBuffer commandBuffer) {

        return this.commandPool.submitCommands(commandBuffer, Vulkan.getGraphicsQueue());
    }

}
