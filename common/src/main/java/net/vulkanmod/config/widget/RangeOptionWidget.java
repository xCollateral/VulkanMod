package net.vulkanmod.config.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.RangeOption;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.glfw.GLFW;

public class RangeOptionWidget extends OptionWidget {
    protected double value;
    private RangeOption option;

    private boolean focused;

    public RangeOptionWidget(RangeOption option, int x, int y, int width, int height, Component name) {
        super(x, y, width, height, name);
        this.option = option;
        this.setValue(option.getScaledValue());
    }

    public RangeOptionWidget(RangeOption option, int x, int y, int width, int height, String name) {
        this(option, x, y, width, height, Component.nullToEmpty(name));
    }

    @Override
    protected int getYImage(boolean hovered) {
        return 0;
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, Minecraft client, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = (this.isHovered() ? 2 : 1) * 20;
//        this.drawTexture(matrices, this.controlX + (int)(this.value * (this.controlWidth - 8)), this.y, 0, 46 + i, 4, 20);
//        this.drawTexture(matrices, this.controlX + (int)(this.value * (this.controlWidth - 8)) + 4, this.y, 196, 46 + i, 4, 20);

        int color = this.controlHovered ? ColorUtil.packColorInt(1.0f, 1.0f, 1.0f, 1.0f) : ColorUtil.packColorInt(1.0f, 1.0f, 1.0f, 0.8f);

        guiGraphics.fill(this.controlX + (int)(this.value * (this.controlWidth - 8)), this.y + 20, this.controlX + (int)(this.value * (this.controlWidth - 8)) + 8, this.y, color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setValueFromMouse(mouseX);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean bl;
        boolean bl2 = bl = keyCode == GLFW.GLFW_KEY_LEFT;
        if (bl || keyCode == GLFW.GLFW_KEY_RIGHT) {
            float f = bl ? -1.0f : 1.0f;
            this.setValue(this.value + (double)(f / (float)(this.width - 8)));
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
        this.setValue((mouseX - (double)(this.controlX + 4)) / (double)((this.controlWidth) - 8));
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
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    protected void applyValue() {
        option.setValue((float) this.value);
        this.value = option.getScaledValue();
    }

    private void updateDisplayedValue() {
        this.displayedValue = Component.nullToEmpty(option.getDisplayedValue());
    }

    @Override
    public Component getTooltip() {
        return this.option.getTooltip();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    //    @Override
//    public void playDownSound(SoundManager soundManager) {
//    }
//
//    @Override
//    public void onRelease(double mouseX, double mouseY) {
//        super.playDownSound(MinecraftClient.getInstance().getSoundManager());
//    }

//    @Override
//    protected void applyValue() {
//        ((RangeOption)(option)).setValue((float) this.value);
//        this.displayedValue = Text.of(Float.toString(((RangeOption)(option)).getScaledValue()));
//    }
}
