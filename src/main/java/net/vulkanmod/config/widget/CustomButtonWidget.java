package net.vulkanmod.config.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import static net.vulkanmod.config.widget.OptionWidget.WIDGETS_TEXTURE;

public class CustomButtonWidget extends Button {
    boolean selected = false;

    public CustomButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
    }

    public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Font textRenderer = minecraftClient.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        int i = this.getYImage(this.isHovered);
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
        fill(matrices, this.x, this.y, this.x + width, this.y + this.height, color);
        this.renderBg(matrices, minecraftClient, mouseX, mouseY);
        int j = this.active ? 0xFFFFFF : 0xA0A0A0;
        drawCenteredString(matrices, textRenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0f) << 24);

        if(this.selected) {
            fill(matrices, this.x, this.y + this.height - 2, this.x + this.width, this.y + this.height,  0xFFFFFFFF);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
