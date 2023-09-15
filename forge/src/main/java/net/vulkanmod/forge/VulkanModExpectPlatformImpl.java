package net.vulkanmod.forge;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class VulkanModExpectPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static String getVersion() {
        return ModList.get().getModContainerById("vulkanmod").get().getModInfo().getVersion().toString();
    }
}
