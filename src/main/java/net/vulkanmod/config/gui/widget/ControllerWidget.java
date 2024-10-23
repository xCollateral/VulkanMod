package net.vulkanmod.config.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ControllerWidget<T> extends AbstractWidget {
    private final Option<T> option;
    private final Dimension<Integer> controllerDim;
    private final int optionStateColor;
    private Component displayedValue;

    public ControllerWidget(@NotNull Option<T> option, Dimension<Integer> dim) {
        super(dim);

        this.option = option;

        this.optionStateColor = option.isActive()
                ? GuiConstants.COLOR_WHITE
                : GuiConstants.COLOR_GRAY;

        int controlWidth = Math.min((int) (dim.width() * 0.5f) - 8, 120);
        int controlX = dim.xLimit() - controlWidth - 8;

        this.controllerDim = Dimension.ofInt(
                controlX, getDim().y(),
                controlWidth, getDim().height()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        this.updateDisplayedValue();

        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_BLACK, 0.45f);

        GuiRenderer.fill(
                getDim().x(), getDim().y(),
                getDim().xLimit(), getDim().yLimit(),
                backgroundColor);
        if (option.isChanged()) {
            GuiRenderer.drawString(
                    MutableComponent.create(option.name().getContents()).withStyle(ChatFormatting.ITALIC),
                    getDim().x() + 8, getDim().centerY() - 4,
                    getOptionStateColor());
        } else {
            GuiRenderer.drawString(
                    option.name(),
                    getDim().x() + 8, getDim().centerY() - 4,
                    getOptionStateColor());
        }
    }

    public Option<T> getOption() {
        return option;
    }

    public Dimension<Integer> getControllerDim() {
        return controllerDim;
    }

    public Component getDisplayedValue() {
        return this.displayedValue;
    }

    public void setDisplayedValue(Component displayedValue) {
        this.displayedValue = displayedValue;
    }

    protected void updateDisplayedValue() {
        setDisplayedValue(option.displayedValue());
    }

    public int getOptionStateColor() {
        return optionStateColor;
    }

    @Override
    public boolean isActive() {
        return option.isActive();
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!option.isActive())
            return null;
        return super.nextFocusPath(event);
    }
}
