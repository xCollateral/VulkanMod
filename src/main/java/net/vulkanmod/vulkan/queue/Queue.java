package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Queue {
    private static VkDevice DEVICE;

    private static QueueFamilyIndices queueFamilyIndices;
    protected CommandPool commandPool;

    private final VkQueue queue;

    public synchronized CommandPool.CommandBuffer beginCommands() {
        return this.commandPool.beginCommands();
    }

    Queue(MemoryStack stack, int familyIndex) {
        this(stack, familyIndex, true);
    }

    Queue(MemoryStack stack, int familyIndex, boolean initCommandPool) {
        PointerBuffer pQueue = stack.mallocPointer(1);
        vkGetDeviceQueue(DeviceManager.vkDevice, familyIndex, 0, pQueue);
        this.queue = new VkQueue(pQueue.get(0), DeviceManager.vkDevice);

        if (initCommandPool)
            this.commandPool = new CommandPool(familyIndex);
    }

    public synchronized long submitCommands(CommandPool.CommandBuffer commandBuffer) {
        return this.commandPool.submitCommands(commandBuffer, queue);
    }

    public VkQueue queue() {
        return this.queue;
    }

    public void cleanUp() {
        if (commandPool != null)
            commandPool.cleanUp();
    }

    public void waitIdle() {
        vkQueueWaitIdle(queue);
    }

    public enum Family {
        Graphics,
        Transfer,
        Compute
    }

    public static QueueFamilyIndices getQueueFamilies() {
        if (DEVICE == null)
            DEVICE = Vulkan.getVkDevice();

        if (queueFamilyIndices == null) {
            queueFamilyIndices = findQueueFamilies(DEVICE.getPhysicalDevice());
        }
        return queueFamilyIndices;
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {

        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = stackPush()) {

            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);

//            for(int i = 0; i < queueFamilies.capacity() || !indices.isComplete();i++) {
            for (int i = 0; i < queueFamilies.capacity(); i++) {
                int queueFlags = queueFamilies.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;

                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if (presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = i;
                    }
                } else if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    indices.computeFamily = i;
                } else if ((queueFlags & (VK_QUEUE_COMPUTE_BIT | VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    indices.transferFamily = i;
                }

                if (indices.presentFamily == null) {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if (presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = i;
                    }
                }

                if (indices.isComplete()) break;
            }

            if (indices.transferFamily == null) {

                int fallback = -1;
                for (int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if ((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                        if (fallback == -1)
                            fallback = i;

                        if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0) {
                            indices.transferFamily = i;

                            if (i != indices.computeFamily)
                                break;
                            fallback = i;
                        }
                    }

                    if (fallback == -1)
                        throw new RuntimeException("Failed to find queue family with transfer support");

                    indices.transferFamily = fallback;
                }
            }

            if (indices.computeFamily == null) {
                for (int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if ((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
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
            return new int[]{graphicsFamily, presentFamily};
        }
    }
}
