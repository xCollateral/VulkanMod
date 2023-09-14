package net.vulkanmod.forge.mixin;

import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DisplayWindow.class)
public class ImmediateWindowHandlerMixin {
    @Redirect(method = "initWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"), remap = false)
    public void initWindow(int hint, int value) {
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
    }
}
