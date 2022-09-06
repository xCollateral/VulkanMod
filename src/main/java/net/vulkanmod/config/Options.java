package net.vulkanmod.config;

import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;

public class Options {

    public static Option<?>[] getOptions(Config config) {
        return new Option[] {
                new RangeOption("RenderFrameQueue", 2,
                        Math.max(2, Vulkan.swapChainSupport.getCapabilities().maxImageCount()), 1,
                        value -> {
                    Drawer.shouldRecreate = true;
                    config.frameQueueSize = value;
                    }, () -> config.frameQueueSize)
        };
    }

    public static void applyOptions(Config config, Option<?>[] options) {
        for(Option<?> option : options) {
            option.apply();
        }
        config.write();
    }
}
