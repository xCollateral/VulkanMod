package net.vulkanmod.config.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.config.gui.widget.OptionWidget;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VOptionList extends GuiElement {
    boolean scrolling = false;
    float scrollAmount = 0.0f;

    int itemWidth;
    int totalItemHeight;
    int itemHeight;
    int itemMargin;
    int listLength = 0;

    Entry focused;

    private final List<Entry> children = new ObjectArrayList<>();

    public VOptionList(int x, int y, int width, int height, int itemHeight) {
        this.setPosition(x, y, width, height);

        this.width = width;
        this.height = height;

        this.itemWidth = (int) (0.95f * this.width);
        this.itemHeight = itemHeight;
        this.itemMargin = 3;
        this.totalItemHeight = this.itemHeight + this.itemMargin;
    }

    public void addButton(OptionWidget widget) {
        this.addEntry(new Entry(widget, this.itemMargin));
    }

    public void addAll(OptionBlock[] blocks) {
        for (int i = 0; i < blocks.length; i++) {
            int x0 = this.x;
            int width = this.itemWidth;
            int height = this.itemHeight;

            var options = blocks[i].options;
            for (int j = 0; j < options.length; j++) {

                int margin = this.itemMargin;

                var option = options[j];
                this.addEntry(new Entry(option.createOptionWidget(x0, 0, width, height), margin));
            }

            this.addEntry(new Entry(null, 12));
        }
    }

    public void addAll(Option<?>[] options) {
        for (int i = 0; i < options.length; i++) {
            int x0 = this.x;
            int width = this.itemWidth;
            int height = this.itemHeight;

            this.addEntry(new Entry(options[i].createOptionWidget(x0, 0, width, height), this.itemMargin));
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

    protected void updateScrollingState(double mouseX, double mouseY, int button) {
        this.scrolling = button == 0 && mouseX >= (double)this.getScrollbarPosition() && mouseX < (double)(this.getScrollbarPosition() + 6);
    }

    protected float getScrollAmount() {
        return scrollAmount;
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
        this.updateScrollingState(mouseX, mouseY, button);
        if (this.isMouseOver(mouseX, mouseY)) {
            Entry entry = this.getEntryAtPos(mouseX, mouseY);
            if (entry != null) {
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    setFocused(entry);
                    entry.setFocused(true);
                    return true;
                }
            } else if (button == 0) {
                return true;
            }

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
        if (this.getFocused() != null && button == 0) {
            return this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        } else if (button == 0 && this.scrolling) {
            if (mouseY < (double)this.y) {
                this.setScrollAmount(0.0);
            } else if (mouseY > (double)this.getBottom()) {
                this.setScrollAmount(this.getMaxScroll());
            } else {
                double maxScroll = this.getMaxScroll();

                if (maxScroll == 0.0)
                    return false;

                int height = this.height;

                int totalLength = this.getTotalLength();
                int k = (int)((float)(height * height) / (float)totalLength);
                double l = Math.max(1.0, maxScroll / (double)(height - k));
                this.setScrollAmount(this.getScrollAmount() + deltaY * l);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double xScroll, double yScroll) {
        this.setScrollAmount(this.getScrollAmount() - yScroll * (double)this.totalItemHeight / 2.0);
        return true;
    }

    public int getMaxScroll() {
        return Math.max(0, this.getTotalLength() - (this.height));
    }

    protected int getTotalLength() {
        return this.listLength;
    }

    public void setScrollAmount(double d) {
        this.scrollAmount = (float) Mth.clamp(d, 0.0, this.getMaxScroll());
    }

    public int getBottom() {
        return this.y + this.height;
    }

    @Nullable
    protected VOptionList.Entry getEntryAtPos(double x, double y) {
        int x0 = this.x;

        if (x > this.getScrollbarPosition() || x < (double)x0)
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

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        GuiRenderer.enableScissor(x, y, width, height);

        this.renderList(mouseX, mouseY);
        GuiRenderer.disableScissor();

        // Scroll bar
        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            int barHeight = (int)((float)((this.getHeight()) * (this.getHeight())) / (float)this.getTotalLength());
            barHeight = Mth.clamp(barHeight, 32, + this.getHeight() - 8);
            int barY = (int)this.getScrollAmount() * (this.getHeight() - barHeight) / maxScroll + this.getY();
            if (barY < this.getY()) {
                barY = this.getY();
            }

            int scrollbarPosition = this.getScrollbarPosition();
            int thickness = 3;

//            int color = ColorUtil.ARGB.pack(0.0f, 0.0f, 0.0f, 0.5f);
            int color = ColorUtil.ARGB.pack(0.8f, 0.8f, 0.8f, 0.2f);
            GuiRenderer.fill(scrollbarPosition, this.getY(), scrollbarPosition + thickness, this.getY() + this.getHeight(), color);

            color = ColorUtil.ARGB.pack(0.3f, 0.0f, 0.0f, 0.6f);
//            color = ColorUtil.ARGB.pack(0.8f, 0.8f, 0.8f, 0.5f);
            GuiRenderer.fill(scrollbarPosition, barY, scrollbarPosition + thickness, barY + barHeight, color);
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
        public void setFocused(boolean bl) {
            widget.setFocused(bl);
        }

        @Override
        public boolean isFocused() {
            return false;
        }
    }
}
