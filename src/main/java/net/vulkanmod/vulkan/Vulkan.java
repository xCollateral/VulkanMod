package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffers;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.util.VkResult;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static net.vulkanmod.vulkan.queue.Queue.getQueueFamilies;
import static net.vulkanmod.vulkan.util.VUtil.asPointerBuffer;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class Vulkan {

        public static final boolean ENABLE_VALIDATION_LAYERS = false;
//    public static final boolean ENABLE_VALIDATION_LAYERS = true;

    public static final boolean DYNAMIC_RENDERING = true;

    public static final Set<String> VALIDATION_LAYERS;

    static {
        if (ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
//            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_synchronization2");

        } else {
            // We are not going to use it, so we don't create it
            VALIDATION_LAYERS = null;
        }
    }

    public static final Set<String> REQUIRED_EXTENSION = getRequiredExtensionSet();

    private static Set<String> getRequiredExtensionSet() {
        ArrayList<String> extensions = new ArrayList<>(List.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME));

        if (DYNAMIC_RENDERING) {
            extensions.add(VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME);
        }

        return new HashSet<>(extensions);
    }

    private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {

        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

        String s;
        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            s = "\u001B[31m" + callbackData.pMessageString();

//            System.err.println("Stack dump:");
//            Thread.dumpStack();
        } else {
            s = callbackData.pMessageString();
        }

        System.err.println(s);

        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0)
            System.nanoTime();

        return VK_FALSE;
    }

    private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                    VkAllocationCallbacks allocationCallbacks,
                                                    LongBuffer pDebugMessenger) {

        if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger,
                                                      VkAllocationCallbacks allocationCallbacks) {

        if (vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
        }

    }

    public static VkDevice getVkDevice() {
        return DeviceManager.vkDevice;
    }

    public static long getAllocator() {
        return allocator;
    }

    public static long window;

    private static VkInstance instance;
    private static long debugMessenger;
    private static long surface;

    private static long commandPool;
    private static VkCommandBuffer immediateCmdBuffer;
    private static long immediateFence;

    private static long allocator;

    private static final StagingBuffers stagingBuffers = new StagingBuffers();

    public static boolean use24BitsDepthFormat = true;
    public static boolean surfaceCapabilities2Supported = false;
    private static int DEFAULT_DEPTH_FORMAT = 0;

    public static void initVulkan(long window) {
        createInstance();
        setupDebugMessenger();
        createSurface(window);

        DeviceManager.init(instance);

        createVma();
        MemoryTypes.createMemoryTypes();

        createCommandPool();

        setupDepthFormat();
    }

    static void createStagingBuffers() {
        stagingBuffers.updateFrameCount(Renderer.getFramesNum());
    }

    static void setupDepthFormat() {
        DEFAULT_DEPTH_FORMAT = DeviceManager.findDepthFormat(use24BitsDepthFormat);
    }

    public static void waitIdle() {
        vkDeviceWaitIdle(DeviceManager.vkDevice);
    }

    public static void cleanUp() {
        vkDeviceWaitIdle(DeviceManager.vkDevice);
        vkDestroyCommandPool(DeviceManager.vkDevice, commandPool, null);
        vkDestroyFence(DeviceManager.vkDevice, immediateFence, null);

        Pipeline.destroyPipelineCache();

        Renderer.getInstance().cleanUpResources();

        freeStagingBuffers();

        try {
            MemoryManager.getInstance().freeAllBuffers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        vmaDestroyAllocator(allocator);

        SamplerManager.cleanUp();
        DeviceManager.destroy();
        destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }

    private static void freeStagingBuffers() {
        stagingBuffers.free();
    }

    private static void createInstance() {

        if (ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport()) {
            throw new RuntimeException("Validation requested but not supported");
        }

        try (MemoryStack stack = stackPush()) {
            checkSurfaceCapabilities2Support(stack);

            // Use calloc to initialize the structs with 0s. Otherwise, the program can crash due to random values

            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe("VulkanMod"));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe("VulkanMod Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_2);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(getRequiredInstanceExtensions());

            if (ENABLE_VALIDATION_LAYERS) {

                createInfo.ppEnabledLayerNames(asPointerBuffer(VALIDATION_LAYERS));

                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            // We need to retrieve the pointer of the created instance
            PointerBuffer instancePtr = stack.mallocPointer(1);

            int result = vkCreateInstance(createInfo, null, instancePtr);
            checkResult(result, "Failed to create instance");

            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    static boolean checkValidationLayerSupport() {

        try (MemoryStack stack = stackPush()) {

            IntBuffer layerCount = stack.ints(0);

            vkEnumerateInstanceLayerProperties(layerCount, null);

            VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);

            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

            Set<String> availableLayerNames = availableLayers.stream()
                                                             .map(VkLayerProperties::layerNameString)
                                                             .collect(toSet());

            return availableLayerNames.containsAll(Vulkan.VALIDATION_LAYERS);
        }
    }

    private static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
//        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
//        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT);
        debugCreateInfo.pfnUserCallback(Vulkan::debugCallback);
    }

    private static void setupDebugMessenger() {

        if (!ENABLE_VALIDATION_LAYERS) {
            return;
        }

        try (MemoryStack stack = stackPush()) {

            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);

            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

            checkResult(createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger),
                        "Failed to set up debug messenger");

            debugMessenger = pDebugMessenger.get(0);
        }
    }

    public static void setDebugLabel(MemoryStack stack, int objectType, long handle, String label) {
        if (ENABLE_VALIDATION_LAYERS) {
            VkDebugUtilsObjectNameInfoEXT nameInfo = VkDebugUtilsObjectNameInfoEXT.calloc(stack);
            nameInfo.sType$Default();
            nameInfo.objectType(objectType);
            nameInfo.objectHandle(handle);
            nameInfo.pObjectName(stackUTF8(label));
            EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(Vulkan.getVkDevice(), nameInfo);
        }
    }

    private static void createSurface(long handle) {
        window = handle;

        try (MemoryStack stack = stackPush()) {

            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            checkResult(glfwCreateWindowSurface(instance, window, null, pSurface),
                        "Failed to create window surface");

            surface = pSurface.get(0);
        }
    }

    private static void createVma() {
        try (MemoryStack stack = stackPush()) {

            VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.calloc(stack);
            vulkanFunctions.set(instance, DeviceManager.vkDevice);

            VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.calloc(stack);
            allocatorCreateInfo.physicalDevice(DeviceManager.physicalDevice);
            allocatorCreateInfo.device(DeviceManager.vkDevice);
            allocatorCreateInfo.pVulkanFunctions(vulkanFunctions);
            allocatorCreateInfo.instance(instance);
            allocatorCreateInfo.vulkanApiVersion(VK_API_VERSION_1_2);

            PointerBuffer pAllocator = stack.pointers(VK_NULL_HANDLE);

            checkResult(vmaCreateAllocator(allocatorCreateInfo, pAllocator),
                        "Failed to create Allocator");

            allocator = pAllocator.get(0);
        }
    }

    private static void createCommandPool() {

        try (MemoryStack stack = stackPush()) {

            Queue.QueueFamilyIndices queueFamilyIndices = getQueueFamilies();

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);

            checkResult(vkCreateCommandPool(DeviceManager.vkDevice, poolInfo, null, pCommandPool),
                        "Failed to create command pool");

            commandPool = pCommandPool.get(0);
        }
    }

    private static void checkSurfaceCapabilities2Support(MemoryStack stack) {
        if (org.lwjgl.system.Platform.get() != org.lwjgl.system.Platform.WINDOWS) {
            return;
        }

        IntBuffer propertyCount = stack.ints(0);
        vkEnumerateInstanceExtensionProperties((String) null, propertyCount, null);

        if (propertyCount.get(0) == 0) {
            return;
        }

        VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(propertyCount.get(0), stack);
        vkEnumerateInstanceExtensionProperties((String) null, propertyCount, availableExtensions);

        Set<String> availableExtensionNames = availableExtensions.stream()
                                                                 .map(VkExtensionProperties::extensionNameString)
                                                                 .collect(toSet());

        if (availableExtensionNames.contains(KHRGetSurfaceCapabilities2.VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME)) {
            surfaceCapabilities2Supported = true;
        }
    }

    private static PointerBuffer getRequiredInstanceExtensions() {

        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        int extraCount = (ENABLE_VALIDATION_LAYERS ? 1 : 0) + (surfaceCapabilities2Supported ? 1 : 0);
        if (extraCount > 0) {

            MemoryStack stack = stackGet();

            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + extraCount);

            extensions.put(glfwExtensions);
            if (ENABLE_VALIDATION_LAYERS) {
                extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            }
            if (surfaceCapabilities2Supported) {
                extensions.put(stack.UTF8(KHRGetSurfaceCapabilities2.VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME));
            }

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind();
        }

        return glfwExtensions;
    }

    public static void checkResult(int result, String errorMessage) {
        if (result != VK_SUCCESS) {
            throw new RuntimeException(String.format("%s: %s", errorMessage, VkResult.decode(result)));
        }
    }

    public static void setVsync(boolean b) {
        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        if (swapChain.isVsync() != b) {
            Renderer.scheduleSwapChainUpdate();
            swapChain.setVsync(b);
        }
    }

    public static int getDefaultDepthFormat() {
        return DEFAULT_DEPTH_FORMAT;
    }

    public static long getSurface() {
        return surface;
    }

    public static long getCommandPool() {
        return commandPool;
    }

    public static StagingBuffer getStagingBuffer() {
        return stagingBuffers.getStagingBuffer();
    }

    public static StagingBuffers getStagingBuffers() {
        return stagingBuffers;
    }

    public static Device getDevice() {
        return DeviceManager.device;
    }
}

