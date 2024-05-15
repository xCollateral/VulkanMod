package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.widget.OptionWidget;
import net.vulkanmod.config.gui.widget.RangeOptionWidget;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RangeOption extends Option<Integer> {
    int min;
    int max;
    int step;

    public RangeOption(Component name, int min, int max, int step, Function<Integer, Component> translator, Consumer<Integer> setter, Supplier<Integer> getter) {
        super(name, setter, getter, translator);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public RangeOption(Component name, int min, int max, int step, Consumer<Integer> setter, Supplier<Integer> getter) {
        this(name, min, max, step, (i) -> Component.literal(String.valueOf(i)), setter, getter);
    }

    public OptionWidget<?> createOptionWidget(int x, int y, int width, int height) {
        return new RangeOptionWidget(this, x, y, width, height, this.name);
    }

    public Component getName() {
        return Component.nullToEmpty(this.name.getString() + ": " + this.getNewValue().toString());
    }

    public float getScaledValue() {
        float value = this.getNewValue();

        return (value - this.min) / (this.max - this.min);
    }

    public void setValue(float f) {
        double n = Mth.lerp(f, min, max);

        n = this.step * Math.round(n / this.step);

        this.setNewValue((int) n);
    }
}
