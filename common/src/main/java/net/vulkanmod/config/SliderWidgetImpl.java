package net.vulkanmod.config;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class SliderWidgetImpl extends AbstractSliderButton {
    private RangeOption option;

    public SliderWidgetImpl(RangeOption option, int x, int y, int width, int height, Component text, double value) {
        super(x, y, width, height, text, value);
        this.option = option;
    }

    @Override
    protected void updateMessage() {
        this.setMessage(option.getName());
    }

    @Override
    protected void applyValue() {
        option.setValue((float) this.value);
        this.value = option.getScaledValue();
    }
}
