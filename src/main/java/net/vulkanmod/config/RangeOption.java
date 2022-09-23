package net.vulkanmod.config;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RangeOption extends Option<Integer> {

    int min;
    int max;
    int step;

    public RangeOption(String name, int min, int max, int step, Consumer<Integer> setter, Supplier<Integer> getter) {
        super(name, setter, getter);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public ClickableWidget createWidget(int x, int y, int width, int height) {
        return new SliderWidgetImpl(this, x, y, width, height, getText(), this.getScaledValue());
    }

    public Text getText() {
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
}
