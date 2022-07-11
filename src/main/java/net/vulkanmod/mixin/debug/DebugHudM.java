package net.vulkanmod.mixin.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.vulkanmod.vulkan.DeviceInfo;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(DebugHud.class)
public abstract class DebugHudM {

    @Shadow @Final private MinecraftClient client;

    @Shadow
    private static long toMiB(long bytes) {
        return 0;
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> redirectList(Object[] elements) {
        ArrayList<String> strings = new ArrayList<>();

        long l = Runtime.getRuntime().maxMemory();
        long m = Runtime.getRuntime().totalMemory();
        long n = Runtime.getRuntime().freeMemory();
        long o = m - n;

        strings.add(String.format("Java: %s %dbit", System.getProperty("java.version"), this.client.is64Bit() ? 64 : 32));
        strings.add(String.format("Mem: % 2d%% %03d/%03dMB", o * 100L / l, toMiB(o), toMiB(l)));
        strings.add(String.format("Allocated: % 2d%% %03dMB", m * 100L / l, toMiB(m)));
        strings.add(String.format("Off-heap: " + getOffHeapMemory() + "MB"));
        strings.add("NativeMemory: " + MemoryManager.getNativeMemory() / (1024 * 1024) + "MB");
        strings.add("DeviceMemory: " + MemoryManager.getDeviceMemory() / (1024 * 1024) + "MB");
        strings.add("");
        strings.add("VulkanMod " + getVersion());
        strings.add("CPU: " + DeviceInfo.cpuInfo);
        strings.add("GPU: " + DeviceInfo.deviceName);
        strings.add("Driver: " + DeviceInfo.driverVersion);
        strings.add("");

        return strings;
    }

    private long getOffHeapMemory() {
        return toMiB(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }
}
