package net.vulkanmod.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.vulkanmod.config.VideoResolution;
import org.lwjgl.glfw.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InputConstants.class)
public class InputConstantsM {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void setupMouseCallbacks(long l, GLFWCursorPosCallbackI gLFWCursorPosCallbackI, GLFWMouseButtonCallbackI gLFWMouseButtonCallbackI, GLFWScrollCallbackI gLFWScrollCallbackI, GLFWDropCallbackI gLFWDropCallbackI) {
        if(!VideoResolution.isWayLand())
        {
           GLFW.glfwSetCursorPosCallback(l, gLFWCursorPosCallbackI);
        }
        GLFW.glfwSetMouseButtonCallback(l, gLFWMouseButtonCallbackI);
        GLFW.glfwSetScrollCallback(l, gLFWScrollCallbackI);
        GLFW.glfwSetDropCallback(l, gLFWDropCallbackI);
    }
}
