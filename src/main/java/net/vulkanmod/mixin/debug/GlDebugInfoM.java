package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.platform.GlDebugInfo;
import net.vulkanmod.vulkan.DeviceInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GlDebugInfo.class)
public class GlDebugInfoM {

    /**
     * @author
     */
    @Overwrite
    public static String getVendor() {
        return DeviceInfo.vendorId;
    }

    /**
     * @author
     */
    @Overwrite
    public static String getRenderer() {
        return DeviceInfo.deviceName;
    }

    /**
     * @author
     */
    @Overwrite
    public static String getVersion() {
        return DeviceInfo.driverVersion;
    }

    /**
     * @author
     */
    @Overwrite
    public static String getCpuInfo() {
        return DeviceInfo.cpuInfo;
    }
}
