package net.vulkanmod.config;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Option<T> {
    protected final Text name;
    protected final Consumer<T> setter;
    protected final Supplier<T> getter;

    protected T value;
    protected T newValue;

    public Option(String name, Consumer<T> setter, Supplier<T> getter) {
        this.name = new LiteralText(name);
        this.setter = setter;
        this.getter = getter;

        this.newValue = this.value = this.getter.get();
    }

    public abstract ClickableWidget createWidget(int x, int y, int width, int height);

    public abstract Text getText();

    public abstract void setValue(T t);

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
}
