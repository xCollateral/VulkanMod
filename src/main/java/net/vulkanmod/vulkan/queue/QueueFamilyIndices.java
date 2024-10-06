package net.vulkanmod.vulkan.queue;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class QueueFamilyIndices {


    public static boolean findQueueFamilies(VkPhysicalDevice device) {

        try (MemoryStack stack = stackPush()) {

            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);



            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            if(queueFamilies.capacity()==1)
            {
                graphicsFamily = transferFamily = presentFamily = 0;
                return true;
            }


            //Instead using VK_QUEUE_COMPUTE_BIT to check for Present Support
            for (int i = 0; i < queueFamilies.capacity(); i++) {
                int queueFlags = queueFamilies.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsFamily = i;


                    if ((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        presentFamily = i;
                    }
                } if ((queueFlags & (VK_QUEUE_COMPUTE_BIT | VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    transferFamily = i;
                }

                if (presentFamily == VK_QUEUE_FAMILY_IGNORED) {

                    if ((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        presentFamily = i;
                    }
                }

                if (isComplete()) break;
            }

            if (transferFamily == VK_QUEUE_FAMILY_IGNORED) {

                int fallback = VK_QUEUE_FAMILY_IGNORED;
                for (int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if ((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                        if (fallback == VK_QUEUE_FAMILY_IGNORED)
                            fallback = i;

                        if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0) {
                            transferFamily = i;


                            fallback = i;
                        }
                    }

                    if (fallback == VK_QUEUE_FAMILY_IGNORED)
                        throw new RuntimeException("Failed to find queue family with transfer support");

                    transferFamily = fallback;
                }
            }



            hasDedicatedTransferQueue = graphicsFamily != transferFamily;


            if (graphicsFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with graphics support.");
            if (presentFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with present support.");

        }
        return true;
    }

    public static boolean hasQueues(VkPhysicalDevice device, MemoryStack stack) {
        IntBuffer queueFamilyCount = stack.ints(0);

        vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
        return queueFamilyCount.get()!=0;
    }

    public static int graphicsFamily = VK_QUEUE_FAMILY_IGNORED, presentFamily = VK_QUEUE_FAMILY_IGNORED, transferFamily = VK_QUEUE_FAMILY_IGNORED;

    public static boolean hasDedicatedTransferQueue = false;

    public static boolean isComplete() {
        return graphicsFamily != VK_QUEUE_FAMILY_IGNORED && presentFamily != VK_QUEUE_FAMILY_IGNORED && transferFamily != VK_QUEUE_FAMILY_IGNORED;
    }

    public static boolean isSuitable() {
        return graphicsFamily != VK_QUEUE_FAMILY_IGNORED && presentFamily != VK_QUEUE_FAMILY_IGNORED;
    }

    public static int[] unique() {
        return IntStream.of(graphicsFamily, presentFamily, transferFamily).distinct().toArray();
    }

    public static int[] array() {
        return new int[]{graphicsFamily, presentFamily};
    }

}
