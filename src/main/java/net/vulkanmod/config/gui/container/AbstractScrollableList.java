package net.vulkanmod.config.gui.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.ConfigScreen;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractScrollableList extends AbstractWidgetContainer {
    private final Dimension<Integer> viewPortDim;
    private int scrollAmount = 0;

    public AbstractScrollableList(Dimension<Integer> dim, ConfigScreen screen) {
        super(dim, screen);
        this.viewPortDim = dim.copy();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiRenderer.guiGraphics.enableScissor(
                viewPortDim.x(), viewPortDim.y(),
                viewPortDim.xLimit(), viewPortDim.yLimit());

        if (getMaxScrollAmount() > 0) {
            getDim().setWidth(viewPortDim.width() - 5);
            renderScrollBar();
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -getScrollAmount(), 0);

        super.render(guiGraphics, mouseX, mouseY + getScrollAmount(), delta);

        guiGraphics.pose().popPose();

        GuiRenderer.guiGraphics.disableScissor();
    }

    private void renderScrollBar() {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_BLACK, 0.45f);
        int barColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, 0.6f);

        float barHeight = Mth.clamp((float) (viewPortDim.height() * viewPortDim.height()) / getTotalHeight(), 32, viewPortDim.height() - 8);
        float barY = Math.max(this.getScrollAmount() * (viewPortDim.height() - barHeight) / getMaxScrollAmount() + viewPortDim.y(), viewPortDim.y());

        Dimension<Float> scrollBarBoxDim = Dimension.ofFloat(
                getDim().xLimit() + 2, viewPortDim.y(),
                3, viewPortDim.yLimit());
        Dimension<Float> scrollBarDim = Dimension.ofFloat(
                scrollBarBoxDim.x(), barY,
                scrollBarBoxDim.width(), barHeight);

        GuiRenderer.fill(
                scrollBarBoxDim.x(), scrollBarBoxDim.y(),
                scrollBarBoxDim.xLimit(), scrollBarBoxDim.yLimit(),
                backgroundColor);
        GuiRenderer.fill(
                scrollBarDim.x(), scrollBarDim.y(),
                scrollBarDim.xLimit(), scrollBarDim.yLimit(),
                barColor);
    }

    public int getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(int scrollAmount) {
        this.scrollAmount = scrollAmount;
    }

    public int getMaxScrollAmount() {
        return Math.max(getTotalHeight() - viewPortDim.height(), 0);
    }

    public abstract int getTotalHeight();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY))
            return false;

        return super.mouseClicked(mouseX, mouseY + getScrollAmount(), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xScroll, double yScroll) {
        this.setScrollAmount(Mth.clamp((int) (getScrollAmount() - (yScroll * 6)), 0, getMaxScrollAmount()));
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.viewPortDim.isPointInside(mouseX, mouseY);
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.viewPortDim.x(), this.viewPortDim.y(), this.viewPortDim.width(), this.viewPortDim.height());
    }
}
