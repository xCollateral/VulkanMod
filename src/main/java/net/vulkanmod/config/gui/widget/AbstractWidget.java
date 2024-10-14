package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractWidget implements GuiEventListener, Renderable, NarratableEntry {
    private final Dimension<Integer> dim;
    private boolean focused;
    private boolean hovered;
    private long hoverStartTime;
    private int hoverTime;
    private long hoverStopTime;

    public AbstractWidget(Dimension<Integer> dim) {
        this.dim = dim;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        this.renderFocusing();
        this.updateHoverState(mouseX, mouseY);
        this.renderHovering();
    }

    private void renderFocusing() {
        if (!this.isFocused()) {
            return;
        }

        int borderColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, 0.8f);
        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, 0.3f);

        GuiRenderer.fill(
                getDim().x() + 1, getDim().y() + 1,
                getDim().xLimit() - 1, getDim().yLimit() - 1,
                backgroundColor);
        GuiRenderer.renderBorder(
                getDim().x(), getDim().y(),
                getDim().xLimit(), getDim().yLimit(),
                45,
                1, borderColor);
    }

    protected void renderHovering() {
        if (this.isFocused() || !this.isActive())
            return;

        float hoverMultiplier = this.getHoverMultiplier(200);

        int borderColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, hoverMultiplier);
        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, 0.3f * hoverMultiplier);

        if (hoverMultiplier > 0.0f) {
            GuiRenderer.fill(
                    getDim().x() + 1, getDim().y() + 1,
                    getDim().xLimit() - 1, getDim().yLimit() - 1,
                    backgroundColor);
            GuiRenderer.renderBorder(
                    getDim().x(), getDim().y(),
                    getDim().xLimit(), getDim().yLimit(),
                    1, borderColor);
        }
    }

    public float getHoverMultiplier(float time) {
        if (this.hovered) {
            return Math.min(((this.hoverTime) / time), 1.0f);
        } else {
            int delta = (int) (Util.getMillis() - this.hoverStopTime);
            return Math.max(1.0f - (delta / time), 0.0f);
        }
    }

    public void updateHoverState(double mouseX, double mouseY) {
        if (getDim().isPointInside(mouseX, mouseY)) {
            if (!this.hovered) {
                this.hoverStartTime = Util.getMillis();
            }

            this.hovered = true;
            this.hoverTime = (int) (Util.getMillis() - this.hoverStartTime);
        } else {
            if (this.hovered) {
                this.hoverStopTime = Util.getMillis();
            }
            this.hovered = false;
            this.hoverTime = 0;
        }
    }

    public boolean isHovered() {
        return hovered;
    }

    public void playDownSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.isPointInside(mouseX, mouseY);
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return !this.isFocused() ? ComponentPath.leaf(this) : null;
    }

    @Override
    public NarratableEntry.@NotNull NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        }
        if (this.isHovered()) {
            return NarratableEntry.NarrationPriority.HOVERED;
        }

        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        if (this.isFocused()) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        } else if (this.hovered) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        }
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }

    public Dimension<Integer> getDim() {
        return this.dim;
    }
}
