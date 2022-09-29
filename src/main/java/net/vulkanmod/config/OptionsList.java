package net.vulkanmod.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OptionsList extends ElementListWidget<OptionsList.Entry> {

    public OptionsList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraftClient, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;
    }

    public void addButton(ClickableWidget widget) {
        this.addEntry(Entry.create(widget));
    }

    public int addSingleOptionEntry(net.minecraft.client.option.Option option) {
        return this.addEntry(Entry.create(this.client.options, this.width, option));
    }

    public void addOptionEntry(net.minecraft.client.option.Option firstOption, @Nullable net.minecraft.client.option.Option secondOption) {
        this.addEntry(Entry.create(this.client.options, this.width, firstOption, secondOption));
    }

    public void addOptionEntry(Option<?> option1, Option<?> option2) {
        this.addEntry(Entry.create(this.width, option1, option2));
    }

    public void addAll(net.minecraft.client.option.Option[] options) {
        for (int i = 0; i < options.length; i += 2) {
            this.addOptionEntry(options[i], i < options.length - 1 ? options[i + 1] : null);
        }

    }

    public void addAll(Option<?>[] options) {
        for (int i = 0; i < options.length; i += 2) {
            this.addOptionEntry(options[i], i < options.length - 1 ? options[i + 1] : null);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateScrollingState(mouseX, mouseY, button);
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        } else {
            OptionsList.Entry entry = this.getEntryAtPosition(mouseX, mouseY);
            if (entry != null) {
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(entry);
                    this.setDragging(true);
                    return true;
                }
            } else if (button == 0) {
                this.clickedHeader((int)(mouseX - (double)(this.left + this.width / 2 - this.getRowWidth() / 2)), (int)(mouseY - (double)this.top) + (int)this.getScrollAmount() - 4);
                return true;
            }

            return false;
        }
    }

    @Override
    public int getRowWidth() {
        return 400;
    }

    @Override
    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 32;
    }

    @Nullable
    public ClickableWidget getButtonFor(Option option) {
        for (Entry buttonEntry : this.children()) {
            ClickableWidget clickableWidget = buttonEntry.optionsToButtons.get(option);
            if (clickableWidget == null) continue;
            return clickableWidget;
        }
        return null;
    }

    public Optional<ClickableWidget> getHoveredButton(double mouseX, double mouseY) {
        for (Entry buttonEntry : this.children()) {
            for (ClickableWidget clickableWidget : buttonEntry.buttons) {
                if (!clickableWidget.isMouseOver(mouseX, mouseY)) continue;
                return Optional.of(clickableWidget);
            }
        }
        return Optional.empty();
    }

    protected static class Entry
            extends ElementListWidget.Entry<Entry> {
        Map<net.minecraft.client.option.Option, ClickableWidget> optionsToButtons;
        final List<ClickableWidget> buttons;

        private Entry(Map<net.minecraft.client.option.Option, ClickableWidget> optionsToButtons) {
            this.optionsToButtons = optionsToButtons;
            this.buttons = ImmutableList.copyOf(optionsToButtons.values());
        }

        private Entry(List<ClickableWidget> buttons) { this.buttons = buttons; }

        public static Entry create(ClickableWidget button) {
            return new Entry(ImmutableList.of(button));
        }

        public static Entry create(int width, Option option1, @Nullable Option option2) {
            if (option2 == null) {
                return new Entry(ImmutableList.of(option1.createWidget(width / 2 - 155, 0, 150, 20)));
            }
            return new Entry(ImmutableList.of(option1.createWidget(width / 2 - 155, 0, 150, 20),
                    option2.createWidget(width / 2 - 155 + 160, 0, 150, 20)
            ));
        }

        public static Entry create(GameOptions options, int width, net.minecraft.client.option.Option option) {
            return new Entry(ImmutableMap.of(option, option.createButton(options, width / 2 - 155, 0, 310)));
        }

        public static Entry create(GameOptions options, int width, net.minecraft.client.option.Option firstOption, @Nullable net.minecraft.client.option.Option secondOption) {
            ClickableWidget clickableWidget = firstOption.createButton(options, width / 2 - 155, 0, 150);
            if (secondOption == null) {
                return new Entry(ImmutableMap.of(firstOption, clickableWidget));
            }
            return new Entry(ImmutableMap.of(firstOption, clickableWidget, secondOption, secondOption.createButton(options, width / 2 - 155 + 160, 0, 150)));
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.buttons.forEach(button -> {
                button.y = y;
                button.render(matrices, mouseX, mouseY, tickDelta);
            });
        }

        @Override
        public List<? extends Element> children() {
            return this.buttons;
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return this.buttons;
        }
    }
}
