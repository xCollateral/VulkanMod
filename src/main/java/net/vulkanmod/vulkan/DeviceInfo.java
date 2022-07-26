package net.vulkanmod.vulkan;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.vulkan.VK10.*;

public class DeviceInfo {

    public static final String cpuInfo;
    public static final String vendorId;
    public static final String deviceName;
    public static final String driverVersion;
    public static String vkVersion;

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
            vkVersion= (VK_VERSION_MAJOR(Vulkan.vkRawVersion)+"."+VK_VERSION_MINOR(Vulkan.vkRawVersion)+"."+VK_VERSION_PATCH(Vulkan.vkRawVersion));
        }
        else {
            vendorId = "n/a";
            deviceName = "n/a";
            driverVersion = "n/a";
        }

    }
    //Source: https://old.reddit.com/r/vulkan/comments/fmift4/how_to_decode_driverversion_field_of/fl4mx0t/
    //0x10DE = Nvidia: https://pcisig.com/membership/member-companies?combine=Nvidia
    //https://registry.khronos.org/vulkan/specs/1.3-extensions/man/html/VkPhysicalDeviceProperties.html
    private static String decodeDvrVersion(int v, int i) {
        return i == 0x10DE ? ((v >>> 22) & 0x3FF) + "." + ((v >>> 14) & 0xff) + "." + ((v >>> 6) & 0xff) + "." + ((v) & 0xf) : ((v >>> 22) & 0x3FF) + "." + ((v >>> 12) & 0xff) + "." + ((v & 0xfff));
    }
}
