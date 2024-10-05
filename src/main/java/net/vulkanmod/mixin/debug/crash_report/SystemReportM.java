package net.vulkanmod.mixin.debug.crash_report;

import net.minecraft.SystemReport;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SystemReport.class)
public class SystemReportM {

	@Inject(method = "appendToCrashReportString", at = @At("RETURN"))
	private void addVulkanDevicesInfo(StringBuilder stringBuilder, CallbackInfo ci) {
		stringBuilder.append("\n\n -- VulkanMod Device Report --");
		stringBuilder.append(DeviceManager.getAvailableDevicesInfo());
	}
}
