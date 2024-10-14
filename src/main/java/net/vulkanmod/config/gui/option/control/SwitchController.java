package net.vulkanmod.config.gui.option.control;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.widget.ControllerWidget;
import net.vulkanmod.vulkan.util.ColorUtil;

public record SwitchController(Option<Boolean> option) implements Controller<Boolean> {
    @Override
    public ControllerWidget<Boolean> createWidget(Dimension<Integer> dim) {
        return new SwitchControllerWidget(dim);
    }

    private class SwitchControllerWidget extends ControllerWidget<Boolean> {
        private final Dimension<Integer> switchDim;
        private final Dimension<Integer> switchInnerDim;

        public SwitchControllerWidget(Dimension<Integer> dim) {
            super(option, dim);

            int switchBoxWidth = 24;
            int switchBoxHeight = dim.height() - 8;
            this.switchDim = Dimension.ofInt(
                    getControllerDim().centerX() - (switchBoxWidth / 2), dim.y() + 4,
                    switchBoxWidth, switchBoxHeight
            );

            this.switchInnerDim = Dimension.ofInt(
                    switchDim.x() + 2, switchDim.y() + 2,
                    switchDim.width() - 4, switchDim.height() - 4
            );
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            super.render(guiGraphics, mouseX, mouseY, delta);

            int textXPosition = getControllerDim().centerX() - (switchDim.width() * 3 / 4) - 4 - getOnOffMargin();

            int offStateColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_WHITE, 0.4f);
            int onStateBackgroundColor = GuiConstants.COLOR_DARK_GRAY;
            int borderColor = GuiConstants.COLOR_GRAY;

            RenderSystem.enableBlend();

            if (option.pendingValue()) {
                GuiRenderer.fill(
                        switchInnerDim.x(), switchInnerDim.y(),
                        switchInnerDim.centerX() + 1, switchInnerDim.yLimit(),
                        onStateBackgroundColor);
                GuiRenderer.fill(
                        switchInnerDim.centerX() + 2, switchInnerDim.y(),
                        switchInnerDim.xLimit(), switchInnerDim.yLimit(),
                        getOptionStateColor());
            } else {
                GuiRenderer.fill(
                        switchInnerDim.x(), switchInnerDim.y(),
                        switchInnerDim.centerX() - 2, switchInnerDim.yLimit(),
                        offStateColor);
            }

            GuiRenderer.renderBorder(
                    switchDim.x(), switchDim.y(),
                    switchDim.xLimit(), switchDim.yLimit(),
                    1, borderColor);
            GuiRenderer.drawCenteredString(
                    getDisplayedValue(),
                    textXPosition, getDim().centerY() - 4,
                    getOptionStateColor());
        }

        private int getOnOffMargin() {
            Font font = Minecraft.getInstance().font;

            return Math.max(
                    font.width(Component.translatable("options.on")) / 3,
                    font.width(Component.translatable("options.off")) / 3
            );
        }

        public void toggleSetting() {
            option.setPendingValue(!option.pendingValue());
            updateDisplayedValue();
            playDownSound();
        }

        @Override
        protected void updateDisplayedValue() {
            setDisplayedValue(option.pendingValue()
                    ? Component.translatable("options.on")
                    : Component.translatable("options.off"));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!switchDim.isPointInside(mouseX, mouseY) || !this.isActive() || button != 0) {
                return false;
            }

            toggleSetting();
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isFocused()) {
                return false;
            }

            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_NUMPADENTER) {
                toggleSetting();
                return true;
            }

            return false;
        }
    }
}
