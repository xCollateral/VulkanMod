package net.vulkanmod.vulkan;

import org.jetbrains.annotations.NotNull;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.vulkan.VK10.VK_VERSION_MAJOR;
import static org.lwjgl.vulkan.VK10.VK_VERSION_MINOR;
import static org.lwjgl.vulkan.VK10.VK_VERSION_PATCH;

public class DeviceInfo {

    public static final String cpuInfo;
    public static final String vendorId;
    public static final String deviceName;
    public static final String driverVersion;
    public static final String vkVersion;

    static {
        CentralProcessor centralProcessor = new SystemInfo().getHardware().getProcessor();
        cpuInfo = String.format("%s", centralProcessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");

        if(Vulkan.deviceProperties != null) {
            vendorId = String.valueOf(Vulkan.deviceProperties.vendorID());

            ByteBuffer byteBuffer = Vulkan.deviceProperties.deviceName();

            int pos = 0;
            while(byteBuffer.get(pos) != '\0') pos++;

            byte[] bytes = new byte[pos];
            byteBuffer.get(bytes);

//        deviceName = new String(bytes, StandardCharsets.UTF_8);
            deviceName = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
            driverVersion = decodeDvrVersion(Vulkan.deviceProperties.driverVersion(), Vulkan.deviceProperties.vendorID());
            vkVersion= decDefVersion(Vulkan.vkRawVersion);
        }
        else {
            vendorId = "n/a";
            deviceName = "n/a";
            driverVersion = "n/a";
            vkVersion = "n/a";
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
    /**Major -> 000 - Minor -> 00 - Patch -> 00*/
    private static String decodeNvidia(int v) {
        return (v >>> 22 & 0x3FF) + "." + (v >>> 14 & 0xff) + "." + (v & 0xf >>> 1) + (v & 0xf);
    }

}
