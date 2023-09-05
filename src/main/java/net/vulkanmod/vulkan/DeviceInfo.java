package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.framebuffer.SwapChain;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkEnumerateInstanceVersion;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;

public class DeviceInfo {

    public static final String cpuInfo;
    public static final List<GraphicsCard> graphicsCards;

    private final VkPhysicalDevice device;
    public final String vendorId;
    public final String deviceName;
    public final String driverVersion;
    public final String vkVersion;

    public GraphicsCard graphicsCard;

    public final VkPhysicalDeviceFeatures2 availableFeatures;
    public final VkPhysicalDeviceVulkan11Features availableFeatures11;

//    public final VkPhysicalDeviceVulkan13Features availableFeatures13;
//    public final boolean vulkan13Support;

    private boolean drawIndirectSupported;

    static {
        CentralProcessor centralProcessor = new SystemInfo().getHardware().getProcessor();
        cpuInfo = String.format("%s", centralProcessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");
        graphicsCards = new SystemInfo().getHardware().getGraphicsCards();

    }

    public DeviceInfo(VkPhysicalDevice device, VkPhysicalDeviceProperties properties) {
        for(GraphicsCard gpu : graphicsCards) {
            if(Objects.equals(gpu.getName(), properties.deviceNameString()))
                graphicsCard = gpu;
        }

        this.device = device;
        this.vendorId = decodeVendor(properties.vendorID());
        this.deviceName = properties.deviceNameString();
        this.driverVersion = decodeDvrVersion(Device.deviceProperties.driverVersion(), Device.deviceProperties.vendorID());
        this.vkVersion = decDefVersion(getVkVer());

        this.availableFeatures = VkPhysicalDeviceFeatures2.calloc();
        this.availableFeatures.sType$Default();

        this.availableFeatures11 = VkPhysicalDeviceVulkan11Features.malloc();
        this.availableFeatures11.sType$Default();
        this.availableFeatures.pNext(this.availableFeatures11);

        //Vulkan 1.3
//        this.availableFeatures13 = VkPhysicalDeviceVulkan13Features.malloc();
//        this.availableFeatures13.sType$Default();
//        this.availableFeatures11.pNext(this.availableFeatures13.address());
//
//        this.vulkan13Support = this.device.getCapabilities().apiVersion == VK_API_VERSION_1_3;

        vkGetPhysicalDeviceFeatures2(this.device, this.availableFeatures);

        if(this.availableFeatures.features().multiDrawIndirect() && this.availableFeatures11.shaderDrawParameters())
                this.drawIndirectSupported = true;

    }

    private static String decodeVendor(int i) {
        return switch (i) {
            case (0x10DE) -> "Nvidia";
            case (0x1022) -> "AMD";
            case (0x8086) -> "Intel";
            default -> "undef"; //Either AMD or Unknown Driver version/vendor and.or Encoding Scheme
        };
    }

    //Should Work with AMD: https://gpuopen.com/learn/decoding-radeon-vulkan-versions/

    static String decDefVersion(int v) {
        return VK_VERSION_MAJOR(v) + "." + VK_VERSION_MINOR(v) + "." + VK_VERSION_PATCH(v);
    }
    //0x10DE = Nvidia: https://pcisig.com/membership/member-companies?combine=Nvidia
    //https://registry.khronos.org/vulkan/specs/1.3-extensions/man/html/VkPhysicalDeviceProperties.html
    //this should work with Nvidia + AMD but is not guaranteed to work with intel drivers in Windows and more obscure/Exotic Drivers/vendors
    private static String decodeDvrVersion(int v, int i) {
        return switch (i) {
            case (0x10DE) -> decodeNvidia(v); //Nvidia
            case (0x1022) -> decDefVersion(v); //AMD
            case (0x8086) -> decIntelVersion(v); //Intel
            default -> decDefVersion(v); //Either AMD or Unknown Driver Encoding Scheme
        };
    }

    //Source: https://www.intel.com/content/www/us/en/support/articles/000005654/graphics.html
    //Won't Work with older Drivers (15.45 And.or older)
    //May not work as this uses Guess work+Assumptions
    private static String decIntelVersion(int v) {
        return (glfwGetPlatform()==GLFW_PLATFORM_WIN32) ? (v >>> 14) + "." + (v & 0x3fff) : decDefVersion(v);
    }


    private static String decodeNvidia(int v) {
        return (v >>> 22 & 0x3FF) + "." + (v >>> 14 & 0xff) + "." + (v >>> 6 & 0xff) + "." + (v & 0xff);
    }

    static int getVkVer() {
        try(MemoryStack stack = MemoryStack.stackPush())
        {
            var a = stack.mallocInt(1);
            vkEnumerateInstanceVersion(a);
            int vkVer1 = a.get(0);
            if(VK_VERSION_MINOR(vkVer1)<2)
            {
                throw new RuntimeException("Vulkan 1.2 not supported!: "+"Only Has: "+ decDefVersion(vkVer1));
            }
            return vkVer1;
        }
    }

    private String unsupportedExtensions(Set<String> requiredExtensions) {

        try(MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

            Set<String> extensions = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet());

            requiredExtensions.removeAll(extensions);

            return "Unsupported extensions: " + Arrays.toString(requiredExtensions.toArray());
        }
    }

    public static String debugString(PointerBuffer ppPhysicalDevices, Set<String> requiredExtensions, VkInstance instance) {
        try (MemoryStack stack = stackPush()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n");

            for(int i = 0; i < ppPhysicalDevices.capacity();i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

                VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.callocStack(stack);
                vkGetPhysicalDeviceProperties(device, deviceProperties);

                DeviceInfo info = new DeviceInfo(device, deviceProperties);

                stringBuilder.append(String.format("Device %d: ", i)).append(info.deviceName).append("\n");
                stringBuilder.append(info.unsupportedExtensions(requiredExtensions)).append("\n");

                Device.SurfaceProperties surfaceProperties = Device.querySurfaceProperties(device, stack);
                boolean swapChainAdequate = surfaceProperties.formats.hasRemaining() && surfaceProperties.presentModes.hasRemaining() ;
                stringBuilder.append("Swapchain supported: ").append(swapChainAdequate ? "true" : "false").append("\n");
            }

            return stringBuilder.toString();
        }
    }

    public boolean isDrawIndirectSupported() {
        return drawIndirectSupported;
    }
}
