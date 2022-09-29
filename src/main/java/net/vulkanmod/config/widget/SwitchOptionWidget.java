package net.vulkanmod.config.widget;

import net.minecraft.text.Text;
import net.vulkanmod.config.SwitchOption;

public class SwitchOptionWidget extends OptionWidget {
    SwitchOption option;

    public SwitchOptionWidget(SwitchOption option, int x, int y, int width, int height, Text name) {
        super(x, y, width, height, name);
        this.option = option;
        updateDisplayedValue();
    }

    public void onClick(double mouseX, double mouseY) {
        this.option.setValue(!this.option.getValue());
        updateDisplayedValue();
    }

    private void updateDisplayedValue() {
        this.displayedValue = option.getValue() ? Text.of("On") : Text.of("Off");
    }

    public Text getTooltip() {
        return this.option.getTooltip();
    }

}
