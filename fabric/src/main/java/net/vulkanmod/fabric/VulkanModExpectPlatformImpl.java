package net.vulkanmod.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class VulkanModExpectPlatformImpl {
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static String getVersion() {
        return FabricLoader.getInstance().getModContainer("vulkanmod").get().getMetadata().getVersion().getFriendlyString();
    }
}
