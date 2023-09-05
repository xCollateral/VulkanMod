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
    public static void grabOrReleaseMouse(long l, int i, double d, double e) {
        if (!VideoResolution.isWayLand()) GLFW.glfwSetCursorPos(l, d, e);
        GLFW.glfwSetInputMode(l, 208897, i);
    }
}