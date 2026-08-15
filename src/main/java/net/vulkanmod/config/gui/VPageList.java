package net.vulkanmod.config.gui;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.config.gui.widget.ModIconWidget;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.config.gui.widget.VButtonWidget;
import net.vulkanmod.config.option.OptionPage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class VPageList extends AbstractScrollList {
    List<VButtonWidget> buttons = new ArrayList<>();

    final Consumer<Integer> onSetOptionList;

    public VPageList(int x, int y, int width, int height, Consumer<Integer> onSetOptionList) {
        this.setPosition(x, y, width, height);

        this.width = width;
        this.height = height;

        this.itemWidth = this.width - 7;
        this.itemMargin = 0;
        this.totalItemHeight = VGuiConstants.WIDGET_HEIGHT + this.itemMargin;

        this.onSetOptionList = onSetOptionList;
    }

    @SuppressWarnings("unused")
    public void addButton(VAbstractWidget widget) {
        this.addEntry(new Entry(widget, this.itemMargin));
    }

    private void setOptionList(int finalIdx) {
        this.onSetOptionList.accept(finalIdx);
    }

    public void addAll(List<ModSettingsEntry> modSettingsEntries) {
        int width = VGuiConstants.PAGE_BUTTON_WIDTH;
        int j = 0;
        for (var modEntry : modSettingsEntries) {
            ModIconWidget iconWidget = new ModIconWidget(modEntry.modName, modEntry.getIcon(), x, y, width, 28);
            this.addButton(iconWidget);

            var pages = modEntry.getPages();
            for (OptionPage page : pages) {
                final int finalIdx = j;
                VButtonWidget widget = new VButtonWidget(x, y, width, VGuiConstants.WIDGET_HEIGHT, Component.nullToEmpty(page.name), button -> this.setOptionList(finalIdx));
                widget.setTextLayout(false, 12);
                this.buttons.add(widget);
                this.addButton(widget);
                j++;
            }
        }
    }
}