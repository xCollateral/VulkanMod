package net.vulkanmod.config.widget;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.SwitchOption;

public class SwitchOptionWidget extends OptionWidget {
    SwitchOption option;

    private boolean focused;

    public SwitchOptionWidget(SwitchOption option, int x, int y, int width, int height, Component name) {
        super(x, y, width, height, name);
        this.option = option;
        updateDisplayedValue();
    }

    public void onClick(double mouseX, double mouseY) {
        this.option.setValue(!this.option.getValue());
        updateDisplayedValue();
    }

    private void updateDisplayedValue() {
        this.displayedValue = option.getValue() ? Component.nullToEmpty("On") : Component.nullToEmpty("Off");
    }

    public Component getTooltip() {
        return this.option.getTooltip();
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

}
