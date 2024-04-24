package net.vulkanmod.mixin.wayland;

import com.mojang.blaze3d.platform.InputConstants;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import org.lwjgl.glfw.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(InputConstants.class)
public class InputConstantsM {
    /**
     * @author
     * @reason Setting the cursor position is not supported on Wayland
     */
    @Overwrite
    public static void grabOrReleaseMouse(long window, int inputMode, double xpos, double ypos) {
        if (!Platform.isWayLand())
            GLFW.glfwSetCursorPos(window, xpos, ypos);
        GLFW.glfwSetInputMode(window, 208897, inputMode);
    }
}