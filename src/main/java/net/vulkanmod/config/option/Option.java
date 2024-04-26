package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.OptionWidget;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Option<T> {
    protected final Component name;
    protected Component tooltip;

    protected Consumer<T> onApply;
    protected Supplier<T> valueSupplier;

    protected T value;
    protected T newValue;

    protected Function<T, Component> translator;

    protected boolean active;
    protected Runnable onChange;

    public Option(Component name, Consumer<T> setter, Supplier<T> getter, Function<T, Component> translator) {
        this.name = name;

        this.onApply = setter;
        this.valueSupplier = getter;

        this.newValue = this.value = this.valueSupplier.get();

        this.translator = translator;
    }

    public Option(Component name, Consumer<T> setter, Supplier<T> getter) {
        this.name = name;

        this.onApply = setter;
        this.valueSupplier = getter;

        this.newValue = this.value = this.valueSupplier.get();
    }

    public Option<T> setOnApply(Consumer<T> onApply) {
        this.onApply = onApply;
        return this;
    }

    public Option<T> setValueSupplier(Supplier<T> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public Option<T> setTranslator(Function<T, Component> translator) {
        this.translator = translator;
        return this;
    }

    public Option<T> setActive(boolean active) {
        this.active = active;
        return this;
    }

    public abstract OptionWidget<?> createOptionWidget(int x, int y, int width, int height);

    public void setNewValue(T t) {
        this.newValue = t;

        if (onChange != null)
            onChange.run();
    }

    public Component getName() {
        return this.name;
    }

    public void setOnChange(Runnable runnable) {
        onChange = runnable;
    }

    public boolean isChanged() {
        return !this.newValue.equals(this.value);
    }

    public void apply() {
        if(!isChanged())
            return;

        onApply.accept(this.newValue);
        this.value = this.newValue;
    }

    public T getNewValue() {
        return this.newValue;
    }

    public Component getDisplayedValue() {
        return this.translator.apply(this.newValue);
    }

    public Option<T> setTooltip(Component text) {
        this.tooltip = text;
        return this;
    }

    public Component getTooltip() {
        return this.tooltip;
    }
}
