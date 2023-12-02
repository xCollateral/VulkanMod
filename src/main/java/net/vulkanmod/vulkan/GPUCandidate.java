package net.vulkanmod.vulkan;

import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.vulkan.VK10.*;

public record GPUCandidate(VkPhysicalDevice physicalDevice, String deviceName, int deviceType, int i) {
    String getDeviceTypeString() {
        return switch (this.deviceType) {
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "DISCRETE_GPU";
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "INTEGRATED_GPU";
            case VK_PHYSICAL_DEVICE_TYPE_OTHER -> "OTHER";
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "VIRTUAL_GPU";
            case VK_PHYSICAL_DEVICE_TYPE_CPU -> "CPU";
            default -> "UNKNOWN";
        };
    }
}
