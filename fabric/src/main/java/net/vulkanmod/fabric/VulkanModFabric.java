package net.vulkanmod.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.vulkanmod.Initializer;

public class VulkanModFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Initializer.onInitializeClient();
    }
}
