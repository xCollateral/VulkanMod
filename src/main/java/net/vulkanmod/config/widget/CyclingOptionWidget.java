package net.vulkanmod.config.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import net.vulkanmod.config.CyclingOption;

public class CyclingOptionWidget extends OptionWidget {
    CyclingOption<?> option;

    private Button leftButton;
    private Button rightButton;

    public CyclingOptionWidget(CyclingOption<?> option, int x, int y, int width, int height, Text name) {
        super(x, y, width, height, name);
        this.option = option;
        this.leftButton = new Button(this.controlX, 16, 0);
        this.rightButton = new Button(this.controlX + this.controlWidth - 16, 16, 1);

        updateDisplayedValue(option.getValueText());
    }

    @Override
    protected int getYImage(boolean hovered) {
        return  0;
    }

    public void renderBackground(MatrixStack matrices, MinecraftClient client, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = (this.isHovered() ? 2 : 1) * 20;
//        this.drawTexture(matrices, this.controlX, this.y, 0, 46 + i, 8, this.height);
//        this.drawTexture(matrices, this.controlX + 8, this.y, 192, 46 + i, 8, this.height);
//        this.drawTexture(matrices, this.controlX + this.controlWidth - 16, this.y, 0, 46 + i, 8, this.height);
//        this.drawTexture(matrices, this.controlX + this.controlWidth - 8, this.y, 192, 46 + i, 8, this.height);

        this.leftButton.setStatus(option.index() > 0);
        this.rightButton.setStatus(option.index() < option.getValues().length - 1);

        this.leftButton.renderButton(matrices, mouseX, mouseY);
        this.rightButton.renderButton(matrices, mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
//        this.setValueFromMouse(mouseX);
        boolean buttonClicked = false;
        if(leftButton.isHovered((int)mouseX, (int)mouseY)) {
            option.prevValue();
            buttonClicked = true;
        }
        else if(rightButton.isHovered((int)mouseX, (int)mouseY)) {
            option.nextValue();
            buttonClicked = true;
        }

        if(buttonClicked) updateDisplayedValue(this.option.getValueText());
    }

    public Text getTooltip() {
        return this.option.getTooltip();
    }

    class Button {
        int x;
        int width;
        boolean active;
        int direction;

        Button(int x, int width, int direction) {
            this.x = x;
            this.width = width;
            this.active = true;
            this.direction = direction;

            if(direction < 0 || direction > 1) throw new RuntimeException("direction not allowed.");
        }

        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        void setStatus(boolean status) {
            this.active = status;
        }

        void renderButton(MatrixStack matrices, int mouseX, int mouseY) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();

            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            int i = (this.isHovered(mouseX, mouseY) ? 2 : 1) * 20;
            i = this.active ? i : 0;
//            drawTexture(matrices, x, y, 0, 46 + i, 8, height);
//            drawTexture(matrices, x + 8, y, 192, 46 + i, 8, height);

            float f = this.isHovered(mouseX, mouseY) && this.active ? 3.0f : 4.0f;

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
//            if(this.isHovered(mouseX, mouseY)) matrix4f.multiply(Matrix4f.scale(1.1f, 1.1f, 1.1f));

            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.enableBlend();

            if(this.isHovered(mouseX, mouseY) && this.active) RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            else if(this.active) RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);
            else RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.8f);

            bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION);

            if(direction == 0) {
                bufferBuilder.vertex(matrix4f, x + f, y + height * 0.5f, 0).next();
                bufferBuilder.vertex(matrix4f, x + 16 - f, y + height - f, 0).next();
                bufferBuilder.vertex(matrix4f, x + 16 - f, y + f, 0).next();
            } else {
                bufferBuilder.vertex(matrix4f, x + 16 - f, y + height * 0.5f, 0).next();
                bufferBuilder.vertex(matrix4f, x + f, y + f, 0).next();
                bufferBuilder.vertex(matrix4f, x + f, y + height - f, 0).next();
            }

            tessellator.draw();

//            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
//            bufferBuilder.vertex(matrix4f, x + f, y + height - f, 0).color(255, 255, 255, 255).next();
//            bufferBuilder.vertex(matrix4f, x + 16 - f, y + height - f, 0).color(255, 255, 255, 255).next();
//            bufferBuilder.vertex(matrix4f, x + 16 - f, y + f, 0).color(255, 255, 255, 255).next();
//            bufferBuilder.vertex(matrix4f, x + f, y + f, 0).color(255, 255, 255, 255).next();
//            tessellator.draw();

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }
    }

}
