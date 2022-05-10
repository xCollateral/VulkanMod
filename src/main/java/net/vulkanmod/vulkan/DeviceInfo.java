package net.vulkanmod.vulkan;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DeviceInfo {

    public static final String cpuInfo;
    public static final String vendorId;
    public static final String deviceName;
    public static final String driverVersion;

    static {
        CentralProcessor centralProcessor = new SystemInfo().getHardware().getProcessor();
        cpuInfo = String.format("%s", centralProcessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");

        vendorId = String.valueOf(Vulkan.deviceProperties.vendorID());

        ByteBuffer byteBuffer = Vulkan.deviceProperties.deviceName();

        int pos = 0;
        while(byteBuffer.get(pos) != '\0') pos++;

        byte[] bytes = new byte[pos];
        byteBuffer.get(bytes);

//        deviceName = new String(bytes, StandardCharsets.UTF_8);
        deviceName = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
        driverVersion = String.valueOf(Vulkan.deviceProperties.driverVersion());
    }

}
