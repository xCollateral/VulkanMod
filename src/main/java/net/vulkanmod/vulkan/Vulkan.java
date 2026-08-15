package net.vulkanmod.vulkan;

import net.minecraft.util.Util;
import net.vulkanmod.Initializer;
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
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
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

    public static final Set<String> REQUIRED_DEVICE_EXTENSIONS = Set.of(
            "VK_KHR_dynamic_rendering", "VK_KHR_synchronization2", "VK_KHR_swapchain"
    );

    public static long window;


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
        Instance.createInstance();
        Debug.setupDebugMessenger();
        createSurface(window);

        DeviceManager.init(Instance.instance);
        setupDepthFormat();

        createVma();
        MemoryTypes.createMemoryTypes();

        createCommandPool();
    }

    public static void createSurface(long handle) {
        window = handle;

        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            checkResult(glfwCreateWindowSurface(Instance.instance, window, null, pSurface),
                        "Failed to create window surface");

            surface = pSurface.get(0);
        }
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
        Debug.destroyDebugUtilsMessengerEXT(Instance.instance, debugMessenger, null);
        KHRSurface.vkDestroySurfaceKHR(Instance.instance, surface, null);
        vkDestroyInstance(Instance.instance, null);
    }

    private static void freeStagingBuffers() {
        stagingBuffers.free();
    }

    private static void createVma() {
        try (MemoryStack stack = stackPush()) {

            VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.calloc(stack);
            vulkanFunctions.set(Instance.instance, DeviceManager.vkDevice);

            VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.calloc(stack);
            allocatorCreateInfo.physicalDevice(DeviceManager.physicalDevice);
            allocatorCreateInfo.device(DeviceManager.vkDevice);
            allocatorCreateInfo.pVulkanFunctions(vulkanFunctions);
            allocatorCreateInfo.instance(Instance.instance);
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

    public static void checkResult(int result, String errorMessage) {
        if (result < VK_SUCCESS) {
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

    public static VkDevice getVkDevice() {
        return DeviceManager.vkDevice;
    }

    public static long getAllocator() {
        return allocator;
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

    public static class Instance {
        public static Set<String> instanceExtensions;
        private static VkInstance instance;

        private static void createInstance() {
            if (ENABLE_VALIDATION_LAYERS && !Debug.checkValidationLayerSupport()) {
                throw new RuntimeException("Validation requested but not supported");
            }

            instanceExtensions = querySupportedInstanceExtension();
            Vulkan.surfaceCapabilities2Supported = hasSurfaceCapabilities2Support();

        try (MemoryStack stack = stackPush()) {
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
                    Debug.populateDebugMessengerCreateInfo(debugCreateInfo);
                    createInfo.pNext(debugCreateInfo.address());
                }

                // We need to retrieve the pointer of the created instance
                PointerBuffer instancePtr = stack.mallocPointer(1);

                int result = vkCreateInstance(createInfo, null, instancePtr);
                checkResult(result, "Failed to create instance");

                instance = new VkInstance(instancePtr.get(0), createInfo);
            }
        }

        private static PointerBuffer getRequiredInstanceExtensions() {
            PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

            List<String> otherExtensions = new ArrayList<>();
            if (Util.getPlatform() == Util.OS.OSX && instanceExtensions.contains(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)) {
                otherExtensions.add(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME);
            }

            if (surfaceCapabilities2Supported) {
                otherExtensions.add(KHRGetSurfaceCapabilities2.VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME);
            }

            boolean renderdocAttached = "1".equals(System.getenv("ENABLE_VULKAN_RENDERDOC_CAPTURE"));
            if (ENABLE_VALIDATION_LAYERS || renderdocAttached) {
                otherExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
                Debug.useDebugLabels = true;
            }

            if (!otherExtensions.isEmpty()) {
                MemoryStack stack = stackGet();
                PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + otherExtensions.size());

                extensions.put(glfwExtensions);

                for (String s : otherExtensions) {
                    extensions.put(stack.UTF8(s));
                }

                // Rewind the buffer before returning it to reset its position back to 0
                return extensions.rewind();
            }

            return glfwExtensions;
        }

        private static boolean hasSurfaceCapabilities2Support() {
            if (org.lwjgl.system.Platform.get() != org.lwjgl.system.Platform.WINDOWS) {
                return false;
            }

            if (instanceExtensions.contains(KHRGetSurfaceCapabilities2.VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME)) {
                return true;
            }

            return false;
        }

        private static Set<String> querySupportedInstanceExtension() {
            MemoryStack stack = stackGet();
            var pExtensionCount = stack.mallocInt(1);

            vkEnumerateInstanceExtensionProperties((String) null, pExtensionCount, null);

            VkExtensionProperties.Buffer instanceExtensionProperties = VkExtensionProperties.malloc(pExtensionCount.get(0));
            vkEnumerateInstanceExtensionProperties((String) null, pExtensionCount, instanceExtensionProperties);

            HashSet<String> instanceExtensions = new HashSet<>();
            for (int i = 0; i < pExtensionCount.get(0); ++i) {
                instanceExtensions.add(instanceExtensionProperties.get(i).extensionNameString());
            }

            return instanceExtensions;
        }
    }

    public static class Debug {
        public static final Set<String> VALIDATION_LAYERS;
        private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), 3);
        private static final Logger LOGGER = Initializer.LOGGER;

        private static boolean useDebugLabels;

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

        private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
            VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

            String message;
            if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                message = "\u001B[31m" + callbackData.pMessageString();

                String callStack = STACK_WALKER.walk(
                        s -> s.filter(frame -> frame.getDeclaringClass() != Vulkan.Debug.class && !frame.getDeclaringClass().getPackageName().startsWith("org.lwjgl"))
                              .limit(5L)
                              .map(frame -> "\t" + frame)
                              .collect(Collectors.joining("\n"))
                );
                LOGGER.error("{}\n{}", message, callStack);
            } else {
                message = callbackData.pMessageString();
                LOGGER.info("{}", message);
            }

//            System.err.println(message);

            if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0)
                System.nanoTime();

            return VK_FALSE;
        }

        private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                        VkAllocationCallbacks allocationCallbacks,
                                                        LongBuffer pDebugMessenger
        ) {
            if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
                return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
            }

            return VK_ERROR_EXTENSION_NOT_PRESENT;
        }

        private static void destroyDebugUtilsMessengerEXT(
                VkInstance instance, long debugMessenger, VkAllocationCallbacks allocationCallbacks
        ) {
            if (vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
                vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
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
            debugCreateInfo.pfnUserCallback(Vulkan.Debug::debugCallback);
        }

        private static void setupDebugMessenger() {
            if (!ENABLE_VALIDATION_LAYERS) {
                return;
            }

            try (MemoryStack stack = stackPush()) {
                VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);

                populateDebugMessengerCreateInfo(createInfo);

                LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

                checkResult(createDebugUtilsMessengerEXT(Instance.instance, createInfo, null, pDebugMessenger),
                            "Failed to set up debug messenger");

                debugMessenger = pDebugMessenger.get(0);
            }
        }

        public static void setDebugLabel(MemoryStack stack, int objectType, long handle, String label) {
            if (useDebugLabels) {
                VkDebugUtilsObjectNameInfoEXT nameInfo = VkDebugUtilsObjectNameInfoEXT.calloc(stack);
                nameInfo.sType$Default();
                nameInfo.objectType(objectType);
                nameInfo.objectHandle(handle);
                nameInfo.pObjectName(stackUTF8(label));
                EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(Vulkan.getVkDevice(), nameInfo);
            }
        }

        public static void pushDebugSection(VkCommandBuffer commandBuffer, String s) {
            if (useDebugLabels) {
                try (MemoryStack stack = stackPush()) {
                    VkDebugUtilsLabelEXT markerInfo = VkDebugUtilsLabelEXT.calloc(stack).sType$Default();
                    ByteBuffer string = stack.UTF8(s);
                    markerInfo.pLabelName(string);
                    vkCmdBeginDebugUtilsLabelEXT(commandBuffer, markerInfo);
                }
            }
        }

        public static void popDebugSection(VkCommandBuffer commandBuffer) {
            if (useDebugLabels) {
                vkCmdEndDebugUtilsLabelEXT(commandBuffer);
            }
        }
    }
}

