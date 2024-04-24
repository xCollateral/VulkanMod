package net.vulkanmod.config.gui;

import net.vulkanmod.config.option.Option;

public class OptionBlock {
    public final String title;
    public final Option<?>[] options;

    public OptionBlock(String title, Option<?>[] options) {
        this.title = title;
        this.options = options;
    }

    public Option<?>[] getOptions() {
        return this.options;
    }

}
