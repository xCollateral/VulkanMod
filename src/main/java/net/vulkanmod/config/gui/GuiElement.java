package net.vulkanmod.config.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

public abstract class GuiElement implements GuiEventListener, NarratableEntry {

    protected int width;
    protected int height;
    public int x;
    public int y;

    protected boolean hovered;
    protected long hoverStartTime;
    protected int hoverTime;
    protected long hoverStopTime;

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void updateState(double mX, double mY) {
        // Update hover
        if (isMouseOver(mX, mY)) {
            if (!this.hovered) {
                this.hoverStartTime = Util.getMillis();
            }

            this.hovered = true;
            this.hoverTime = (int) (Util.getMillis() - this.hoverStartTime);
        } else {
            if (this.hovered) {
                this.hoverStopTime = Util.getMillis();
            }
            this.hovered = false;
            this.hoverTime = 0;
        }
    }

    public float getHoverMultiplier(float time) {
        if (this.hovered) {
            return Math.min(((this.hoverTime) / time), 1.0f);
        }
        else {
            int delta = (int) (Util.getMillis() - this.hoverStopTime);
            return Math.max(1.0f - (delta / time), 0.0f);
        }
    }

    @Override
    public void mouseMoved(double d, double e) {
        GuiEventListener.super.mouseMoved(d, e);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        return GuiEventListener.super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseReleased(double d, double e, int i) {
        return GuiEventListener.super.mouseReleased(d, e, i);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        return GuiEventListener.super.mouseDragged(d, e, i, f, g);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        return GuiEventListener.super.mouseScrolled(d, e, f, g);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return GuiEventListener.super.keyPressed(i, j, k);
    }

    @Override
    public boolean keyReleased(int i, int j, int k) {
        return GuiEventListener.super.keyReleased(i, j, k);
    }

    @Override
    public boolean charTyped(char c, int i) {
        return GuiEventListener.super.charTyped(c, i);
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
        return GuiEventListener.super.nextFocusPath(focusNavigationEvent);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseY >= this.y
                && mouseX <= (this.x + this.width) && mouseY <= (this.y + this.height);
    }

    @Nullable
    @Override
    public ComponentPath getCurrentFocusPath() {
        return GuiEventListener.super.getCurrentFocusPath();
    }

    @Override
    public ScreenRectangle getRectangle() {
        return GuiEventListener.super.getRectangle();
    }

    @Override
    public void setFocused(boolean bl) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
