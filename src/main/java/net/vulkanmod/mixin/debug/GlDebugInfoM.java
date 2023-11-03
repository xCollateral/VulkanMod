package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.platform.GlUtil;
import net.vulkanmod.vulkan.DeviceInfo;
import net.vulkanmod.vulkan.Vulkan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GlUtil.class)
public class GlDebugInfoM {

    /**
     * @author
     */
    @Overwrite
    public static String getVendor() {
        return Vulkan.getDeviceInfo() != null ? Vulkan.getDeviceInfo().vendorIdString : "n/a";
    }

    /**
     * @author
     */
    @Overwrite
    public static String getRenderer() {
        return Vulkan.getDeviceInfo() != null ? Vulkan.getDeviceInfo().deviceName : "n/a";
    }

    /**
     * @author
     */
    @Overwrite
    public static String getOpenGLVersion() {
        return Vulkan.getDeviceInfo() != null ? Vulkan.getDeviceInfo().driverVersion : "n/a";
    }

    /**
     * @author
     */
    @Overwrite
    public static String getCpuInfo() {
        return DeviceInfo.cpuInfo;
    }
}
