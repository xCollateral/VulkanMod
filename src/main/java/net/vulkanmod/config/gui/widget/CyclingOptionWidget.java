package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.option.CyclingOption;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix4f;

public class CyclingOptionWidget extends OptionWidget<CyclingOption<?>> {
    private Button leftButton;
    private Button rightButton;

    private boolean focused;

    public CyclingOptionWidget(CyclingOption<?> option, int x, int y, int width, int height, Component name) {
        super(x, y, width, height, name);
        this.option = option;
        this.leftButton = new Button(this.controlX, 16, Button.Direction.LEFT);
        this.rightButton = new Button(this.controlX + this.controlWidth - 16, 16, Button.Direction.RIGHT);

//        updateDisplayedValue(option.getValueText());
    }

    @Override
    protected int getYImage(boolean hovered) {
        return  0;
    }

    public void renderControls(double mouseX, double mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        this.renderBars();

        this.leftButton.setStatus(option.index() > 0);
        this.rightButton.setStatus(option.index() < option.getValues().length - 1);

        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        Font textRenderer = Minecraft.getInstance().font;
        int x = this.controlX + this.controlWidth / 2;
        int y = this.y + (this.height - 9) / 2;
        GuiRenderer.drawCenteredString(textRenderer, this.getDisplayedValue(), x, y, color);

        this.leftButton.renderButton(GuiRenderer.guiGraphics.pose(), mouseX, mouseY);
        this.rightButton.renderButton(GuiRenderer.guiGraphics.pose(), mouseX, mouseY);
    }

    public void renderBars() {
        int count = option.getValues().length;
        int current = option.index();

        int margin = 30;
        int padding = 4;

        int barWidth = (this.controlWidth - (2 * margin) - (padding * count)) / count;
        int color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.4f);
        int activeColor = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 1.0f);

        if (barWidth <= 0)
            return;

        for (int i = 0; i < count; i++) {
            float x0 = this.controlX + margin + i * (barWidth + padding);
            float y0 = this.y + this.height - 5.0f;

            int c = i == current ? activeColor : color;
            GuiRenderer.fill(x0, y0, x0 + barWidth, y0 + 1.5f, c);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (leftButton.isHovered(mouseX, mouseY)) {
            option.prevValue();
        }
        else if (rightButton.isHovered(mouseX, mouseY)) {
            option.nextValue();
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {

    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {

    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    class Button {
        int x;
        int width;
        boolean active;
        Direction direction;

        Button(int x, int width, Direction direction) {
            this.x = x;
            this.width = width;
            this.active = true;
            this.direction = direction;
        }

        boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        void setStatus(boolean status) {
            this.active = status;
        }

        void renderButton(PoseStack matrices, double mouseX, double mouseY) {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            float f = this.isHovered(mouseX, mouseY) && this.active ? 5.0f : 4.5f;

            Matrix4f matrix4f = matrices.last().pose();

            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.enableBlend();

            if(this.isHovered(mouseX, mouseY) && this.active)
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            else if(this.active)
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);
            else
                RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 0.8f);

            float h = f;
            float w = f - 1.0f;
            float yC = y + height * 0.5f;
            float xC = x + width * 0.5f;
            if (this.direction == Direction.LEFT) {
                bufferBuilder.addVertex(matrix4f, xC - w, yC, 0);
                bufferBuilder.addVertex(matrix4f, xC + w, yC + h, 0);
                bufferBuilder.addVertex(matrix4f, xC + w, yC - h, 0);
            } else {
                bufferBuilder.addVertex(matrix4f, xC + w, yC, 0);
                bufferBuilder.addVertex(matrix4f, xC - w, yC - h, 0);
                bufferBuilder.addVertex(matrix4f, xC - w, yC + h, 0);
            }

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }

        enum Direction {
            LEFT,
            RIGHT
        }
    }

}
