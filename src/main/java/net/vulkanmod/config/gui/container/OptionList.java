package net.vulkanmod.config.gui.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.vulkanmod.config.gui.ConfigScreen;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.option.OptionGroup;
import net.vulkanmod.config.gui.option.OptionPage;
import net.vulkanmod.config.gui.option.control.Controller;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.util.dim.IntDimension;
import net.vulkanmod.config.gui.widget.ControllerWidget;
import net.vulkanmod.vulkan.util.ColorUtil;

import java.util.List;

public class OptionList extends AbstractScrollableList {
    private static final int TOOLTIP_WIDTH = 170;

    private final OptionPage page;

    private long lastTooltipTime = 0;

    public OptionList(Dimension<Integer> dim, ConfigScreen screen, OptionPage page) {
        super(dim, screen);

        this.page = page;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        ControllerWidget<?> widget = (ControllerWidget<?>)
                (getFocused() != null
                        ? getFocused()
                        : getHovered());
        if (widget != null && widget.getOption().tooltip() != null) {
            if (lastTooltipTime == 0)
                lastTooltipTime = Util.getMillis();

            renderWidgetTooltip(widget);
        } else {
            lastTooltipTime = 0;
        }
    }

    private void renderWidgetTooltip(ControllerWidget<?> widget) {
        if ((lastTooltipTime + 500) > Util.getMillis()
                || widget == null
                || widget.getOption().tooltip() == null
                || (!widget.isHovered() && !widget.isFocused()))
            return;

        GuiRenderer.guiGraphics.enableScissor(
                getDim().x(), getDim().y(),
                getDim().xLimit(), getDim().yLimit());

        int padding = 3;

        List<FormattedCharSequence> tooltip = GuiRenderer.font.split(
                widget.getOption().tooltip(),
                TOOLTIP_WIDTH - padding * 2);

        Dimension<Integer> tooltipDim = Dimension.ofInt(
                widget.getDim().xLimit() - TOOLTIP_WIDTH, widget.getDim().yLimit() + 3,
                TOOLTIP_WIDTH, (tooltip.size() * 12) + padding);

        int backgroundColor1 = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_BLACK, 0.8f);
        int backgroundColor2 = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_DARK_RED, 0.8f);
        int borderColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_RED, 0.8f);
        int textColor = GuiConstants.COLOR_WHITE;

        // Ensure the tooltip doesn't go off any edges
        if ((tooltipDim.yLimit() - getScrollAmount()) > getDim().yLimit())
            tooltipDim.setY(widget.getDim().y() - tooltipDim.height() - 3);
        if ((tooltipDim.y() - getScrollAmount()) < 0)
            tooltipDim.setY(widget.getDim().yLimit());

        RenderSystem.enableBlend();

        GuiRenderer.guiGraphics.pose().pushPose();
        GuiRenderer.guiGraphics.pose().translate(0, -getScrollAmount(), 0);

        GuiRenderer.fillGradient(
                tooltipDim.x() + 1, tooltipDim.y() + 1,
                tooltipDim.xLimit() - 1, tooltipDim.yLimit() - 1,
                90, backgroundColor1, backgroundColor2);
        GuiRenderer.renderBorder(
                tooltipDim.x(), tooltipDim.y(),
                tooltipDim.xLimit(), tooltipDim.yLimit(),
                90, 1, borderColor);

        for (int i = 0; i < tooltip.size(); i++) {
            GuiRenderer.drawString(
                    tooltip.get(i),
                    tooltipDim.x() + padding, tooltipDim.y() + padding + (i * 12),
                    90, textColor);
        }

        GuiRenderer.guiGraphics.pose().popPose();
        GuiRenderer.guiGraphics.disableScissor();
    }

    @Override
    public void addChildren() {
        if (page == null) return;

        int x = getDim().x();
        int topY = getDim().y();
        for (OptionGroup group : page.groups()) {
            if (group.options().isEmpty()) continue;
            for (Option<?> option : group.options()) {
                IntDimension widgetDim = Dimension.ofInt(x, topY, getDim().width(), GuiConstants.WIDGET_HEIGHT);
                Controller<?> controller = option.controller();
                ControllerWidget<?> controllerWidget = controller.createWidget(widgetDim);

                children().add(controllerWidget);

                topY += GuiConstants.WIDGET_HEIGHT;
            }

            topY += GuiConstants.WIDGET_MARGIN;
        }
    }

    @Override
    public int getTotalHeight() {
        int groupsMarginHeight = (page.groups().size() - 1) * GuiConstants.WIDGET_MARGIN;
        int widgetsHeight = page.options().size() * GuiConstants.WIDGET_HEIGHT;
        return groupsMarginHeight + widgetsHeight;
    }
}
