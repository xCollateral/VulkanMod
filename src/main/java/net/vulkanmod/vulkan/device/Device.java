package net.vulkanmod.vulkan.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;

public class Device {
    final VkPhysicalDevice physicalDevice;
    final VkPhysicalDeviceProperties2 properties;

    private final int vendorId;
    public final String vendorIdString;
    public final String deviceName;
    public final String driverVersion;
    public final String vkVersion;


    private final boolean drawIndirectSupported, hasIndexedDescriptors, hasSamplerAnisotropy, hasInlineUniforms, hasLogicOp, hasWideLines;
    private final boolean hasBindless;
    public Device(VkPhysicalDevice device) {

        //Using memory stack to avoid the off-heap becoming cluttered w/ unused DeviceProperties
        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.physicalDevice = device;

            VkPhysicalDeviceSubgroupProperties subgroupProperties = VkPhysicalDeviceSubgroupProperties.malloc(stack).sType$Default();
            VkPhysicalDeviceVulkan12Properties vk12Properties = VkPhysicalDeviceVulkan12Properties.malloc(stack).sType$Default();

            properties = VkPhysicalDeviceProperties2.calloc().sType$Default().pNext(subgroupProperties).pNext(vk12Properties);
            VK11.vkGetPhysicalDeviceProperties2(physicalDevice, properties);

            this.vendorId = properties.properties().vendorID();
            this.vendorIdString = decodeVendor(properties.properties().vendorID());
            this.deviceName = properties.properties().deviceNameString();
            this.driverVersion = decodeDvrVersion(properties.properties().driverVersion(), properties.properties().vendorID());
            this.vkVersion = decDefVersion(getVkVer());

            VkPhysicalDeviceFeatures2 availableFeatures = VkPhysicalDeviceFeatures2.calloc(stack);
            availableFeatures.sType$Default();

            VkPhysicalDeviceInlineUniformBlockFeaturesEXT inlineUniformBlockFeaturesEXT = VkPhysicalDeviceInlineUniformBlockFeaturesEXT.malloc(stack).sType$Default();

            VkPhysicalDeviceVulkan12Features availableFeatures12 = VkPhysicalDeviceVulkan12Features.malloc(stack).sType$Default();

            VkPhysicalDeviceVulkan11Features availableFeatures11 = VkPhysicalDeviceVulkan11Features.malloc(stack).sType$Default();
            availableFeatures.pNext(availableFeatures12);
            availableFeatures.pNext(availableFeatures11);
            availableFeatures.pNext(inlineUniformBlockFeaturesEXT);
            vkGetPhysicalDeviceFeatures2(this.physicalDevice, availableFeatures);


            this.drawIndirectSupported = availableFeatures.features().multiDrawIndirect() && availableFeatures11.shaderDrawParameters();
            this.hasIndexedDescriptors = availableFeatures12.runtimeDescriptorArray() && availableFeatures12.descriptorBindingVariableDescriptorCount() && availableFeatures12.descriptorBindingPartiallyBound() && availableFeatures12.shaderSampledImageArrayNonUniformIndexing();
            this.hasSamplerAnisotropy = availableFeatures.features().samplerAnisotropy();
            this.hasInlineUniforms = inlineUniformBlockFeaturesEXT.inlineUniformBlock();
            this.hasLogicOp = availableFeatures.features().logicOp();
            this.hasWideLines = availableFeatures.features().wideLines();

            this.hasBindless = vk12Properties.maxPerStageDescriptorUpdateAfterBindSamplers() > 65536 || properties.properties().limits().maxPerStageDescriptorSamplers() > 65536;


            final int subGroupStages = subgroupProperties.supportedStages();
            final int subGroupOps = subgroupProperties.supportedOperations();

            //TODO: Specific MoltenVk drivers do not support Subgroup Ballot, which is used for texture broadcast
            // Uniform texture broadcast: Ensures that a texture index is Dynamically Uniform (i.e. Ensures The index is constant for the entire Drawcall)
            if((subGroupStages & VK_SHADER_STAGE_FRAGMENT_BIT)==0 ||(subGroupOps & VK_SUBGROUP_FEATURE_BALLOT_BIT)==0)
                throw new RuntimeException("Uniform texture broadcast optimization not available!");

            if(!hasInlineUniforms)
                throw new RuntimeException("Inline Uniform Block not available!");

            if(!hasIndexedDescriptors)
                throw new RuntimeException("Descriptor indexing (Bindless Textures) not available!");


        }

    }

    private static String decodeVendor(int i) {
        return switch (i) {
            case (0x10DE) -> "Nvidia";
            case (0x1022) -> "AMD";
            case (0x8086) -> "Intel";
            default -> "undef"; //Either AMD or Unknown Driver version/vendor and.or Encoding Scheme
        };
    }

    // Should Work with AMD: https://gpuopen.com/learn/decoding-radeon-vulkan-versions/

    static String decDefVersion(int v) {
        return VK_VERSION_MAJOR(v) + "." + VK_VERSION_MINOR(v) + "." + VK_VERSION_PATCH(v);
    }

    // 0x10DE = Nvidia: https://pcisig.com/membership/member-companies?combine=Nvidia
    // https://registry.khronos.org/vulkan/specs/1.3-extensions/man/html/VkPhysicalDeviceProperties.html
    // this should work with Nvidia + AMD but is not guaranteed to work with intel drivers in Windows and more obscure/Exotic Drivers/vendors
    private static String decodeDvrVersion(int v, int i) {
        return switch (i) {
            case (0x10DE) -> decodeNvidia(v); //Nvidia
            case (0x1022) -> decDefVersion(v); //AMD
            case (0x8086) -> decIntelVersion(v); //Intel
            default -> decDefVersion(v); //Either AMD or Unknown Driver Encoding Scheme
        };
    }

    // Source: https://www.intel.com/content/www/us/en/support/articles/000005654/graphics.html
    // Won't Work with older Drivers (15.45 And.or older)
    // May not work as this uses Guess work+Assumptions
    private static String decIntelVersion(int v) {
        return (glfwGetPlatform() == GLFW_PLATFORM_WIN32) ? (v >>> 14) + "." + (v & 0x3fff) : decDefVersion(v);
    }


    private static String decodeNvidia(int v) {
        return (v >>> 22 & 0x3FF) + "." + (v >>> 14 & 0xff) + "." + (v >>> 6 & 0xff) + "." + (v & 0xff);
    }

    static int getVkVer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var a = stack.mallocInt(1);
            vkEnumerateInstanceVersion(a);
            int vkVer1 = a.get(0);
            if (VK_VERSION_MINOR(vkVer1) < 2) {
                throw new RuntimeException("Vulkan 1.2 not supported: Only Has: %s".formatted(decDefVersion(vkVer1)));
            }
            return vkVer1;
        }
    }

    public Set<String> getUnsupportedExtensions(Set<String> requiredExtensions) {
        try (MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, availableExtensions);

            Set<String> extensions = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet());

            Set<String> unsupportedExtensions = new HashSet<>(requiredExtensions);
            unsupportedExtensions.removeAll(extensions);

            return unsupportedExtensions;
        }
    }

    public boolean isDrawIndirectSupported() {
        return drawIndirectSupported;
    }

    public boolean isHasIndexedDescriptors() {
        return hasIndexedDescriptors;
    }

    // Added these to allow detecting GPU vendor, to allow handling vendor specific circumstances:
    // (e.g. such as in case we encounter a vendor specific driver bug)
    public boolean isAMD() {
        return vendorId == 0x1022;
    }

    public boolean isNvidia() {
        return vendorId == 0x10DE;
    }

    public boolean isIntel() {
        return vendorId == 0x8086;
    }

    public boolean hasSamplerAnisotropy() {
        return this.hasSamplerAnisotropy;
    }

    public boolean hasWideLines() {
        return this.hasWideLines;
    }

    public boolean hasLogicOp() {
        return this.hasLogicOp;
    }

    public boolean isHasInlineUniforms() {
        return hasInlineUniforms;
    }

    public boolean isHasBindless() {
        return hasBindless;
    }
}
