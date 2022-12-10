package net.vulkanmod.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OptionsList extends ContainerObjectSelectionList<OptionsList.Entry> {

    public OptionsList(Minecraft minecraftClient, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraftClient, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;
    }

    public void addButton(AbstractWidget widget) {
        this.addEntry(Entry.create(widget));
    }

    public int addSingleOptionEntry(OptionInstance<?> option) {
        return this.addEntry(Entry.create(this.minecraft.options, this.width, option));
    }

    public void addOptionEntry(OptionInstance<?> firstOption, @Nullable OptionInstance<?> secondOption) {
        this.addEntry(Entry.create(this.minecraft.options, this.width, firstOption, secondOption));
    }

    public void addOptionEntry(Option<?> option1, Option<?> option2) {
        this.addEntry(Entry.create(this.width, option1, option2));
    }

    public void addAll(OptionInstance<?>[] options) {
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
                this.clickedHeader((int)(mouseX - (double)(this.x0 + this.width / 2 - this.getRowWidth() / 2)), (int)(mouseY - (double)this.y0) + (int)this.getScrollAmount() - 4);
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
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 32;
    }

    @Nullable
    public AbstractWidget getButtonFor(Option option) {
        for (Entry buttonEntry : this.children()) {
            AbstractWidget AbstractWidget = buttonEntry.optionsToButtons.get(option);
            if (AbstractWidget == null) continue;
            return AbstractWidget;
        }
        return null;
    }

    public Optional<AbstractWidget> getHoveredButton(double mouseX, double mouseY) {
        for (Entry buttonEntry : this.children()) {
            for (AbstractWidget AbstractWidget : buttonEntry.buttons) {
                if (!AbstractWidget.isMouseOver(mouseX, mouseY)) continue;
                return Optional.of(AbstractWidget);
            }
        }
        return Optional.empty();
    }

    protected static class Entry
            extends ContainerObjectSelectionList.Entry<Entry> {
        Map<OptionInstance<?>, AbstractWidget> optionsToButtons;
        final List<AbstractWidget> buttons;

        private Entry(Map<OptionInstance<?>, AbstractWidget> optionsToButtons) {
            this.optionsToButtons = optionsToButtons;
            this.buttons = ImmutableList.copyOf(optionsToButtons.values());
        }

        private Entry(List<AbstractWidget> buttons) { this.buttons = buttons; }

        public static Entry create(AbstractWidget button) {
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

        public static Entry create(net.minecraft.client.Options options, int width, OptionInstance<?> option) {
            return new Entry(ImmutableMap.of(option, option.createButton(options, width / 2 - 155, 0, 310)));
        }

        public static Entry create(net.minecraft.client.Options options, int width, OptionInstance<?> firstOption, @Nullable OptionInstance<?> secondOption) {
            AbstractWidget AbstractWidget = firstOption.createButton(options, width / 2 - 155, 0, 150);
            if (secondOption == null) {
                return new Entry(ImmutableMap.of(firstOption, AbstractWidget));
            }
            return new Entry(ImmutableMap.of(firstOption, AbstractWidget, secondOption, secondOption.createButton(options, width / 2 - 155 + 160, 0, 150)));
        }

        @Override
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.buttons.forEach(button -> {
                button.y = y;
                button.render(matrices, mouseX, mouseY, tickDelta);
            });
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.buttons;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.buttons;
        }
    }
}
