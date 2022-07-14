package net.vulkanmod.mixin;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vulkanmod.vulkan.Vulkan.setvSyncState;

@Mixin(Window.class)
public class GameOptionsHook
{
    @Shadow
    private boolean vsync;

    @Inject(at = @At("HEAD"), method="setVsync(Z)V")
    public void setVsync(boolean vsync, CallbackInfo ci) {
//        RenderSystem.assertOnRenderThreadOrInit();
        this.vsync= vsync;
        System.out.println("VSYNC: "+this.vsync);
        setvSyncState(this.vsync);
//        GLFW.glfwSwapInterval(vsync ? 1 : 0);
    }
}
