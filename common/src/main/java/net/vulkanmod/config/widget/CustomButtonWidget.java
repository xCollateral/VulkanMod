package net.vulkanmod.config.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

import static net.vulkanmod.config.widget.OptionWidget.WIDGETS_TEXTURE;

public class CustomButtonWidget extends AbstractButton {
    int x;
    int y;
    boolean selected = false;
    Consumer<CustomButtonWidget> onPress;

    public CustomButtonWidget(int x, int y, int width, int height, Component message, Consumer<CustomButtonWidget> onPress) {
        super(x, y, width, height, message);
        this.x = x;
        this.y = y;
        this.onPress = onPress;
    }

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Font textRenderer = minecraftClient.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        int i = this.getTextureY();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
//        this.drawTexture(matrices, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
//        this.drawTexture(matrices, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
//        fill(matrices, this.x, this.y, this.x + width, this.y + this.height, this.isHovered() ? 0x507E0707 : 0x307E0707);

        int color;
        if(this.isHovered && this.active) color = 0xB0000000;
        else if(this.active) color = 0x90000000;
        else color = 0x70000000;
        guiGraphics.fill(this.x, this.y, this.x + width, this.y + this.height, color);
//        this.renderWidget(matrices, mouseX, mouseY);
//        this.renderWidget(matrices, 0, 0, 0);
        int j = this.active ? 0xFFFFFF : 0xA0A0A0;
        guiGraphics.drawCenteredString(textRenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0f) << 24);

        if(this.selected) {
            guiGraphics.fill(this.x, this.y + this.height - 2, this.x + this.width, this.y + this.height,  0xFFFFFFFF);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onPress() {
        this.onPress.accept(this);
    }

    private int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) {
            i = 2;
        }

        return 46 + i * 20;
    }
}
