package net.vulkanmod;

import java.nio.file.Path;

public class ExpectPlatform {

    @dev.architectury.injectables.annotations.ExpectPlatform
    public static Path getConfigDirectory() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }
}
