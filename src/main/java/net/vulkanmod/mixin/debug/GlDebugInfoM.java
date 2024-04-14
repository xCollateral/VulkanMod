package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.platform.GlUtil;
import net.vulkanmod.vulkan.SystemInfo;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GlUtil.class)
public class GlDebugInfoM {

    /**
     * @author
     */
    @Overwrite
    public static String getVendor() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().vendorIdString : "n/a";
    }

    /**
     * @author
     */
    @Overwrite
    public static String getRenderer() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().deviceName : "n/a";
    }

    /**
     * @author
     */
    @Overwrite
    public static String getOpenGLVersion() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().driverVersion : "n/a";
    }

    /**
     * @author
     */
    @Overwrite
    public static String getCpuInfo() {
        return SystemInfo.cpuInfo;
    }
}
