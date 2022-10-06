package net.vulkanmod.config;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class SliderWidgetImpl extends SliderWidget {
    private RangeOption option;

    public SliderWidgetImpl(RangeOption option, int x, int y, int width, int height, Text text, double value) {
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
