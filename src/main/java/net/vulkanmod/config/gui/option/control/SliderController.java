package net.vulkanmod.config.gui.option.control;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.widget.ControllerWidget;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.apache.commons.lang3.Validate;

public record SliderController(Option<Integer> option, int min, int max, int step) implements Controller<Integer> {
    public SliderController {
        Validate.isTrue(max >= min, "`max` cannot be smaller than `min`");
        Validate.isTrue(step > 0, "`step` must be more than 0");
    }

    @Override
    public ControllerWidget<Integer> createWidget(Dimension<Integer> dim) {
        return new SliderControllerWidget(dim);
    }

    private class SliderControllerWidget extends ControllerWidget<Integer> {
        private final Dimension<Float> sliderDim;
        private final Dimension<Float> sliderHoveredDim;

        private boolean mouseDown = false;

        public SliderControllerWidget(Dimension<Integer> dim) {
            super(option, dim);

            this.sliderDim = Dimension.ofFloat(
                    getControllerDim().x(), getDim().yLimit() - 5,
                    getControllerDim().width(), 1.5f
            );

            this.sliderHoveredDim = Dimension.ofFloat(
                    getControllerDim().x(), getDim().y() + (getDim().height() * 0.5f) - 1,
                    getControllerDim().width(), 2
            );
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            super.render(guiGraphics, mouseX, mouseY, delta);

            RenderSystem.enableBlend();

            float ratio = Mth.clamp((float) (option.pendingValue() - min) / (max - min), 0, 1);
            int valueX = (int) (sliderDim.x() + ratio * getControllerDim().width());

            if (getControllerDim().isPointInside(mouseX, mouseY) || this.isFocused()) {
                int thumbWidth = 2;
                int thumbHeight = 4;

                GuiRenderer.fill(
                        sliderHoveredDim.x(), sliderHoveredDim.y(),
                        sliderHoveredDim.xLimit(), sliderHoveredDim.yLimit(),
                        ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_WHITE, 0.1f));
                GuiRenderer.fill(
                        sliderHoveredDim.x(), sliderHoveredDim.y(),
                        valueX - thumbWidth, sliderHoveredDim.yLimit(),
                        ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_WHITE, 0.3f));

                GuiRenderer.renderBorder(
                        valueX - thumbWidth, sliderHoveredDim.y() - thumbHeight,
                        valueX + thumbWidth, sliderHoveredDim.yLimit() + thumbHeight,
                        1, ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_WHITE, 0.3f));
            } else {
                GuiRenderer.fill(
                        sliderDim.x(), sliderDim.y(),
                        sliderDim.xLimit(), sliderDim.yLimit(),
                        ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_WHITE, 0.3f));
                GuiRenderer.fill(
                        sliderDim.x(), sliderDim.y(),
                        valueX, sliderDim.yLimit(),
                        getOptionStateColor());
            }

            GuiRenderer.drawString(
                    getDisplayedValue(),
                    getControllerDim().centerX() - (GuiRenderer.font.width(getDisplayedValue()) / 2), getDim().centerY() - 4,
                    getOptionStateColor());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.isActive() || button != 0 || !getControllerDim().isPointInside(mouseX, mouseY))
                return false;

            mouseDown = true;

            setValueFromMouse(mouseX);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!this.isActive() || button != 0 || !mouseDown)
                return false;

            setValueFromMouse(mouseX);
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
            if (!this.isActive() || (!getControllerDim().isPointInside(mouseX, mouseY)) || (!Screen.hasShiftDown() && !Screen.hasControlDown()))
                return false;

            incrementValue(vertical);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (this.isActive() && mouseDown)
                playDownSound();
            mouseDown = false;

            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isFocused())
                return false;

            switch (keyCode) {
                case InputConstants.KEY_LEFT -> incrementValue(-1);
                case InputConstants.KEY_RIGHT -> incrementValue(1);
                default -> {
                    return false;
                }
            }

            return true;
        }

        protected void setValueFromMouse(double mouseX) {
            double value = (mouseX - sliderHoveredDim.x()) / sliderHoveredDim.width() * (max - min);
            option.setPendingValue((int) roundToStepSize(value));
        }

        protected double roundToStepSize(double value) {
            return Mth.clamp(min + (step * Math.round(value / step)), min, max);
        }

        public void incrementValue(double amount) {
            option.setPendingValue((int) Mth.clamp(option.pendingValue() + step * amount, min, max));
        }

    }
}
