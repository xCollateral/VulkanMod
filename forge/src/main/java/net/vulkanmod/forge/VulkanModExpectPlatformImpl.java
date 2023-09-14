package net.vulkanmod.forge;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class VulkanModExpectPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
