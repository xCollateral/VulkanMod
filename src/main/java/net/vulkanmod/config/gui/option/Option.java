package net.vulkanmod.config.gui.option;

import net.minecraft.network.chat.Component;
import net.minecraft.util.OptionEnum;
import net.vulkanmod.config.gui.option.control.Controller;
import net.vulkanmod.config.gui.util.Binding;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Option<T> {
    private final Component name;
    private final Component tooltip;
    private final Binding<T> binding;
    private final Controller<T> controller;
    private final Function<T, Component> translator;
    private T value;
    private T pendingValue;
    private boolean active = true;

    private Option(Component name,
                   Component tooltip,
                   Binding<T> binding,
                   Function<Option<T>, Controller<T>> controllerGetter,
                   Function<T, Component> translator,
                   boolean active) {
        this.name = name;
        this.tooltip = tooltip;
        this.binding = binding;
        this.controller = controllerGetter.apply(this);
        this.translator = translator;
        this.active = active;
        this.value = this.pendingValue = binding.get();
    }

    public static <T> Option.@NotNull Builder<T> createBuilder() {
        return new Builder<>();
    }

    public Component name() {
        return name;
    }

    public Component tooltip() {
        return tooltip;
    }

    public Binding<T> binding() {
        return binding;
    }

    public Controller<T> controller() {
        return controller;
    }

    public Function<T, Component> translator() {
        return translator;
    }

    public T value() {
        return value;
    }

    public Component displayedValue() {
        return translator.apply(pendingValue);
    }

    public T pendingValue() {
        return pendingValue;
    }

    public void setPendingValue(T pendingValue) {
        this.pendingValue = pendingValue;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isChanged() {
        return !this.pendingValue.equals(this.value);
    }

    public void apply() {
        if (!isChanged())
            return;

        binding.set(this.pendingValue);
        this.value = this.pendingValue;
    }

    public void undo() {
        if (!isChanged())
            return;

        binding.set(this.value);
        this.pendingValue = value;
    }

    public static class Builder<T> {
        private Component name;
        private Component tooltip;
        private Binding<T> binding;
        private Function<Option<T>, Controller<T>> controllerGetter;
        private Function<T, Component> translator = value -> value instanceof OptionEnum
                ? ((OptionEnum) value).getCaption()
                : Component.literal(String.valueOf(value));
        private boolean active = true;

        private Builder() {
        }

        public Builder<T> name(@NotNull Component name) {
            this.name = name;
            return this;
        }

        public Builder<T> tooltip(Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder<T> binding(@NotNull Supplier<T> getter, @NotNull Consumer<T> setter) {
            Validate.notNull(getter, "`getter` must not be null");
            Validate.notNull(setter, "`setter` must not be null");

            this.binding = new Binding<>(getter, setter);
            return this;
        }

        public Builder<T> controller(Function<Option<T>, Controller<T>> controller) {
            this.controllerGetter = controller;
            return this;
        }

        public Builder<T> translator(Function<T, Component> translator) {
            this.translator = translator;
            return this;
        }

        public Builder<T> active(boolean active) {
            this.active = active;
            return this;
        }

        public Option<T> build() {
            Validate.notNull(name, "`name` must not be null");
            Validate.notNull(binding, "`binding` must not be null");
            Validate.notNull(controllerGetter, "`controller` must not be null");

            return new Option<>(name, tooltip, binding, controllerGetter, translator, active);
        }
    }

}
