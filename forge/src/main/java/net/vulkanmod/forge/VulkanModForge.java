package net.vulkanmod.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VulkanMod.MOD_ID)
public class VulkanModForge {
    public VulkanModForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(VulkanModForge.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        ExampleMod.init();
    }
}
