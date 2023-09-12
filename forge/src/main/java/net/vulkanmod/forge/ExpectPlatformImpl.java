package net.vulkanmod.forge;

import net.vulkanmod.ExpectPlatform;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ExpectPlatformImpl {
    /**
     * This is our actual method to {@link ExpectPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
