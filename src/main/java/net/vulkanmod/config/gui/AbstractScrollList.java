package net.vulkanmod.config.gui;

import com.mojang.blaze3d.opengl.GlStateManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public abstract class AbstractScrollList extends GuiElement {
    protected final List<Entry> children = new ObjectArrayList<>();
    protected boolean scrolling = false;
    protected float scrollAmount = 0.0f;
    protected int itemWidth;
    protected int totalItemHeight;
    protected int itemMargin;
    protected int listLength = 0;
    protected Entry focused;

    protected AbstractScrollList() {
    }

    protected void addEntry(Entry entry) {
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

    protected int getItemCount() {
        return this.children.size();
    }

    protected GuiEventListener getFocused() {
        return focused;
    }

    protected void setFocused(Entry focussed) {
        this.focused = focussed;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        this.updateScrollingState(event.x(), event.button());
        if (this.isMouseOver(event.x(), event.y())) {
            Entry entry = this.getEntryAtPos(event.x(), event.y());
            if (entry != null && entry.mouseClicked(event, bl)) {
                setFocused(entry);
                entry.setFocused(true);
                return true;
            }

            return event.button() == 0;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.isValidClickButton(event.button())) {
            Entry entry = this.getEntryAtPos(event.x(), event.y());
            if (entry != null) {
                if (entry.mouseReleased(event)) {
                    entry.setFocused(false);
                    setFocused(null);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != 0) {
            return false;
        }

        if (this.getFocused() != null) {
            return this.getFocused().mouseDragged(event, deltaX, deltaY);
        }

        if (!this.scrolling) {
            return false;
        }

        double maxScroll = this.getMaxScroll();
        if (event.y() < this.y) {
            this.setScrollAmount(0.0);
        } else if (event.y() > this.getBottom()) {
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
    protected Entry getEntryAtPos(double x, double y) {
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
        GuiRenderer.enableScissor(x, y, x + width, y + height);

        this.renderList(mouseX, mouseY);
        GuiRenderer.disableScissor();

        // Scroll bar
        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            GlStateManager._enableBlend();

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
        return this.x + this.width;
    }

    public VAbstractWidget getHoveredWidget(double mouseX, double mouseY) {
        if (this.focused != null)
            return focused.widget;

        if (!this.isMouseOver(mouseX, mouseY))
            return null;

        for (Entry entry : this.children) {
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
            Entry entry = this.getEntry(j);

            if (rowTop + entry.getTotalHeight() >= this.y && rowTop <= (this.y + this.height)) {
                boolean updateState = this.focused == null;
                entry.render(rowTop, mouseX, mouseY, updateState, this.x);
            }

            rowTop += entry.getTotalHeight();
        }
    }

    protected Entry getEntry(int j) {
        return this.children.get(j);
    }

    protected boolean isValidClickButton(int i) {
        return i == 0;
    }

    protected static class Entry implements GuiEventListener {
        final VAbstractWidget widget;
        final int margin;

        protected Entry(VAbstractWidget widget, int margin) {
            this.widget = widget;
            this.margin = margin;
        }

        public void render(int y, int mouseX, int mouseY, boolean updateState, int listX) {
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

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean bl) {
            if (widget == null) return false;
            return widget.mouseClicked(event, bl);
        }

        @Override
        public boolean mouseReleased(@NonNull MouseButtonEvent event) {
            if (widget == null) return false;
            return widget.mouseReleased(event);
        }

        @Override
        public boolean mouseDragged(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
            if (widget == null) return false;
            return widget.mouseDragged(event, deltaX, deltaY);
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void setFocused(boolean bl) {
            if (widget != null)
                widget.setFocused(bl);
        }
    }
}