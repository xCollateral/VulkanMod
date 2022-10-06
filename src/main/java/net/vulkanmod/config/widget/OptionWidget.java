package net.vulkanmod.config.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.vulkanmod.vulkan.util.VUtil;

public abstract class OptionWidget extends DrawableHelper
        implements Drawable,
        Element, Selectable {

    public static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");
    protected int width;
    protected int height;
    public int x;
    public int y;
    public int controlX;
    public int controlWidth;
    private Text name;
    protected Text displayedValue;
    protected boolean hovered;
    protected boolean controlHovered;
    public boolean active = true;
    public boolean visible = true;
    protected float alpha = 1.0f;
    private boolean focused;

    public OptionWidget(int x, int y, int width, int height, Text name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.displayedValue = Text.of("N/A");
        this.controlWidth = (int) (width * 0.4f);
        this.controlX = (int) (x + 0.5f * width);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        this.controlHovered = mouseX >= this.controlX && mouseY >= this.y && mouseX < this.controlX + this.controlWidth && mouseY < this.y + this.height;
        this.renderWidget(matrices, mouseX, mouseY, delta);
    }

    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        TextRenderer textRenderer = minecraftClient.textRenderer;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        int i = this.getYImage(this.isHovered());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int color = this.controlHovered ? VUtil.packColor(0.0f, 0.0f, 0.0f, 0.45f) : VUtil.packColor(0.0f, 0.0f, 0.0f, 0.3f);

        if(this.hovered) fill(matrices, this.x - 2, this.y - 2, this.x + this.width + 2, this.y + this.height + 2, 0x28000000);
        fill(matrices, this.controlX, this.y, this.controlX + this.controlWidth, this.y + height, color);

        this.renderBackground(matrices, minecraftClient, mouseX, mouseY);
        int j = this.active ? 0xFFFFFF : 0xA0A0A0;
        ClickableWidget.drawWithShadow(matrices, textRenderer, this.getName().asOrderedText(), this.x, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0f) << 24);
        ClickableWidget.drawCenteredText(matrices, textRenderer, this.getDisplayedValue(), this.controlX + this.controlWidth / 2, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0f) << 24);
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

    protected void renderBackground(MatrixStack matrices, MinecraftClient client, int mouseX, int mouseY) {
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

    @Override
    public boolean changeFocus(boolean lookForwards) {
        if (!this.active || !this.visible) {
            return false;
        }
        this.focused = !this.focused;
//        this.onFocusedChanged(this.focused);
        return this.focused;
    }

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
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
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

    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.controlX && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    public Text getName() {
        return this.name;
    }

    public Text getDisplayedValue() {
        return this.displayedValue;
    }

    public void updateDisplayedValue(Text text) { this.displayedValue = text; }

    protected void applyValue() {}

    abstract public Text getTooltip();

    @Override
    public SelectionType getType() {
        if (this.focused) {
            return Selectable.SelectionType.FOCUSED;
        }
        if (this.hovered) {
            return Selectable.SelectionType.HOVERED;
        }
        return Selectable.SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
//        builder.put(NarrationPart.TITLE, (Text)this.getNarrationMessage());
//        if (this.active) {
//            if (this.focused) {
//                builder.put(NarrationPart.USAGE, (Text)new TranslatableText("narration.slider.usage.focused"));
//            } else {
//                builder.put(NarrationPart.USAGE, (Text)new TranslatableText("narration.slider.usage.hovered"));
//            }
//        }
    }

    public void playDownSound(SoundManager soundManager) {
        soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
