package net.vulkanmod.vulkan;

import oshi.hardware.CentralProcessor;

public class SystemInfo {
    public static final String cpuInfo;

    static {
        CentralProcessor centralProcessor = new oshi.SystemInfo().getHardware().getProcessor();
        cpuInfo = String.format("%s", centralProcessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");
    }
}
