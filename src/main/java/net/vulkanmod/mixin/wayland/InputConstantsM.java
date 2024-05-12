package net.vulkanmod.mixin.wayland;

import com.mojang.blaze3d.platform.InputConstants;
import net.vulkanmod.config.Platform;
import org.lwjgl.glfw.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InputConstants.class)
public class InputConstantsM {
    /**
     * @author
     * @reason Setting the cursor position is not supported on Wayland
     */
    @Redirect(method = "grabOrReleaseMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"))
    private static void grabOrReleaseMouse(long window, double xpos, double ypos) {
        if (!Platform.isWayLand())
            GLFW.glfwSetCursorPos(window, xpos, ypos);
    }
}