package net.vulkanmod.config.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.widget.OptionWidget;
import net.vulkanmod.config.option.Option;

public class VOptionList extends AbstractScrollList {
    int itemHeight;

    public VOptionList(int x, int y, int width, int height, int itemHeight) {
        this.setPosition(x, y, width, height);

        this.width = width;
        this.height = height;

        this.itemWidth = this.width - 7;
        this.itemHeight = itemHeight;
        this.itemMargin = 3;
        this.totalItemHeight = this.itemHeight + this.itemMargin;
    }

    @SuppressWarnings("unused")
    public void addButton(OptionWidget<?> widget) {
        this.addEntry(new Entry(widget, this.itemMargin, null));
    }

    public void addAll(OptionBlock[] blocks) {
        for (OptionBlock block : blocks) {
            int x0 = this.x;
            int width = this.itemWidth;
            int height = this.itemHeight;

            // add a header (this is MOSTLY for the search)
            String title = block.title();
            if (title != null && !title.isEmpty()) {
                this.addEntry(new Entry(null, 8, title));
            }

            var options = block.options();
            for (Option<?> option : options) {
                int margin = this.itemMargin;
                OptionWidget<?> widget = option.getWidget();
                widget.setDimensions(x0, 0, width, height);
                this.addEntry(new Entry(widget, margin, null));
            }

            this.addEntry(new Entry(null, 12, null));
        }
    }

    protected static class Entry extends AbstractScrollList.Entry {
        final String headerTitle;

        private Entry(OptionWidget<?> widget, int margin, String headerTitle) {
            super(widget, margin);
            this.headerTitle = headerTitle;
        }

        @Override
        public void render(int y, int mouseX, int mouseY, boolean updateState, int listX) {
            // if there is a title, RENDER IT!!!
            if (headerTitle != null && !headerTitle.isEmpty()) {
                int headerY = y + 4;
                GuiRenderer.drawString(
                        Minecraft.getInstance().font,
                        Component.literal(headerTitle),
                        listX + 8,
                        headerY,
                        0xFFFFFFFF
                );
                return;
            }

            super.render(y, mouseX, mouseY, updateState, listX);
        }

        @Override
        public int getTotalHeight() {
            if (headerTitle != null && !headerTitle.isEmpty()) {
                return Minecraft.getInstance().font.lineHeight + margin;
            }
            return super.getTotalHeight();
        }
    }
}