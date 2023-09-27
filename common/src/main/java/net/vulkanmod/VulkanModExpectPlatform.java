package net.vulkanmod;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class VulkanModExpectPlatform {

    @ExpectPlatform
    public static Path getConfigDirectory() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getVersion() {
        throw new AssertionError();
    }
}
