package net.vulkanmod.config.gui.option.control;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.widget.ControllerWidget;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix4f;

import java.util.List;

public record CyclingController<T>(Option<T> option, List<T> values) implements Controller<T> {
    @Override
    public ControllerWidget<T> createWidget(Dimension<Integer> dim) {
        return new CyclingControllerWidget(dim);
    }

    private class CyclingControllerWidget extends ControllerWidget<T> {
        private final Dimension<Float> barsDim;
        private final Dimension<Float> leftArrowDim;
        private final Dimension<Float> rightArrowDim;

        public CyclingControllerWidget(Dimension<Integer> dim) {
            super(option, dim);

            this.barsDim = Dimension.ofFloat(
                    getControllerDim().x() + 30, getDim().yLimit() - 5,
                    getControllerDim().width() - 60, 1.5f);

            this.leftArrowDim = Dimension.ofFloat(
                    getControllerDim().x() + 8, getDim().centerY() - 4,
                    7, 9);
            this.rightArrowDim = Dimension.ofFloat(
                    getControllerDim().xLimit() - 18, getDim().centerY() - 4,
                    7, 9);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            super.render(guiGraphics, mouseX, mouseY, delta);

            renderBars();
            renderArrow(leftArrowDim, mouseX, mouseY);
            renderArrow(rightArrowDim, mouseX, mouseY);

            GuiRenderer.drawScrollingString(
                    getDisplayedValue(),
                    getControllerDim().centerX(), (int) (getDim().centerY() - 4.5f),
                    (int) (rightArrowDim.x() - leftArrowDim.xLimit() - 12),
                    getOptionStateColor());
        }

        private void renderBars() {
            int pendingValueId = values.indexOf(option.pendingValue());

            int padding = 4;
            int totalPadding = padding * values.size();
            float availableWidth = barsDim.width() - totalPadding;
            float barWidth = availableWidth / (float) values.size();

            if (barWidth < 1)
                return;

            RenderSystem.enableBlend();

            for (int i = 0; i < values.size(); i++) {
                float x = barsDim.x() + i * (barWidth + padding);
                int barColor = (i == pendingValueId)
                        ? GuiConstants.COLOR_WHITE
                        : ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_WHITE, 0.4f);

                GuiRenderer.fill(
                        x, barsDim.y(),
                        x + barWidth, barsDim.yLimit(),
                        barColor);
            }
        }


        private void renderArrow(Dimension<Float> arrowDim, double mouseX, double mouseY) {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            Matrix4f matrix4f = GuiRenderer.pose.last().pose();

            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.enableBlend();

            int valueIndex = values.indexOf(option.pendingValue());

            if (arrowDim.isPointInside(mouseX, mouseY) && this.isActive())
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            else if (this.isActive())
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);

            if (arrowDim == leftArrowDim) {
                if (valueIndex == 0 || !this.isActive())
                    RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 0.8f);

                bufferBuilder.addVertex(matrix4f, arrowDim.x(), arrowDim.centerY(), 0);
                bufferBuilder.addVertex(matrix4f, arrowDim.xLimit(), arrowDim.yLimit(), 0);
                bufferBuilder.addVertex(matrix4f, arrowDim.xLimit(), arrowDim.y(), 0);
            } else if (arrowDim == rightArrowDim) {
                if (valueIndex == values.size() - 1 || !this.isActive())
                    RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 0.8f);

                bufferBuilder.addVertex(matrix4f, arrowDim.x(), arrowDim.y(), 0);
                bufferBuilder.addVertex(matrix4f, arrowDim.x(), arrowDim.yLimit(), 0);
                bufferBuilder.addVertex(matrix4f, arrowDim.xLimit(), arrowDim.centerY(), 0);
            }

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.isActive())
                return false;

            if (rightArrowDim.isPointInside(mouseX, mouseY)) {
                nextValue(1);
                playDownSound();
                return true;
            } else if (leftArrowDim.isPointInside(mouseX, mouseY)) {
                nextValue(-1);
                playDownSound();
                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isFocused() || !this.isActive())
                return false;

            switch (keyCode) {
                case InputConstants.KEY_LEFT -> nextValue(-1);
                case InputConstants.KEY_RIGHT -> nextValue(1);
                case InputConstants.KEY_RETURN, InputConstants.KEY_SPACE, InputConstants.KEY_NUMPADENTER ->
                        nextValue(Screen.hasControlDown() || Screen.hasShiftDown() ? -1 : 1);
                default -> {
                    return false;
                }
            }

            return true;
        }

        private void nextValue(int direction) {
            var nextIndex = values.indexOf(option.pendingValue()) + direction;

            if (nextIndex >= 0 && nextIndex < values.size()) {
                option.setPendingValue(values.get(nextIndex));
            }
        }
    }
}
