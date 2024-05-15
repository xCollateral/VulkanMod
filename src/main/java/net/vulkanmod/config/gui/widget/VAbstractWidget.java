package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.vulkanmod.config.gui.GuiElement;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.vulkan.util.ColorUtil;

public abstract class VAbstractWidget extends GuiElement {
    public boolean active = true;
    public boolean visible = true;
    public boolean focused;

    protected Component message;

    public void render(double mX, double mY) {
        this.updateState(mX, mY);
        this.renderWidget(mX, mY);
    }

    public void renderWidget(double mX, double mY) {
    }

    public void onClick(double mX, double mY) {
    }

    public void onRelease(double mX, double mY) {
    }

    protected void onDrag(double mX, double mY, double f, double g) {
    }

    protected void renderHovering(int xPadding, int yPadding) {
        float hoverMultiplier = this.getHoverMultiplier(200);

        if (hoverMultiplier > 0.0f) {
//            int color = ColorUtil.ARGB.pack(0.5f, 0.5f, 0.5f, hoverMultiplier * 0.2f);
            int color = ColorUtil.ARGB.pack(0.3f, 0.0f, 0.0f, hoverMultiplier * 0.2f);
//            int color = ColorUtil.ARGB.multiplyAlpha(VOptionScreen.RED, hoverMultiplier);
            GuiRenderer.fill(this.x - xPadding, this.y - yPadding, this.x + this.width + xPadding, this.y + this.height + yPadding, color);

//            color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, hoverMultiplier * 0.8f);
            color = ColorUtil.ARGB.pack(0.3f, 0.0f, 0.0f, hoverMultiplier * 0.8f);

            int x0 = this.x - xPadding;
            int x1 = this.x + this.width + xPadding;
            int y0 = this.y - yPadding;
            int y1 = this.y + height + yPadding;
            int border = 1;

            GuiRenderer.renderBorder(x0, y0, x1, y1, border, color);
        }
    }

    @Override
    public boolean mouseClicked(double mX, double mY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean bl = this.clicked(mX, mY);
                if (bl) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onClick(mX, mY);
                    return true;
                }
            }

        }
        return false;
    }

    protected boolean clicked(double mX, double mY) {
        return this.active
                && this.visible
                && mX >= (double)this.getX()
                && mY >= (double)this.getY()
                && mX < (double)(this.getX() + this.getWidth())
                && mY < (double)(this.getY() + this.getHeight());
    }

    @Override
    public boolean mouseReleased(double mX, double mY, int button) {
        if (this.isValidClickButton(button)) {
            this.onRelease(mX, mY);
            return true;
        } else {
            return false;
        }
    }

    protected boolean isValidClickButton(int button) {
        return button == 0;
    }

    @Override
    public boolean mouseDragged(double mX, double mY, int button, double f, double g) {
        if (this.isValidClickButton(button)) {
            this.onDrag(mX, mY, f, g);
            return true;
        } else {
            return false;
        }
    }

    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public Component getTooltip() {
        return null;
    }
}
