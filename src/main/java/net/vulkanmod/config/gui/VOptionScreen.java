package net.vulkanmod.config.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.config.gui.widget.VButtonWidget;
import net.vulkanmod.config.option.OptionPage;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.vulkan.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class VOptionScreen extends Screen {
    final ResourceLocation ICON = new ResourceLocation("vulkanmod", "vlogo_transparent.png");

    private final Screen parent;

    private final List<OptionPage> optionPages;

    private int currentListIdx = 0;

    private final int tooltipBoxWidth = 200;

    private VButtonWidget supportButton;

    private VButtonWidget doneButton;
    private VButtonWidget applyButton;

    private final List<VButtonWidget> pageButtons = Lists.newArrayList();
    private final List<VButtonWidget> buttons = Lists.newArrayList();

    public VOptionScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;

        this.optionPages = new ArrayList<>();
    }

    private void addPages() {
        this.optionPages.clear();

        OptionPage page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.video").getString(),
                Options.getVideoOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.graphics").getString(),
                Options.getGraphicsOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.optimizations").getString(),
                Options.getOptimizationOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.other").getString(),
                Options.getOtherOpts()
        );
        this.optionPages.add(page);
    }

    @Override
    protected void init() {
        this.addPages();

        int top = 40;
        int bottom = 40;
        int itemHeight = 20;

        int leftMargin = 100;
//        int listWidth = (int) (this.width * 0.65f);
        int listWidth = Math.min((int) (this.width * 0.65f), 420);
        int listHeight = this.height - top - bottom;

        this.buildLists(leftMargin, top, listWidth, listHeight, itemHeight);

        int x = leftMargin + listWidth + 10;
//        int width = Math.min(this.width - this.tooltipX - 10, 200);
        int width = this.width - x - 10;
        int y = 50;

        if (width < 200) {
            x = 100;
            width = listWidth;
            y = this.height - bottom + 10;
        }

        buildPage();

        this.applyButton.active = false;
    }

    private void buildLists(int left, int top, int listWidth, int listHeight, int itemHeight) {
        for (OptionPage page : this.optionPages) {
            page.createList(left, top, listWidth, listHeight, itemHeight);
        }
    }

    private void addPageButtons(int x0, int y0, int width, int height, boolean verticalLayout) {
        int x = x0;
        int y = y0;
        for (int i = 0; i < this.optionPages.size(); ++i) {
            var page = this.optionPages.get(i);
            final int finalIdx = i;
            VButtonWidget widget = new VButtonWidget(x, y, width, height, Component.nullToEmpty(page.name), button -> this.setOptionList(finalIdx));
            this.buttons.add(widget);
            this.pageButtons.add(widget);
            this.addWidget(widget);

            if (verticalLayout)
                y += height + 1;
            else
                x += width + 1;
        }

        this.pageButtons.get(this.currentListIdx).setSelected(true);
    }

    private void buildPage() {
        this.buttons.clear();
        this.pageButtons.clear();
        this.clearWidgets();

//        this.addPageButtons(20, 6, 60, 20, false);
        this.addPageButtons(10, 40, 80, 22, true);

        VOptionList currentList = this.optionPages.get(this.currentListIdx).getOptionList();
        this.addWidget(currentList);

        this.addButtons();
    }

    private void addButtons() {
        int rightMargin = 20;
        int buttonHeight = 20;
        int padding = 10;
        int buttonMargin = 5;
        int buttonWidth = minecraft.font.width(CommonComponents.GUI_DONE) + 2 * padding;
        int x0 = (this.width - buttonWidth - rightMargin);
        int y0 = this.height - buttonHeight - 7;

        this.doneButton = new VButtonWidget(
                x0, y0,
                buttonWidth, buttonHeight,
                CommonComponents.GUI_DONE,
                button -> this.minecraft.setScreen(this.parent)
        );

        buttonWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.apply")) + 2 * padding;
        x0 -= (buttonWidth + buttonMargin);
        this.applyButton = new VButtonWidget(
                x0, y0,
                buttonWidth, buttonHeight,
                Component.translatable("vulkanmod.options.buttons.apply"),
                button -> this.applyOptions()
        );

        buttonWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + 10;
        x0 = (this.width - buttonWidth - rightMargin);
        this.supportButton = new VButtonWidget(
                x0, 6,
                buttonWidth, buttonHeight,
                Component.translatable("vulkanmod.options.buttons.kofi"),
                button -> Util.getPlatform().openUri("https://ko-fi.com/xcollateral")
        );

        this.buttons.add(this.applyButton);
        this.buttons.add(this.doneButton);
        this.buttons.add(this.supportButton);

        this.addWidget(this.applyButton);
        this.addWidget(this.doneButton);
        this.addWidget(this.supportButton);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener element : this.children()) {
            if (element.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(element);
                if (button == 0) {
                    this.setDragging(true);
                }

                this.updateState();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.setDragging(false);
        this.updateState();
        return this.getChildAt(mouseX, mouseY)
                .filter(guiEventListener -> guiEventListener.mouseReleased(mouseX, mouseY, button))
                .isPresent();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        if (this.minecraft.level != null) {
            this.renderTransparentBackground(guiGraphics);
        } else {
            this.renderDirtBackground(guiGraphics);
            RenderSystem.enableBlend();
            GuiRenderer.fillGradient(0, 0, this.width, this.height,
                    ColorUtil.ARGB.pack(0.0f, 0.0f, 0.0f, 0.2f), ColorUtil.ARGB.pack(0.0f, 0.0f, 0.0f, 0.3f));
        }

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiRenderer.guiGraphics = guiGraphics;
        GuiRenderer.setPoseStack(guiGraphics.pose());

        this.renderBackground(guiGraphics, 0, 0, delta);

        RenderSystem.enableBlend();

        int size = font.lineHeight * 4;
        guiGraphics.blit(ICON, 30, 4, 0f, 0f, size, size, size, size);

        VOptionList currentList = this.optionPages.get(this.currentListIdx).getOptionList();
        currentList.updateState(mouseX, mouseY);
        currentList.renderWidget(mouseX, mouseY);
        renderButtons(mouseX, mouseY);

        List<FormattedCharSequence> tooltip = getHoveredButtonTooltip(currentList, mouseX, mouseY);
        if (tooltip != null) {
            this.renderTooltip(tooltip, mouseX, mouseY);
        }
    }

    public void renderButtons(int mouseX, int mouseY) {
        for (VButtonWidget button : buttons) {
            button.render(mouseX, mouseY);
        }
    }

    private void renderTooltip(List<FormattedCharSequence> tooltip, int mouseX, int mouseY) {
        int textPadding = 3;
        int boxPadding = 3;

        int mouseCursorMargin = 10;

        int boxX = mouseX + mouseCursorMargin;
        int boxY = mouseY + mouseCursorMargin;

        int tooltipBoxHeight = (tooltip.size() * 12) + boxPadding;

        int backgroundColor = ColorUtil.ARGB.pack(0.05f, 0.05f, 0.05f, 0.7f);
        int borderColor = ColorUtil.RED;
        int textColor = ColorUtil.WHITE;

        // Flip the tooltip horizontally if it goes off the right edge
        if (boxX + tooltipBoxWidth > this.width) {
            boxX = mouseX - mouseCursorMargin - tooltipBoxWidth;
        }

        // Flip the tooltip vertically if it goes off the bottom edge
        if (boxY + tooltipBoxHeight > this.height) {
            boxY = mouseY - mouseCursorMargin - tooltipBoxHeight;
        }

        // Ensure the tooltip doesn't go off the left edge (in case settings layout changes)
        if (boxX < 0) {
            boxX = Math.max(0, mouseX - mouseCursorMargin - tooltipBoxWidth);
        }

        // Ensure the tooltip doesn't go off the top edge
        if (boxY < 0) {
            boxY = Math.max(0, mouseY - mouseCursorMargin - tooltipBoxHeight);
        }

        GuiRenderer.fill(boxX, boxY, boxX + this.tooltipBoxWidth, boxY + tooltipBoxHeight, 1, backgroundColor);
        GuiRenderer.renderBorder(boxX, boxY, boxX + this.tooltipBoxWidth, boxY + tooltipBoxHeight, 1, 1, borderColor);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < tooltip.size(); i++) {
            GuiRenderer.drawString(this.font, tooltip.get(i), bufferSource, boxX + textPadding, boxY + textPadding + (i * 12), 1, textColor);
        }

        bufferSource.endBatch();
    }


    private List<FormattedCharSequence> getHoveredButtonTooltip(VOptionList buttonList, int mouseX, int mouseY) {
        VAbstractWidget widget = buttonList.getHoveredWidget(mouseX, mouseY);
        if (widget != null) {
            var tooltip = widget.getTooltip();
            if (tooltip == null)
                return null;

            return this.font.split(tooltip, this.tooltipBoxWidth);
        }
        return null;
    }

    private void updateState() {
        boolean modified = false;
        for (var page : this.optionPages) {
            modified |= page.optionChanged();
        }

        this.applyButton.active = modified;
    }

    private void setOptionList(int i) {
        this.currentListIdx = i;

        this.buildPage();

        this.pageButtons.get(i).setSelected(true);
    }

    private void applyOptions() {
        List<OptionPage> pages = List.copyOf(this.optionPages);
        for (var page : pages) {
            page.applyOptionChanges();
        }

        Initializer.CONFIG.write();
    }
}
