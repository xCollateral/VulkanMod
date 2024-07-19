package net.vulkanmod.mixin.gui;

import net.minecraft.client.gui.Gui;
import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public class VignetteMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private boolean redirectFancyGraphics() {
        return Initializer.CONFIG.vignette;
    }
}
