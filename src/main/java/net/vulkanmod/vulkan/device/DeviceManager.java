package net.vulkanmod.vulkan.device;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.queue.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toSet;
import static net.vulkanmod.vulkan.util.VUtil.asPointerBuffer;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public abstract class DeviceManager {
    public static List<Device> availableDevices;
    public static List<Device> suitableDevices;

    public static VkPhysicalDevice physicalDevice;
    public static VkDevice vkDevice;

    public static Device device;

    public static VkPhysicalDeviceProperties deviceProperties;
    public static VkPhysicalDeviceMemoryProperties memoryProperties;

    public static SurfaceProperties surfaceProperties;

    public static void init(VkInstance instance) {
        try {
            DeviceManager.getSuitableDevices(instance);
            DeviceManager.pickPhysicalDevice();
            DeviceManager.createLogicalDevice();
        } catch (Exception e) {
            Initializer.LOGGER.info(getAvailableDevicesInfo());
            throw new RuntimeException(e);
        }
    }

    static List<Device> getAvailableDevices(VkInstance instance) {
        try (MemoryStack stack = stackPush()) {
            List<Device> devices = new ObjectArrayList<>();

            IntBuffer deviceCount = stack.ints(0);

            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if (deviceCount.get(0) == 0) {
                return List.of();
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            VkPhysicalDevice currentDevice;

            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                currentDevice = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

                Device device = new Device(currentDevice);
                devices.add(device);
            }

            return devices;
        }
    }

    static void getSuitableDevices(VkInstance instance) {
        availableDevices = getAvailableDevices(instance);

        List<Device> devices = new ObjectArrayList<>();
        for (Device device : availableDevices) {
            if (isDeviceSuitable(device.physicalDevice)) {
                devices.add(device);
            }
        }

        suitableDevices = devices;
    }

    public static void pickPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {

            int deviceIdx = Initializer.CONFIG.device;
            if (deviceIdx >= 0 && deviceIdx < suitableDevices.size())
                DeviceManager.device = suitableDevices.get(deviceIdx);
            else {
                DeviceManager.device = autoPickDevice();
                Initializer.CONFIG.device = -1;
            }

            QueueFamilyIndices.findQueueFamilies(physicalDevice = DeviceManager.device.physicalDevice);

            // Get device properties
            deviceProperties = device.properties;

            memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);

            surfaceProperties = querySurfaceProperties(physicalDevice, stack);
        }
    }

    static Device autoPickDevice() {
        ArrayList<Device> integratedGPUs = new ArrayList<>();
        ArrayList<Device> otherDevices = new ArrayList<>();

        boolean flag = false;

        Device currentDevice = null;
        for (Device device : suitableDevices) {
            currentDevice = device;

            int deviceType = device.properties.deviceType();
            if (deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                flag = true;
                break;
            } else if (deviceType == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU)
                integratedGPUs.add(device);
            else
                otherDevices.add(device);
        }

        if (!flag) {
            if (!integratedGPUs.isEmpty())
                currentDevice = integratedGPUs.get(0);
            else if (!otherDevices.isEmpty())
                currentDevice = otherDevices.get(0);
            else {
                throw new IllegalStateException("Failed to find a suitable GPU");
            }
        }

        return currentDevice;
    }

    public static void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {

            int[] uniqueQueueFamilies = QueueFamilyIndices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceVulkan11Features deviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack);
            deviceVulkan11Features.sType$Default();
            deviceVulkan11Features.shaderDrawParameters(device.isDrawIndirectSupported());

            VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack);
            deviceFeatures.sType$Default();
            deviceFeatures.features().samplerAnisotropy(device.availableFeatures.features().samplerAnisotropy());
            deviceFeatures.features().logicOp(device.availableFeatures.features().logicOp());
            // TODO: Disable indirect draw option if unsupported.
            deviceFeatures.features().multiDrawIndirect(device.isDrawIndirectSupported());

            // Must not set line width to anything other than 1.0 if this is not supported
            if (device.availableFeatures.features().wideLines()) {
                deviceFeatures.features().wideLines(true);
                VRenderSystem.canSetLineWidth = true;
            }

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures.features());
            createInfo.pNext(deviceVulkan11Features);

            if (Vulkan.DYNAMIC_RENDERING) {
                VkPhysicalDeviceDynamicRenderingFeaturesKHR dynamicRenderingFeaturesKHR = VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack);
                dynamicRenderingFeaturesKHR.sType$Default();
                dynamicRenderingFeaturesKHR.dynamicRendering(true);

                deviceVulkan11Features.pNext(dynamicRenderingFeaturesKHR.address());

