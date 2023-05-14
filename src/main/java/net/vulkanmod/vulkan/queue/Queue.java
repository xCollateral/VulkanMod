package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Queue {
    private static VkDevice DEVICE;

    private static QueueFamilyIndices queueFamilyIndices;
    protected CommandPool commandPool;

    public synchronized CommandPool.CommandBuffer beginCommands() {

        return this.commandPool.beginCommands();
    }

    public abstract long submitCommands(CommandPool.CommandBuffer commandBuffer);

    public void cleanUp() {
        commandPool.cleanUp();
    }

    public enum Family {
        Graphics,
        Transfer,
        Compute
    }

    public static void initQueues() {
        GraphicsQueue.createInstance();
        TransferQueue.createInstance();
    }

    public static QueueFamilyIndices getQueueFamilies() {
        if(DEVICE == null)
            DEVICE = Vulkan.getDevice();

        if(queueFamilyIndices == null) {
            queueFamilyIndices = findQueueFamilies(DEVICE.getPhysicalDevice());
        }
        return queueFamilyIndices;
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {

        QueueFamilyIndices indices = new QueueFamilyIndices();

        try(MemoryStack stack = stackPush()) {

            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);

//            for(int i = 0; i < queueFamilies.capacity() || !indices.isComplete();i++) {
            for(int i = 0; i < queueFamilies.capacity(); i++) {
                int queueFlags = queueFamilies.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;

                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if(presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = i;
                    }
                } else if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    indices.computeFamily = i;
                } else if ((queueFlags & (VK_QUEUE_COMPUTE_BIT | VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    indices.transferFamily = i;
                }

                if(indices.presentFamily == null) {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if(presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = i;
                    }
                }

                if(indices.isComplete()) break;
            }

            if(indices.transferFamily == null) {

                int fallback = -1;
                for(int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                        if(fallback == -1)
                            fallback = i;

                        if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0) {
                            indices.transferFamily = i;

                            if(i != indices.computeFamily)
                                break;
                            fallback = i;
                        }
                    }

                    if(fallback == -1)
                        throw new RuntimeException("Failed to find queue family with transfer support");

                    indices.transferFamily = fallback;
                }
            }
            
            if(indices.computeFamily == null) {
                for(int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        indices.computeFamily = i;
                        break;
                    }
                }
            }

            if (indices.graphicsFamily == null)
                throw new RuntimeException("Unable to find queue family with graphics support.");
            if (indices.presentFamily == null)
                throw new RuntimeException("Unable to find queue family with present support.");
            if (indices.computeFamily == null)
                throw new RuntimeException("Unable to find queue family with compute support.");

            return indices;
        }
    }

    public static class QueueFamilyIndices {

        // We use Integer to use null as the empty value
        public Integer graphicsFamily;
        public Integer presentFamily;
        public Integer transferFamily;
        public Integer computeFamily;

        public boolean isComplete() {
            return graphicsFamily != null && presentFamily != null && transferFamily != null && computeFamily != null;
        }

        public boolean isSuitable() {
            return graphicsFamily != null && presentFamily != null;
        }

        public int[] unique() {
            return IntStream.of(graphicsFamily, presentFamily, transferFamily, computeFamily).distinct().toArray();
        }

        public int[] array() {
            return new int[] {graphicsFamily, presentFamily};
        }
    }
}
