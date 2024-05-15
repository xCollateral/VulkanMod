package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.option.RangeOption;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.glfw.GLFW;

public class RangeOptionWidget extends OptionWidget<RangeOption> {
    protected double value;

    private boolean focused;

    public RangeOptionWidget(RangeOption option, int x, int y, int width, int height, Component name) {
        super(x, y, width, height, name);
        this.setOption(option);
        this.setValue(option.getScaledValue());

    }

    @Override
    protected int getYImage(boolean hovered) {
        return 0;
    }

    @Override
    protected void renderControls(double mouseX, double mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int valueX = this.controlX + (int) (this.value * (this.controlWidth));

        if (this.controlHovered) {
            int halfWidth = 2;
            int halfHeight = 4;

            float y0 = this.y + this.height * 0.5f - 1.0f;
            float y1 = y0 + 2.0f;
            GuiRenderer.fill(this.controlX, y0, this.controlX + this.controlWidth, y1, ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.1f));
            GuiRenderer.fill(this.controlX, y0, valueX - halfWidth, y1, ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.3f));

            int color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.3f);
            GuiRenderer.renderBorder(valueX - halfWidth, y0 - halfHeight, valueX + halfWidth, y1 + halfHeight, 1, color);
//            GuiRenderer.fill(valueX - halfWidth, y0 - 3.0f, valueX + halfWidth, y1 + 3.0f, color);

        } else {
            float y0 = this.y + this.height - 5.0f;
            float y1 = y0 + 1.5f;
            GuiRenderer.fill(this.controlX, y0, this.controlX + this.controlWidth, y1, ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.3f));
            GuiRenderer.fill(this.controlX, y0, valueX, y1, ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.8f));
        }

        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        Font font = Minecraft.getInstance().font;
        var text = this.getDisplayedValue();
        int width = font.width(text);
        int x = this.controlX + this.controlWidth / 2 - width / 2;
//        int x = (int) (this.x + 0.5f * width);
        int y = this.y + (this.height - 9) / 2;
        GuiRenderer.drawString(font, text.getVisualOrderText(), x, y, color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setValueFromMouse(mouseX);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean isLeft = keyCode == GLFW.GLFW_KEY_LEFT;
        boolean isRight = keyCode == GLFW.GLFW_KEY_RIGHT;

        if (isLeft || isRight) {
            float direction = isLeft ? -1.0f : 1.0f;
            this.setValue(this.value + (double) (direction / (float) (this.width - 8)));
        }

        return false;
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    private void setValueFromMouse(double mouseX) {
        this.setValue((mouseX - (double) (this.controlX + 4)) / (double) ((this.controlWidth) - 8));
    }

    private void setValue(double value) {
        double d = this.value;
        this.value = Mth.clamp(value, 0.0, 1.0);
        if (d != this.value) {
            this.applyValue();
        }
        this.updateDisplayedValue();
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.setValueFromMouse(mouseX);
    }

    private void applyValue() {
        option.setValue((float) this.value);
        this.value = option.getScaledValue();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (this.controlHovered) {
            super.playDownSound(Minecraft.getInstance().getSoundManager());
        }
    }
}
