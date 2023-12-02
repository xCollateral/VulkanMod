package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.queue.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static net.vulkanmod.vulkan.queue.Queue.findQueueFamilies;
import static net.vulkanmod.vulkan.util.VUtil.asPointerBuffer;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class Device {

    public static VkPhysicalDevice physicalDevice;
    public static VkDevice device;

    public static DeviceInfo deviceInfo;

    public static VkPhysicalDeviceProperties deviceProperties;
    public static VkPhysicalDeviceMemoryProperties memoryProperties;

    public static SurfaceProperties surfaceProperties;

    static GraphicsQueue graphicsQueue;
    static PresentQueue presentQueue;
    static TransferQueue transferQueue;
    static ComputeQueue computeQueue;

    static void pickPhysicalDevice(VkInstance instance) {

        try(MemoryStack stack = stackPush()) {


            GPUCandidate[] ppPhysicalDevices = getAvailableGPUs(instance, stack);



            final GPUCandidate currentDevice;


            final int selectedGPU = Math.min(Initializer.CONFIG.selectedGPU, ppPhysicalDevices.length-1);
            if(selectedGPU !=-1)
            {
                currentDevice = ppPhysicalDevices[selectedGPU];
                Initializer.LOGGER.info("User Selection Detected" + selectedGPU);
                Initializer.LOGGER.info("Using Selected GPU Device: "+currentDevice.deviceName());
                Initializer.LOGGER.info("Skipping Suitability Checks");
                if(!isDeviceSuitable(currentDevice))
                {
                    Initializer.LOGGER.error(DeviceInfo.debugString(ppPhysicalDevices, Vulkan.REQUIRED_EXTENSION));
                    throw new RuntimeException("Failed to find a suitable GPU");

                }
            }
            else{


                Initializer.LOGGER.info(ppPhysicalDevices.length + " Devices Detected");
                currentDevice = enumerateGPUCandidates(ppPhysicalDevices);

                if(currentDevice==null) {
                    Initializer.LOGGER.error(DeviceInfo.debugString(ppPhysicalDevices, Vulkan.REQUIRED_EXTENSION));
                    throw new RuntimeException("Failed to find a suitable GPU");
                }
                Initializer.CONFIG.selectedGPU=currentDevice.i();
            }


            Initializer.LOGGER.info("Using GPU Device: "+currentDevice.deviceName());
            physicalDevice = currentDevice.physicalDevice();


            //Get device properties
            deviceProperties = VkPhysicalDeviceProperties.malloc();
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);

            memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);

            surfaceProperties = querySurfaceProperties(physicalDevice, stack);

            deviceInfo = new DeviceInfo(physicalDevice, deviceProperties);
        }
    }


    @Nullable
    private static GPUCandidate getGPUMatchingName(GPUCandidate[] ppPhysicalDevices, String selectedGPU) {
        for (GPUCandidate gpuCandidate1 : ppPhysicalDevices) {
            if (Objects.equals(gpuCandidate1.deviceName(), selectedGPU)) {
                return gpuCandidate1;
            }
        }
        return null;
    }

    private static GPUCandidate getGPUForName(GPUCandidate[] ppPhysicalDevices, String selectedGPU) {
        for (var a : ppPhysicalDevices)
        {
            if(a.deviceName().equals(selectedGPU))
            {
                return a;
            }



        }
        return null;
    }

    private static GPUCandidate enumerateGPUCandidates(GPUCandidate... ppPhysicalDevices) {



        ArrayList<GPUCandidate> dGPUs = new ArrayList<>();
        ArrayList<GPUCandidate> iGPUs = new ArrayList<>();
        ArrayList<GPUCandidate> misc = new ArrayList<>();

        for(var e : ppPhysicalDevices) {


            if(isDeviceSuitable(e)) {

                switch (e.deviceType())
                {
                    case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> dGPUs.add(e);
                    case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU-> iGPUs.add(e);
                    case VK_PHYSICAL_DEVICE_TYPE_OTHER, VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU, VK_PHYSICAL_DEVICE_TYPE_CPU -> misc.add(e);
                    default -> Initializer.LOGGER.error("Device doesn't seem to be an actual GPU, Skipping...");
                }

            }
        }
        final GPUCandidate currentDevice;

        if(!dGPUs.isEmpty()) currentDevice = dGPUs.get(0);
        else if(!iGPUs.isEmpty()) currentDevice = iGPUs.get(0);
        else if(!misc.isEmpty()) currentDevice = misc.get(0);
        else return null;
        return currentDevice;
    }

    @NotNull
    private static IntBuffer getGPUsCount(VkInstance instance, MemoryStack stack) {
        IntBuffer deviceCount = stack.ints(0);

        vkEnumeratePhysicalDevices(instance, deviceCount, null);

        if(deviceCount.get(0) == 0) {
            throw new RuntimeException("Failed to find GPUs with Vulkan support");
        }
        return deviceCount;
    }

    @NotNull
    private static GPUCandidate[] getAvailableGPUs(VkInstance instance, MemoryStack stack) {
        IntBuffer deviceCount = getGPUsCount(instance, stack);

        PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
        vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

        VkPhysicalDeviceProperties.Buffer vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.malloc(deviceCount.get(0), stack);


        var gpuCandidates = new GPUCandidate[deviceCount.get(0)];

        for(int i = 0; i < ppPhysicalDevices.capacity(); i++) {

            var a = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

            VkPhysicalDeviceProperties deviceProperties = vkPhysicalDeviceProperties.get(i);
            vkGetPhysicalDeviceProperties(a, deviceProperties);
            gpuCandidates[i]=new GPUCandidate(a, deviceProperties.deviceNameString(), deviceProperties.deviceType(), i);
        }

        return gpuCandidates;
    }

    static void createLogicalDevice() {

        try(MemoryStack stack = stackPush()) {

            net.vulkanmod.vulkan.queue.Queue.QueueFamilyIndices indices = findQueueFamilies(physicalDevice);

            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack);
            deviceFeatures.sType$Default();

            //TODO indirect draw option disabled in case it is not supported
            if(deviceInfo.availableFeatures.features().samplerAnisotropy())
                deviceFeatures.features().samplerAnisotropy(true);
            if(deviceInfo.availableFeatures.features().logicOp())
                deviceFeatures.features().logicOp(true);

            VkPhysicalDeviceVulkan11Features deviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack);
            deviceVulkan11Features.sType$Default();

            if(deviceInfo.isDrawIndirectSupported()) {
                deviceFeatures.features().multiDrawIndirect(true);
                deviceVulkan11Features.shaderDrawParameters(true);
            }

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            // queueCreateInfoCount is automatically set

            createInfo.pEnabledFeatures(deviceFeatures.features());

            VkPhysicalDeviceDynamicRenderingFeaturesKHR dynamicRenderingFeaturesKHR = VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack);
            dynamicRenderingFeaturesKHR.sType$Default();
            dynamicRenderingFeaturesKHR.dynamicRendering(true);

            createInfo.pNext(deviceVulkan11Features);
            deviceVulkan11Features.pNext(dynamicRenderingFeaturesKHR.address());

            //Vulkan 1.3 dynamic rendering
