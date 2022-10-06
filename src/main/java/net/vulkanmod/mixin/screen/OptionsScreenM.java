package net.vulkanmod.mixin.screen;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import net.vulkanmod.config.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class OptionsScreenM extends Screen {

    @Shadow @Final private Screen parent;

    @Shadow @Final private GameOptions settings;

    protected OptionsScreenM(Text title) {
        super(title);
    }

    @Inject(method = "method_19828", at = @At("HEAD"), cancellable = true)
    private void injectVideoOptionScreen(ButtonWidget button, CallbackInfo ci) {
        this.client.setScreen(new VideoSettingsScreen(this, this.settings));
        ci.cancel();
    }
}
