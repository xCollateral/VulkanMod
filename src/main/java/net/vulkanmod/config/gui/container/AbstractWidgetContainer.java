package net.vulkanmod.config.gui.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.ConfigScreen;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.widget.AbstractWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWidgetContainer implements ContainerEventHandler, Renderable, NarratableEntry {
    private final Dimension<Integer> dim;
    private final ConfigScreen screen;
    private List<AbstractWidget> children;
    private GuiEventListener focused;
    private boolean dragging;

    public AbstractWidgetContainer(Dimension<Integer> dim, ConfigScreen screen) {
        this.dim = dim.copy();
        this.screen = screen;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        for (AbstractWidget widget : children()) {
            widget.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public @NotNull List<AbstractWidget> children() {
        if (children == null) {
            children = new ArrayList<>();
            addChildren();
        }

        return this.children;
    }

    protected abstract void addChildren();

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public AbstractWidget getHovered() {
        return this.children.stream()
                .filter(AbstractWidget::isHovered)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        this.focused = focused;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.children().forEach(widget -> widget.setFocused(false));
        this.setFocused(null);

        for (GuiEventListener guiEventListener : this.children()) {
            if (guiEventListener.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(guiEventListener);
                if (button == 0) {
                    this.setDragging(true);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public NarratableEntry.@NotNull NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        }

        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        if (this.isFocused()) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.isPointInside(mouseX, mouseY);
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }

    public Dimension<Integer> getDim() {
        return this.dim;
    }

    public ConfigScreen getScreen() {
        return screen;
    }
}
