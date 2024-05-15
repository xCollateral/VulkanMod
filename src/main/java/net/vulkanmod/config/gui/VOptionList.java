package net.vulkanmod.config.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.widget.OptionWidget;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VOptionList extends GuiElement {
    private final List<Entry> children = new ObjectArrayList<>();
    boolean scrolling = false;
    float scrollAmount = 0.0f;
    int itemWidth;
    int totalItemHeight;
    int itemHeight;
    int itemMargin;
    int listLength = 0;
    Entry focused;

    public VOptionList(int x, int y, int width, int height, int itemHeight) {
        this.setPosition(x, y, width, height);

        this.width = width;
        this.height = height;

        this.itemWidth = (int) (0.95f * this.width);
        this.itemHeight = itemHeight;
        this.itemMargin = 3;
        this.totalItemHeight = this.itemHeight + this.itemMargin;
    }

    public void addButton(OptionWidget<?> widget) {
        this.addEntry(new Entry(widget, this.itemMargin));
    }

    public void addAll(OptionBlock[] blocks) {
        for (OptionBlock block : blocks) {
            int x0 = this.x;
            int width = this.itemWidth;
            int height = this.itemHeight;

            var options = block.options();
            for (Option<?> option : options) {

                int margin = this.itemMargin;

                this.addEntry(new Entry(option.createOptionWidget(x0, 0, width, height), margin));
            }

            this.addEntry(new Entry(null, 12));
        }
    }

    public void addAll(Option<?>[] options) {
        for (Option<?> option : options) {
            int x0 = this.x;
            int width = this.itemWidth;
            int height = this.itemHeight;

            this.addEntry(new Entry(option.createOptionWidget(x0, 0, width, height), this.itemMargin));
//            this.addEntry(new Entry(options[i].createOptionWidget(width / 2 - 155, 0, 200, 20)));
        }
    }

    private void addEntry(Entry entry) {
        this.children.add(entry);

        this.listLength += entry.getTotalHeight();
    }

    public void clearEntries() {
        this.listLength = 0;
        this.children.clear();
    }

    protected void updateScrollingState(double mouseX, int button) {
        this.scrolling = button == 0 && mouseX >= (double) this.getScrollbarPosition() && mouseX < (double) (this.getScrollbarPosition() + 6);
    }

    protected float getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(double d) {
        this.scrollAmount = (float) Mth.clamp(d, 0.0, this.getMaxScroll());
    }

    private int getItemCount() {
        return this.children.size();
    }

    GuiEventListener getFocused() {
        return focused;
    }

    void setFocused(Entry focussed) {
        this.focused = focussed;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateScrollingState(mouseX, button);
        if (this.isMouseOver(mouseX, mouseY)) {
            Entry entry = this.getEntryAtPos(mouseX, mouseY);
            if (entry != null && entry.mouseClicked(mouseX, mouseY, button)) {
                setFocused(entry);
                entry.setFocused(true);
                return true;
            }

            return button == 0;
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button)) {
            Entry entry = this.getEntryAtPos(mouseX, mouseY);
            if (entry != null) {
                if (entry.mouseReleased(mouseX, mouseY, button)) {
                    entry.setFocused(false);
                    setFocused(null);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0) {
            return false;
        }

        if (this.getFocused() != null) {
            return this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        if (!this.scrolling) {
            return false;
        }

        double maxScroll = this.getMaxScroll();
        if (mouseY < this.y) {
            this.setScrollAmount(0.0);
        } else if (mouseY > this.getBottom()) {
            this.setScrollAmount(maxScroll);
        } else if (maxScroll > 0.0) {
            double barHeight = (double) this.height * this.height / this.getTotalLength();
            double scrollFactor = Math.max(1.0, maxScroll / (this.height - barHeight));
            this.setScrollAmount(this.getScrollAmount() + deltaY * scrollFactor);
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double xScroll, double yScroll) {
        this.setScrollAmount(this.getScrollAmount() - yScroll * (double) this.totalItemHeight / 2.0);
        return true;
    }

    public int getMaxScroll() {
        return Math.max(0, this.getTotalLength() - (this.height));
    }

    protected int getTotalLength() {
        return this.listLength;
    }

    public int getBottom() {
        return this.y + this.height;
    }

    @Nullable
    protected VOptionList.Entry getEntryAtPos(double x, double y) {
        int x0 = this.x;

        if (x > this.getScrollbarPosition() || x < (double) x0)
            return null;

        for (var entry : this.children) {
            VAbstractWidget widget = entry.widget;
            if (widget != null && y >= widget.y && y <= widget.y + widget.height) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public void updateState(double mX, double mY) {
        if (this.focused != null)
            return;

        super.updateState(mX, mY);
    }

    public void renderWidget(int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        GuiRenderer.enableScissor(x, y, width, height);

        this.renderList(mouseX, mouseY);
        GuiRenderer.disableScissor();

        // Scroll bar
        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            int height = this.getHeight();
            int totalLength = this.getTotalLength();
            int barHeight = (int) ((float) (height * height) / totalLength);
            barHeight = Mth.clamp(barHeight, 32, height - 8);

            int scrollAmount = (int) this.getScrollAmount();
            int barY = scrollAmount * (height - barHeight) / maxScroll + this.getY();
            barY = Math.max(barY, this.getY());

            int scrollbarPosition = this.getScrollbarPosition();
            int thickness = 3;

            int backgroundColor = ColorUtil.ARGB.pack(0.8f, 0.8f, 0.8f, 0.2f);
            GuiRenderer.fill(scrollbarPosition, this.getY(), scrollbarPosition + thickness, this.getY() + height, backgroundColor);

            int barColor = ColorUtil.ARGB.pack(0.3f, 0.0f, 0.0f, 0.6f);
            GuiRenderer.fill(scrollbarPosition, barY, scrollbarPosition + thickness, barY + barHeight, barColor);
        }
    }

    protected int getScrollbarPosition() {
        return this.x + this.itemWidth + 5;
    }

    public VAbstractWidget getHoveredWidget(double mouseX, double mouseY) {
        if (this.focused != null)
            return focused.widget;

        if (!this.isMouseOver(mouseX, mouseY))
            return null;

        for (VOptionList.Entry entry : this.children) {
            var widget = entry.widget;

            if (widget == null || !widget.isMouseOver(mouseX, mouseY))
                continue;
            return widget;
        }
        return null;
    }

    protected void renderList(int mouseX, int mouseY) {
        int itemCount = this.getItemCount();

        int rowTop = this.y - (int) this.getScrollAmount();
        for (int j = 0; j < itemCount; ++j) {
            int rowBottom = rowTop + this.itemHeight;

            VOptionList.Entry entry = this.getEntry(j);
            if (rowBottom >= this.y && rowTop <= (this.y + this.height)) {
                boolean updateState = this.focused == null;

                entry.render(rowTop, mouseX, mouseY, updateState);
            }

            rowTop += entry.getTotalHeight();
        }
    }

    private Entry getEntry(int j) {
        return this.children.get(j);
    }

    protected boolean isValidClickButton(int i) {
        return i == 0;
    }

    protected static class Entry implements GuiEventListener {
        final VAbstractWidget widget;
        final int margin;

        private Entry(OptionWidget<?> widget, int margin) {
            this.widget = widget;
            this.margin = margin;
        }

        public void render(int y, int mouseX, int mouseY, boolean updateState) {
            if (widget == null)
                return;

            widget.y = y;

            if (updateState)
                widget.updateState(mouseX, mouseY);

            widget.render(mouseX, mouseY);
        }

        public int getTotalHeight() {
            if (widget != null)
                return widget.height + margin;
            else
                return margin;
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return widget.mouseClicked(mouseX, mouseY, button);
        }

        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return widget.mouseReleased(mouseX, mouseY, button);
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void setFocused(boolean bl) {
            widget.setFocused(bl);
        }
    }
}
