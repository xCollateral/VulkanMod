package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.vulkan.util.ColorUtil;

import java.util.function.Consumer;

public class VButtonWidget extends VAbstractWidget {
    boolean selected = false;
    Consumer<VButtonWidget> onPress;

    float alpha = 1.0f;

    public VButtonWidget(int x, int y, int width, int height, Component message, Consumer<VButtonWidget> onPress) {
        this.setPosition(x, y, width, height);

        this.message = message;
        this.onPress = onPress;
    }

    public void renderWidget(double mouseX, double mouseY) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Font textRenderer = minecraftClient.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);

        RenderSystem.enableBlend();

        int xPadding = 0;
        int yPadding = 0;

        int color = ColorUtil.ARGB.pack(0.0f, 0.0f, 0.0f, this.active ? 0.45f : 0.3f);
        GuiRenderer.fill(this.x - xPadding, this.y - yPadding, this.x + this.width + xPadding, this.y + this.height + yPadding, color);

        if (this.active) {
            this.renderHovering(0, 0);
        }

        int j = this.active ? 0xFFFFFF : 0xA0A0A0;
        GuiRenderer.drawCenteredString(textRenderer, this.message, this.x + this.width / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0f) << 24);

        RenderSystem.enableBlend();

        if(this.selected) {
//            color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 1.0f);
            color = ColorUtil.ARGB.pack(0.3f, 0.0f, 0.0f, 1.0f);
//            GuiRenderer.fillBox(this.x, this.y + this.height - 1, this.width, 1,  color);
            GuiRenderer.fillBox(this.x, this.y, 1.5f, this.height,  color);

//            color = ColorUtil.ARGB.pack(0.5f, 0.5f, 0.5f, 0.2f);
            color = ColorUtil.ARGB.pack(0.3f, 0.0f, 0.0f, 0.2f);
            GuiRenderer.fillBox(this.x, this.y, this.width, this.height,  color);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void onClick(double mX, double mY) {
        this.onPress.accept(this);
    }

}
