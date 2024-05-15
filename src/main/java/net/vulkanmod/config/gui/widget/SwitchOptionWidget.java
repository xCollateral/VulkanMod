package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.option.SwitchOption;
import net.vulkanmod.vulkan.util.ColorUtil;

public class SwitchOptionWidget extends OptionWidget<SwitchOption> {
    private boolean focused;

    public SwitchOptionWidget(SwitchOption option, int x, int y, int width, int height, Component name) {
        super(x, y, width, height, name);
        this.option = option;
        updateDisplayedValue();
    }

    @Override
    protected void renderControls(double mouseX, double mouseY) {
        int center = controlX + controlWidth / 2;
        float halfWidth = 12;
        float x0 = center - halfWidth;
        float y0 = y + 4;
        float height = this.height - 8;
        int color;

        float w1 = halfWidth - 4;
        float h1 = height - 4;
        if (this.option.getNewValue()) {
            float x1 = x0 + halfWidth + 2;

            color = ColorUtil.ARGB.pack(0.4f, 0.4f, 0.4f, 1.0f);
            GuiRenderer.fillBox(x0 + 2, y0 + 2, x1 - (x0 + 2) - 1, h1, color);

            color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 1.0f);
            GuiRenderer.fillBox(x1, y0 + 2, w1, h1, color);
        } else {
            color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.4f);
            GuiRenderer.fillBox(x0 + 2, y0 + 2, w1, h1, color);
        }

        color = ColorUtil.ARGB.pack(0.6f, 0.6f, 0.6f, 1.0f);
        GuiRenderer.renderBoxBorder(x0, y0, halfWidth * 2, height, 1,  color);

        color = this.active ? 0xFFFFFF : 0xA0A0A0;
        Font textRenderer = Minecraft.getInstance().font;
        int margin = Math.max(
                textRenderer.width(Component.translatable("options.on").getString()) / 3,
                textRenderer.width(Component.translatable("options.off").getString()) / 3
        );

        int x = this.controlX + this.controlWidth / 2 - (int) (halfWidth * 1.5f) - 4 - margin;
        int y = this.y + (this.height - 8) / 2;
        GuiRenderer.drawCenteredString(textRenderer, this.getDisplayedValue(), x, y, color);
    }

    public void onClick(double mouseX, double mouseY) {
        this.option.setNewValue(!this.option.getNewValue());
        updateDisplayedValue();
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {

    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {

    }

    protected void updateDisplayedValue() {
        this.displayedValue = option.getNewValue()
                ? Component.translatable("options.on")
                : Component.translatable("options.off");
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

}