//            VkPhysicalDeviceVulkan13Features deviceVulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack);
//            deviceVulkan13Features.sType$Default();
//            if(!deviceInfo.availableFeatures13.dynamicRendering())
//                throw new RuntimeException("Device does not support dynamic rendering feature.");
//
//            deviceVulkan13Features.dynamicRendering(true);
//            createInfo.pNext(deviceVulkan13Features);
//            deviceVulkan13Features.pNext(deviceVulkan11Features.address());

            createInfo.ppEnabledExtensionNames(asPointerBuffer(Vulkan.REQUIRED_EXTENSION));

//            Configuration.DEBUG_FUNCTIONS.set(true);

            if(Vulkan.ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(Vulkan.VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo, VK_API_VERSION_1_2);

//            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);
//
//            vkGetDeviceQueue(device, indices.graphicsFamily, 0, pQueue);
//            graphicsQueue = new VkQueue(pQueue.get(0), device);
//
//            vkGetDeviceQueue(device, indices.presentFamily, 0, pQueue);
//            presentQueue = new VkQueue(pQueue.get(0), device);
//
//            vkGetDeviceQueue(device, indices.transferFamily, 0, pQueue);
//            transferQueue = new VkQueue(pQueue.get(0), device);

            graphicsQueue = new GraphicsQueue(stack, indices.graphicsFamily);
            transferQueue = new TransferQueue(stack, indices.transferFamily);
            presentQueue = new PresentQueue(stack, indices.presentFamily);
            computeQueue = new ComputeQueue(stack, indices.computeFamily);

//            GraphicsQueue.createInstance(stack, indices.graphicsFamily);
//            TransferQueue.createInstance(stack, indices.transferFamily);
//            PresentQueue.createInstance(stack, indices.presentFamily);

        }
    }

    private static PointerBuffer getRequiredExtensions() {

        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if(Vulkan.ENABLE_VALIDATION_LAYERS) {

            MemoryStack stack = stackGet();

            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind();
        }

        return glfwExtensions;
    }


    private static boolean isDeviceSuitable(GPUCandidate device) {
        VkPhysicalDevice vkPhysicalDevice = device.physicalDevice();
        boolean extensionsSupported = checkDeviceExtensionSupport(vkPhysicalDevice);

        boolean swapChainAdequate = false;

        if(extensionsSupported) {
            try(MemoryStack stack = stackPush()) {
                SurfaceProperties surfaceProperties = querySurfaceProperties(vkPhysicalDevice, stack);
                swapChainAdequate = surfaceProperties.formats.hasRemaining() && surfaceProperties.presentModes.hasRemaining();
            }
        }

        boolean anisotropicFilterSupported = false;
        try(MemoryStack stack = stackPush()) {
            VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);
            vkGetPhysicalDeviceFeatures(vkPhysicalDevice, supportedFeatures);
            anisotropicFilterSupported = supportedFeatures.samplerAnisotropy();
        }



        boolean hasQueues = findQueueFamilies(vkPhysicalDevice).isSuitable();
        Initializer.LOGGER.info("Checking GPU Device: "+device.deviceName()+"...");
        Initializer.LOGGER.info("Type: "+ device.getDeviceTypeString());
        Initializer.LOGGER.info("   Has Queues: "+hasQueues);
        Initializer.LOGGER.info("   Has Swapchain Functionality: "+extensionsSupported);
        Initializer.LOGGER.info("   Has Presentable Surface Formats: "+swapChainAdequate);
        Initializer.LOGGER.info((hasQueues && extensionsSupported && swapChainAdequate) ? "Device Suitable!" : "Device not Suitable!");

        return hasQueues && extensionsSupported && swapChainAdequate;


    }

    private static boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {

        try(MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

            Set<String> extensions = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet());

            extensions.removeAll(Vulkan.REQUIRED_EXTENSION);

            return availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet())
                    .containsAll(Vulkan.REQUIRED_EXTENSION);
        }
    }
    // Use the optimal most performant depth format for the specific GPU
    // Nvidia performs best with 24 bit depth, while AMD is most performant with 32-bit float
    public static int findDepthFormat() {
        return findSupportedFormat(
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT);
    }

    private static int findSupportedFormat(int tiling, int features, int... formatCandidates) {

        try(MemoryStack stack = stackPush()) {

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

    public static void destroy() {
        graphicsQueue.cleanUp();
        transferQueue.cleanUp();
        computeQueue.cleanUp();

        vkDestroyDevice(device, null);
    }

    public static GraphicsQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public static PresentQueue getPresentQueue() {
        return presentQueue;
    }

    public static TransferQueue getTransferQueue() {
        return transferQueue;
    }

    public static ComputeQueue getComputeQueue() {
        return computeQueue;
    }

    public static SurfaceProperties querySurfaceProperties(VkPhysicalDevice device, MemoryStack stack) {

        long surface = Vulkan.getSurface();
        SurfaceProperties details = new SurfaceProperties();

        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if(count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

        if(count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes);
        }

        return details;
    }

    public static String[] getAvailableGPUNames(VkInstance instance, MemoryStack stack) {
        var a = getAvailableGPUs(instance, stack);

        final String[] aa = new String[a.length];
        for (int j = 0; j < aa.length; j++) {
            aa[j] = a[j].deviceName();
        }
        return aa;
    }

    public static class SurfaceProperties {
        public VkSurfaceCapabilitiesKHR capabilities;
        public VkSurfaceFormatKHR.Buffer formats;
        public IntBuffer presentModes;
    }

}
