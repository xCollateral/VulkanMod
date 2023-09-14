package net.vulkanmod.forge.mixin;

import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DisplayWindow.class)
public class ImmediateWindowHandlerMixin {
    @Redirect(method = "initWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"), remap = false)
    public void initWindow(int hint, int value) {
        if (hint == GLFW.GLFW_CLIENT_API) {
            GLFW.glfwWindowHint(hint, GLFW.GLFW_NO_API);
            System.out.println("WindowHint Updated!");
        } else {
            GLFW.glfwWindowHint(hint, value);
        }
    }
}