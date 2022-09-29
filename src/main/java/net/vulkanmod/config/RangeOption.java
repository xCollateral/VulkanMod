package net.vulkanmod.config;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.vulkanmod.config.widget.OptionWidget;
import net.vulkanmod.config.widget.RangeOptionWidget;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RangeOption extends Option<Integer> {
    int min;
    int max;
    int step;

    Function<Integer, String> translator;

    public RangeOption(String name, int min, int max, int step, Function<Integer, String> translator, Consumer<Integer> setter, Supplier<Integer> getter) {
        super(name, setter, getter);
        this.min = min;
        this.max = max;
        this.step = step;
        this.translator = translator;
    }

    public RangeOption(String name, int min, int max, int step, Consumer<Integer> setter, Supplier<Integer> getter) {
        this(name, min, max, step, String::valueOf, setter, getter);
    }

    @Override
    public ClickableWidget createWidget(int x, int y, int width, int height) {
        return new SliderWidgetImpl(this, x, y, width, height, getName(), this.getScaledValue());
    }

    public OptionWidget createOptionWidget(int x, int y, int width, int height) {
        return new RangeOptionWidget(this, x, y, width, height, this.name);
    }

    public Text getName() {
        return Text.of(this.name.asString() + ": " + this.getValue().toString());
    }

    public float getScaledValue() {
        float value = this.getValue();

        return (value - this.min) / (this.max - this.min);
    }

    public void setValue(Integer n) {
        this.newValue = n;
    }

    public void setValue(float f) {
        double n = MathHelper.lerp(f, min, max);

        n = this.step * Math.round(n / this.step);

        this.setValue(Integer.valueOf((int)n));
    }

    public String getDisplayedValue() {
        return this.translator.apply(this.newValue);
    }
}
