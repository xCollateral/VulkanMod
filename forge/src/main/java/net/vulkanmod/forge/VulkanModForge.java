package net.vulkanmod.forge;

import dev.architectury.platform.Platform;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.vulkanmod.Initializer;

@Mod(Initializer.MODID)
public class VulkanModForge {
    public VulkanModForge() {
        // Submit our event bus to let architectury register our content on the right time
        Initializer.onInitializeClient();
    }
}
