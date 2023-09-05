package net.vulkanmod.config.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.VUtil;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class OptionWidget
        implements Renderable, LayoutElement, GuiEventListener, NarratableEntry {

    public static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation("textures/gui/widgets.png");
    protected int width;
    protected int height;
    public int x;
    public int y;
    public int controlX;
    public int controlWidth;
    private Component name;
    protected Component displayedValue;
    protected boolean hovered;
    protected boolean controlHovered;
    public boolean active = true;
    public boolean visible = true;
    protected float alpha = 1.0f;
    private boolean focused;

    public OptionWidget(int x, int y, int width, int height, Component name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.displayedValue = Component.literal("N/A");
        this.controlWidth = (int) (width * 0.4f);
        this.controlX = (int) (x + 0.5f * width);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        this.controlHovered = mouseX >= this.controlX && mouseY >= this.y && mouseX < this.controlX + this.controlWidth && mouseY < this.y + this.height;
        this.renderWidget(guiGraphics, mouseX, mouseY, delta);
    }

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Font textRenderer = minecraftClient.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        int i = this.getYImage(this.isHovered());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int color = this.controlHovered ? ColorUtil.packColorInt(0.0f, 0.0f, 0.0f, 0.45f) : ColorUtil.packColorInt(0.0f, 0.0f, 0.0f, 0.3f);

        if(this.hovered)
            guiGraphics.fill(this.x - 2, this.y - 2, this.x + this.width + 2, this.y + this.height + 2, 0x28000000);
        guiGraphics.fill(this.controlX, this.y, this.controlX + this.controlWidth, this.y + height, color);

        this.renderBackground(guiGraphics, minecraftClient, mouseX, mouseY);
        int j = this.active ? 0xFFFFFF : 0xA0A0A0;
        guiGraphics.drawString(textRenderer, this.getName().getVisualOrderText(), this.x, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0f) << 24);
        guiGraphics.drawCenteredString(textRenderer, this.getDisplayedValue(), this.controlX + this.controlWidth / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0f) << 24);
    }

    protected static void renderScrollingString(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k, int l, int m) {
        int n = font.width(component);
        int var10000 = j + l;
        Objects.requireNonNull(font);
        int o = (var10000 - 9) / 2 + 1;
        int p = k - i;
        if (n > p) {
            int q = n - p;
            double d = (double) Util.getMillis() / 1000.0;
            double e = Math.max((double)q * 0.5, 3.0);
            double f = Math.sin(1.5707963267948966 * Math.cos(6.283185307179586 * d / e)) / 2.0 + 0.5;
            double g = Mth.lerp(f, 0.0, (double)q);
            guiGraphics.enableScissor(i, j, k, l);
            guiGraphics.drawString(font, component, i - (int)g, o, m);
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawCenteredString(font, component, (i + k) / 2, o, m);
        }

    }

    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
        int k = this.getX() + i;
        int l = this.getX() + this.getWidth() - i;
        renderScrollingString(guiGraphics, font, this.name, k, this.getY(), l, this.getY() + this.getHeight(), j);
    }

    public void renderTexture(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p, int q) {
        int r = l;
        if (!this.isActive()) {
            r = l + m * 2;
        } else if (this.isHovered() || this.isFocused()) {
            r = l + m;
        }

        RenderSystem.enableDepthTest();
        guiGraphics.blit(resourceLocation, i, j, (float)k, (float)r, n, o, p, q);
    }

    protected int getYImage(boolean hovered) {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (hovered) {
            i = 2;
        }
        return i;
    }

    public boolean isHovered() {
        return this.hovered || this.focused;
    }

    protected void renderBackground(GuiGraphics guiGraphics, Minecraft client, int mouseX, int mouseY) {
    }

    public void onClick(double mouseX, double mouseY) {
    }

    public void onRelease(double mouseX, double mouseY) {
    }

    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
    }

    protected boolean isValidClickButton(int button) {
        return button == 0;
    }

    //TODO
//    @Override
//    public boolean changeFocus(boolean lookForwards) {
//        if (!this.active || !this.visible) {
//            return false;
//        }
//        this.focused = !this.focused;
////        this.onFocusedChanged(this.focused);
//        return this.focused;
//    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isValidClickButton(button)) {
            this.onDrag(mouseX, mouseY, deltaX, deltaY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean bl;
        if (!this.active || !this.visible) {
            return false;
        }

        if (this.isValidClickButton(button) && (bl = this.clicked(mouseX, mouseY))) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button)) {
            this.onRelease(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.controlX && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    public Component getName() {
        return this.name;
    }

    public Component getDisplayedValue() {
        return this.displayedValue;
    }

    public void updateDisplayedValue(Component text) { this.displayedValue = text; }

    protected void applyValue() {}

    abstract public Component getTooltip();

    @Override
    public NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        }
        if (this.hovered) {
            return NarrationPriority.HOVERED;
        }
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
//        builder.put(NarrationPart.TITLE, (Component)this.getNarrationMessage());
//        if (this.active) {
//            if (this.focused) {
//                builder.put(NarrationPart.USAGE, (Component)new TranslatableComponent("narration.slider.usage.focused"));
//            } else {
//                builder.put(NarrationPart.USAGE, (Component)new TranslatableComponent("narration.slider.usage.hovered"));
//            }
//        }
    }

    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public void setX(int i) {

    }

    @Override
    public void setY(int i) {

    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }


    @Override
    public void visitWidgets(Consumer<AbstractWidget> consumer) {

    }
}
