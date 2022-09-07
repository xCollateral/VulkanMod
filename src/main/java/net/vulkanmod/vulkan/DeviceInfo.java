package net.vulkanmod.vulkan;


import org.jetbrains.annotations.NotNull;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;

import static org.lwjgl.vulkan.VK10.VK_VERSION_MAJOR;
import static org.lwjgl.vulkan.VK10.VK_VERSION_MINOR;
import static org.lwjgl.vulkan.VK10.VK_VERSION_PATCH;

public class DeviceInfo {

    public static final String cpuInfo;

    public final String vendorId;
    public final String deviceName;
    public final String driverVersion;
    public final String vkVersion;

    public static final List<GraphicsCard> graphicsCards;

    private final VkPhysicalDevice device;

    public GraphicsCard graphicsCard;


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
        this.vendorId = String.valueOf(properties.vendorID());
        this.deviceName = properties.deviceNameString();
        this.driverVersion = decodeDvrVersion(properties.driverVersion(), properties.vendorID());
        this.vkVersion = decDefVersion(Vulkan.vkRawVersion);
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


//

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

                Vulkan.SwapChainSupportDetails swapChainSupport = Vulkan.querySwapChainSupport(device, stack);
                boolean swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining() ;
                stringBuilder.append("Swapchain supported: ").append(swapChainAdequate ? "true" : "false").append("\n");
            }

            return stringBuilder.toString();
        }
    }

    //Should Work with AMD: https://gpuopen.com/learn/decoding-radeon-vulkan-versions/
    @NotNull
    private static String decDefVersion(int v) {
        return VK_VERSION_MAJOR(v) + "." + VK_VERSION_MINOR(v) + "." + VK_VERSION_PATCH(v);
    }

    //Source: https://old.reddit.com/r/vulkan/comments/fmift4/how_to_decode_driverversion_field_of/fl4mx0t/
    //0x10DE = Nvidia: https://pcisig.com/membership/member-companies?combine=Nvidia
    //https://registry.khronos.org/vulkan/specs/1.3-extensions/man/html/VkPhysicalDeviceProperties.html

    //todo: this should work with Nvidia + AMD but is not guaranteed to work with intel drivers in Windows and more obscure/Exotic Drivers/vendors
    private static String decodeDvrVersion(int v, int i) {
        return switch (i) {
            case (0x10DE) -> decodeNvidia(v); //Nvidia
            case (0x1022) -> decDefVersion(v); //AMD
            case (0x5143) -> decQualCommVersion(v); //Qualcomm
            case (0x8086) -> decIntelVersion(v); //Intel
            default -> decDefVersion(v); //Either AMD or Unknown Driver version/vendor and.or Encoding Scheme
        };
    }

    private static String decQualCommVersion(int v) {
        return null;
    }

    //Source: https://www.intel.com/content/www/us/en/support/articles/000005654/graphics.html
    //Won't Work with older Drivers (15.45 And.or older)
    //Extremely unlikely to work as this uses Guess work+Assumptions
    private static String decIntelVersion(int v) {
        return (v >>> 30) + "." + (v >>> 27 & 0x7) + "." + (v & 0xF);
    }

    @NotNull
    private static String decodeNvidia(int v) {
        return (v >>> 22 & 0x3FF) + "." + (v >>> 14 & 0xff) + "." + (v >>> 6 & 0xff) + (v & 0xff);
    }

}
