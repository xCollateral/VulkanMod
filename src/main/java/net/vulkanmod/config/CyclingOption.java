package net.vulkanmod.config;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.vulkanmod.config.widget.CyclingOptionWidget;
import net.vulkanmod.config.widget.OptionWidget;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CyclingOption<E> extends Option<E> {
    private final Function<E, Text> translator;
    private E[] values;
    private int index;

    public CyclingOption(String name, E[] values, Function<E, Text> translator, Consumer<E> setter, Supplier<E> getter) {
        super(name, setter, getter);
        this.values = values;
        this.translator = translator;

        this.index = ArrayUtils.indexOf(this.values, this.getValue());
    }

    @Override
    public ClickableWidget createWidget(int x, int y, int width, int height) {
        return null;
    }

    @Override
    public OptionWidget createOptionWidget(int x, int y, int width, int height) {
        return new CyclingOptionWidget(this, x, y, width, height, this.name);
    }

    public void updateOption(E[] values, Consumer<E> setter, Supplier<E> getter) {
        this.setter = setter;
        this.getter = getter;

        this.values = values;
        this.index = ArrayUtils.indexOf(this.values, this.getValue());
    }

    public int index() { return this.index;}

    @Override
    public void setValue(E e) {
        this.newValue = e;
    }

    public void prevValue() {
        if(this.index > 0) this.index--;
        this.updateValue();
    }

    public void nextValue() {
        if(this.index < values.length - 1) this.index++;
        this.updateValue();
    }

    private void updateValue() {
        if(this.index >= 0 && this.index < this.values.length)
            setValue(values[this.index]);
    }

    public Text getValueText() {
        return translator.apply(this.newValue);
    }

    public E[] getValues() {
        return values;
    }
}
