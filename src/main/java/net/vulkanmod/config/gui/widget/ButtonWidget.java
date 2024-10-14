package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;

public class ButtonWidget extends AbstractWidget {
    private final Runnable onPress;
    private final float alpha = 1.0f;
    private final boolean leftAlignedText;
    private Component name;
    private boolean selected;
    private boolean active = true;
    private boolean visible = true;

    public ButtonWidget(Dimension<Integer> dim, Component name, Runnable onPress, boolean leftAlignedText) {
        super(dim);
        this.name = name;
        this.onPress = onPress;

        this.leftAlignedText = leftAlignedText;
    }

    public ButtonWidget(Dimension<Integer> dim, Component name, Runnable onPress) {
        this(dim, name, onPress, false);
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        super.render(guiGraphics, mouseX, mouseY, delta);

        int backgroundColor = this.isActive()
                ? ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_BLACK, 0.45f)
                : ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_BLACK, 0.3f);
        int textColor = this.isActive()
                ? GuiConstants.COLOR_WHITE
                : GuiConstants.COLOR_GRAY;
        int selectionOutlineColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, 0.8f);
        int selectionFillColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, 0.2f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();

        GuiRenderer.fill(
                getDim().x(), getDim().y(),
                getDim().xLimit(), getDim().yLimit(),
                backgroundColor);

        if (this.leftAlignedText) {
            GuiRenderer.drawString(
                    this.name,
                    getDim().x() + 8, getDim().centerY() - 4,
                    textColor | (Mth.ceil(this.alpha * 255.0f) << 24));
        } else {
            GuiRenderer.drawCenteredString(
                    this.name,
                    getDim().centerX(), getDim().centerY() - 4,
                    textColor | (Mth.ceil(this.alpha * 255.0f) << 24));
        }

        if (this.selected) {
            RenderSystem.enableBlend();

            GuiRenderer.fill(
                    getDim().x(), getDim().y(),
                    getDim().x() + 1.5f, getDim().yLimit(),
                    selectionOutlineColor);
            GuiRenderer.fill(
                    getDim().x(), getDim().y(),
                    getDim().xLimit(), getDim().yLimit(),
                    selectionFillColor);
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) {
            return false;
        }

        if (button == 0 && getDim().isPointInside(mouseX, mouseY)) {
            doAction();

            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused())
            return false;

        if (CommonInputs.selected(keyCode)) {
            doAction();
            return true;
        }

        return false;
    }

    private void doAction() {
        this.onPress.run();
        this.playDownSound();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Component getName() {
        return name;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.active || !this.visible)
            return null;
        return super.nextFocusPath(event);
    }
}
