package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.CyclingOptionWidget;
import net.vulkanmod.config.gui.widget.OptionWidget;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CyclingOption<E> extends Option<E> {
    private E[] values;
    private int index;

    public CyclingOption(Component name, E[] values, Consumer<E> setter, Supplier<E> getter) {
        super(name, setter, getter);
        this.values = values;

        this.index = this.findNewValueIndex();
    }

    @Override
    public OptionWidget<?> createOptionWidget(int x, int y, int width, int height) {
        return new CyclingOptionWidget(this, x, y, width, height, this.name);
    }

    public void updateOption(E[] values, Consumer<E> setter, Supplier<E> getter) {
        this.onApply = setter;
        this.valueSupplier = getter;

        this.values = values;
        this.index = ArrayUtils.indexOf(this.values, this.getNewValue());
    }

    public int index() { return this.index; }

    public void setValues(E[] values) {
        this.values = values;
    }

    public void prevValue() {
        if(this.index > 0)
            this.index--;
        this.updateValue();
    }

    public void nextValue() {
        if(this.index < values.length - 1)
            this.index++;
        this.updateValue();
    }

    private void updateValue() {
        if(this.index >= 0 && this.index < this.values.length) {
            this.newValue = values[this.index];

            if (onChange != null)
                onChange.run();
        }
    }

    public void setNewValue(E e) {
        super.setNewValue(e);
        this.index = findNewValueIndex();
    }

    private int findNewValueIndex() {
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i].equals(this.newValue))
                return i;
        }
        return -1;
    }

    public E[] getValues() {
        return values;
    }
}