//                //Vulkan 1.3 dynamic rendering
//                VkPhysicalDeviceVulkan13Features deviceVulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack);
//                deviceVulkan13Features.sType$Default();
//                if(!deviceInfo.availableFeatures13.dynamicRendering())
//                    throw new RuntimeException("Device does not support dynamic rendering feature.");
//
//                deviceVulkan13Features.dynamicRendering(true);
//                createInfo.pNext(deviceVulkan13Features);
//                deviceVulkan13Features.pNext(deviceVulkan11Features.address());
            }

            createInfo.ppEnabledExtensionNames(asPointerBuffer(Vulkan.REQUIRED_EXTENSION));

//            Configuration.DEBUG_FUNCTIONS.set(true);

            createInfo.ppEnabledLayerNames(Vulkan.ENABLE_VALIDATION_LAYERS ? asPointerBuffer(Vulkan.VALIDATION_LAYERS) : null);

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            int res = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
            Vulkan.checkResult(res, "Failed to create logical device");

            vkDevice = new VkDevice(pDevice.get(0), physicalDevice, createInfo, VK_API_VERSION_1_2);
        }
    }

    private static PointerBuffer getRequiredExtensions() {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if (Vulkan.ENABLE_VALIDATION_LAYERS) {

            MemoryStack stack = stackGet();

            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind();
        }

        return glfwExtensions;
    }

    private static boolean isDeviceSuitable(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {


            VkExtensionProperties.Buffer availableExtensions = getAvailableExtension(stack, device);
            boolean extensionsSupported = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet())
                    .containsAll(Vulkan.REQUIRED_EXTENSION);

            boolean swapChainAdequate = false;

            if (extensionsSupported) {
                SurfaceProperties surfaceProperties = querySurfaceProperties(device, stack);
                swapChainAdequate = surfaceProperties.formats.hasRemaining() && surfaceProperties.presentModes.hasRemaining();
            }

            VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
            vkGetPhysicalDeviceFeatures(device, supportedFeatures);
            boolean anisotropicFilterSupported = supportedFeatures.samplerAnisotropy();

            return extensionsSupported && swapChainAdequate;
        }
    }

    private static VkExtensionProperties.Buffer getAvailableExtension(MemoryStack stack, VkPhysicalDevice device) {
        IntBuffer extensionCount = stack.ints(0);
        vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);

        VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
        vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);

        return availableExtensions;
    }

    // Use the optimal most performant depth format for the specific GPU
    // Nvidia performs best with 24 bit depth, while AMD is most performant with 32-bit float
    public static int findDepthFormat(boolean use24BitsDepthFormat) {
        int[] formats = use24BitsDepthFormat ? new int[]
                {VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT}
                : new int[]{VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT};

        return findSupportedFormat(
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
                formats);
    }

    private static int findSupportedFormat(int tiling, int features, int... formatCandidates) {
        try (MemoryStack stack = stackPush()) {

            VkFormatProperties props = VkFormatProperties.calloc(stack);

            for (int format : formatCandidates) {

                vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);

                if (tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
                    return format;
                } else if (tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
                    return format;
                }

            }
        }

        throw new RuntimeException("Failed to find supported format");
    }

    public static String getAvailableDevicesInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");

        if (availableDevices == null) {
            stringBuilder.append("\tDevice Manager not initialized");
            return stringBuilder.toString();
        }

        if (availableDevices.isEmpty()) {
            stringBuilder.append("\tNo available device found");
        }

        for (Device device : availableDevices) {
            stringBuilder.append("\tDevice: %s\n".formatted(device.deviceName));

            stringBuilder.append("\t\tVulkan Version: %s\n".formatted(device.vkVersion));

            stringBuilder.append("\t\t");
            var unsupportedExtensions = device.getUnsupportedExtensions(Vulkan.REQUIRED_EXTENSION);
            if (unsupportedExtensions.isEmpty()) {
                stringBuilder.append("All required extensions are supported\n");
            } else {
                stringBuilder.append("Unsupported extension: %s\n".formatted(unsupportedExtensions));
            }
        }

        return stringBuilder.toString();
    }

    public static void destroy() {
        Queue.GraphicsQueue.cleanUp();
        Queue.TransferQueue.cleanUp();

        vkDestroyDevice(vkDevice, null);
    }

    public static Queue getGraphicsQueue() {
        return Queue.GraphicsQueue;
    }

    public static Queue getPresentQueue() {
        return Queue.PresentQueue;
    }

    public static Queue getTransferQueue() {
        return Queue.TransferQueue;
    }

    public static SurfaceProperties querySurfaceProperties(VkPhysicalDevice device, MemoryStack stack) {

        long surface = Vulkan.getSurface();
        SurfaceProperties details = new SurfaceProperties();

        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null);

        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes);
        }

        return details;
    }

    public static class SurfaceProperties {
        public VkSurfaceCapabilitiesKHR capabilities;
        public VkSurfaceFormatKHR.Buffer formats;
        public IntBuffer presentModes;
    }

}
