package net.vulkanmod.config.gui.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.config.gui.ConfigScreen;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.option.OptionPage;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.widget.ButtonWidget;
import net.vulkanmod.vulkan.util.ColorUtil;

import java.util.List;

public class PageList extends AbstractWidgetContainer {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("vulkanmod", "vlogo_transparent.png");
    private static final int ICON_SIZE = GuiConstants.WIDGET_HEIGHT - 3;

    private static OptionPage currentPage;

    private final List<OptionPage> pages;

    public PageList(Dimension<Integer> dim, ConfigScreen screen, List<OptionPage> pages) {
        super(dim, screen);
        this.pages = pages;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        RenderSystem.enableBlend();

        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_BLACK, 0.45f);
        GuiRenderer.fill(
                getDim().x(), getDim().y(),
                getDim().xLimit(), getDim().y() + GuiConstants.WIDGET_HEIGHT + GuiConstants.WIDGET_MARGIN,
                backgroundColor);

        GuiRenderer.guiGraphics.blit(
                ICON,
                getDim().centerX() - ICON_SIZE / 2, getDim().y() + (((GuiConstants.WIDGET_HEIGHT + GuiConstants.WIDGET_MARGIN) - ICON_SIZE) / 2),
                0, 0,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    @Override
    protected void addChildren() {
        if (pages == null) return;

        int topY = getDim().y() + GuiConstants.WIDGET_HEIGHT + GuiConstants.WIDGET_MARGIN;
        for (OptionPage page : pages) {
            Dimension<Integer> buttonDim = Dimension.ofInt(
                    getDim().x(), topY,
                    getDim().width(), GuiConstants.WIDGET_HEIGHT);

            ButtonWidget button = new ButtonWidget(
                    buttonDim,
                    page.name(),
                    () -> setPage(page),
                    true);

            button.setSelected(page == getCurrentPage());

            children().add(button);

            topY += GuiConstants.WIDGET_HEIGHT;
        }
    }

    public OptionPage getCurrentPage() {
        if (pages.isEmpty())
            currentPage = OptionPage.getDummy();
        if (currentPage == null || (!pages.contains(currentPage) && !pages.isEmpty()))
            currentPage = pages.getFirst();

        return currentPage;
    }

    public void setPage(OptionPage currentPage) {
        PageList.currentPage = currentPage;

        getScreen().rebuildWidgets();
    }
}
