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
    public static String VkVersion;

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
            driverVersion = String.valueOf(Vulkan.deviceProperties.driverVersion());
            int va=Vulkan.deviceProperties.apiVersion();
            VkVersion = String.valueOf((VK_VERSION_MAJOR(va) +"."+ VK_VERSION_MINOR(va) +"."+ VK_VERSION_PATCH(va)));
        }
        else {
            vendorId = "n/a";
            deviceName = "n/a";
            driverVersion = "n/a";
        }

    }

}
