package net.vulkanmod.forge;

import net.minecraftforge.fml.common.Mod;
import net.vulkanmod.Initializer;

@Mod(Initializer.MODID)
public class VulkanModForge {
    public VulkanModForge() {
        Initializer.onInitializeClient();
    }
}