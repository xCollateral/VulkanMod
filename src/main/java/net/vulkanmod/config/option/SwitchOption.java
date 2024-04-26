package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.OptionWidget;
import net.vulkanmod.config.gui.widget.SwitchOptionWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SwitchOption extends Option<Boolean> {
    public SwitchOption(Component name, Consumer<Boolean> setter, Supplier<Boolean> getter) {
        super(name, setter, getter, i -> Component.nullToEmpty(String.valueOf(i)));
    }

    @Override
    public OptionWidget createOptionWidget(int x, int y, int width, int height) {
        return new SwitchOptionWidget(this, x, y, width, height, this.name);
    }

}
