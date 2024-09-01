package net.vulkanmod.config.gui.option.control;

import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.widget.ControllerWidget;

public interface Controller<T> {
    Option<T> option();

    ControllerWidget<T> createWidget(Dimension<Integer> dim);
}
