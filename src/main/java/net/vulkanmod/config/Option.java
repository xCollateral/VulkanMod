package net.vulkanmod.config;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.widget.OptionWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Option<T> {
    protected final Component name;
    protected Component tooltip;

    protected Consumer<T> setter;
    protected Supplier<T> getter;

    protected T value;
    protected T newValue;

    public Option(String name, Consumer<T> setter, Supplier<T> getter) {
        this.name = Component.literal(name);

        this.setter = setter;
        this.getter = getter;

        this.newValue = this.value = this.getter.get();
    }

    public abstract AbstractWidget createWidget(int x, int y, int width, int height);

    public abstract OptionWidget createOptionWidget(int x, int y, int width, int height);

    public abstract void setValue(T t);

    public Component getName() {
        return this.name;
    }

    public boolean isModified() {
        return !this.newValue.equals(this.value);
    }

    protected void apply() {
        if(!isModified()) return;
        setter.accept(this.newValue);
        this.value = this.newValue;
    }

    public T getValue() {
        return this.newValue;
    }

    public Option<T> setTooltip(Component text) {
        this.tooltip = text;
        return this;
    }

    public Component getTooltip() {
        return this.tooltip;
    }
}
