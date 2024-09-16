package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.GuiElement;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.option.CyclingOption;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.render.util.MathUtil;
import net.vulkanmod.vulkan.util.ColorUtil;

import java.util.Objects;

public abstract class OptionWidget<O extends Option<?>> extends VAbstractWidget
        implements NarratableEntry {

    public int controlX;
    public int controlWidth;
    private final Component name;
    protected Component displayedValue;

    protected boolean controlHovered;

    O option;

    public OptionWidget(int x, int y, int width, int height, Component name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.displayedValue = Component.literal("N/A");

        this.controlWidth = Math.min((int) (width * 0.5f) - 8, 120);
        this.controlX = this.x + this.width - this.controlWidth - 8;
    }

    public void setOption(O option) {
        this.option = option;
    }

    public void render(double mouseX, double mouseY) {
        if (!this.visible) {
            return;
        }

        this.updateDisplayedValue();

        this.controlHovered = mouseX >= this.controlX && mouseY >= this.y && mouseX < this.controlX + this.controlWidth && mouseY < this.y + this.height;
        this.renderWidget(mouseX, mouseY);
    }

    public void updateState() {

    }

    public void renderWidget(double mouseX, double mouseY) {
        Minecraft minecraftClient = Minecraft.getInstance();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = this.getYImage(this.isHovered());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int xPadding = 0;
        int yPadding = 0;

        int color = ColorUtil.ARGB.pack(0.0f, 0.0f, 0.0f, 0.45f);
        GuiRenderer.fill(this.x - xPadding, this.y - yPadding, this.x + this.width + xPadding, this.y + this.height + yPadding, color);

        this.renderHovering(0, 0);

        color = this.active ? 0xFFFFFF : 0xA0A0A0;
//        j = 0xB0f0d0a0;

        Font textRenderer = minecraftClient.font;
        GuiRenderer.drawString(textRenderer, this.getName().getVisualOrderText(), this.x + 8, this.y + (this.height - 8) / 2, color);

        RenderSystem.enableBlend();

        this.renderControls(mouseX, mouseY);
    }

    protected int getYImage(boolean hovered) {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (hovered) {
            i = 2;
        }
        return i;
    }

    public boolean isHovered() {
        return this.hovered || this.focused;
    }

    protected abstract void renderControls(double mouseX, double mouseY);

    public abstract void onClick(double mouseX, double mouseY);

    public abstract void onRelease(double mouseX, double mouseY);

    protected abstract void onDrag(double mouseX, double mouseY, double deltaX, double deltaY);

    protected boolean isValidClickButton(int button) {
        return button == 0;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isValidClickButton(button)) {
            this.onDrag(mouseX, mouseY, deltaX, deltaY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) {
            return false;
        }

        if (this.isValidClickButton(button) && this.clicked(mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button)) {
            this.onRelease(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.controlX && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    public Component getName() {
        return this.name;
    }

    public Component getDisplayedValue() {
        return this.displayedValue;
    }

    protected void updateDisplayedValue() {
        this.displayedValue = this.option.getDisplayedValue();
    }

    public Component getTooltip() {
        return this.option.getTooltip();
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        }
        if (this.hovered) {
            return NarrationPriority.HOVERED;
        }
        return NarrationPriority.NONE;
    }

    @Override
    public final void updateNarration(NarrationElementOutput narrationElementOutput) {
    }

    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

}
