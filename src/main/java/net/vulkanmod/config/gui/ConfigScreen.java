package net.vulkanmod.config.gui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.gui.container.OptionList;
import net.vulkanmod.config.gui.container.PageList;
import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.option.OptionPage;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.SearchFieldTextModel;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.config.gui.widget.ButtonWidget;
import net.vulkanmod.config.gui.widget.SearchFieldWidget;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Predicate;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final List<OptionPage> optionPages;
    private final SearchFieldTextModel searchFieldTextModel;
    private List<OptionPage> currentOptionPages;
    private SearchFieldWidget searchField;
    private PageList pageList;
    private Dimension<Integer> pageDim, headerDim, footerDim, pageListDim, optionListDim;
    private ButtonWidget doneButton, applyButton, undoButton, kofiButton;

    public ConfigScreen(Screen parent) {
        super(Component.literal("VulkanMod Settings"));

        this.parent = parent;
        this.optionPages = List.of(
                ConfigScreenPages.video(),
                ConfigScreenPages.graphics(),
                ConfigScreenPages.optimizations(),
                ConfigScreenPages.other());
        this.currentOptionPages = optionPages;
        this.searchFieldTextModel = new SearchFieldTextModel(this::filterSearchResults);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiRenderer.guiGraphics = guiGraphics;
        GuiRenderer.pose = guiGraphics.pose();

        updateControls();

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        // Remember if the search field was previously focused since we'll lose that information after recreating the widget
        boolean wasSearchFocused = false;
        if (this.searchField != null)
            wasSearchFocused = this.searchField.isFocused();

        this.createDimensions();
        this.createHeader();
        this.createPageList();
        this.createOptionList();
        this.createFooter();

        if (wasSearchFocused) {
            this.setFocused(this.searchField);
        }
    }

    @Override
    public void rebuildWidgets() {
        this.clearWidgets();
        this.init();
    }

    private void createDimensions() {
        final float aspectRatio = 4f / 3f;
        final int minWidth = 550;

        int newWidth = this.width;
        if (newWidth > minWidth && (float) this.width / (float) this.height > aspectRatio) {
            newWidth = Math.max(minWidth, (int) (this.height * aspectRatio));
        }

        this.pageDim = Dimension.ofInt(
                (this.width - newWidth) / 2 + GuiConstants.WIDGET_MARGIN, GuiConstants.WIDGET_MARGIN,
                newWidth - GuiConstants.WIDGET_MARGIN * 2, this.height - GuiConstants.WIDGET_MARGIN * 2);
        this.headerDim = Dimension.ofInt(
                pageDim.x(), pageDim.y(),
                pageDim.width(), GuiConstants.WIDGET_HEIGHT);
        this.footerDim = Dimension.ofInt(
                pageDim.x(), pageDim.yLimit() - GuiConstants.WIDGET_HEIGHT,
                pageDim.width(), GuiConstants.WIDGET_HEIGHT);
        this.pageListDim = Dimension.ofInt(
                pageDim.x(), pageDim.y(),
                80, footerDim.y() - pageDim.y() - GuiConstants.WIDGET_MARGIN);
        this.optionListDim = Dimension.ofInt(
                pageListDim.xLimit() + GuiConstants.WIDGET_MARGIN, headerDim.yLimit() + GuiConstants.WIDGET_MARGIN,
                pageDim.xLimit() - pageListDim.xLimit() - GuiConstants.WIDGET_MARGIN, footerDim.y() - headerDim.yLimit() - GuiConstants.WIDGET_MARGIN * 2);
    }

    private void createPageList() {
        this.pageList = new PageList(pageListDim, this, currentOptionPages);
        this.addRenderableWidget(pageList);
    }

    private void createOptionList() {
        this.addRenderableWidget(new OptionList(optionListDim, this, pageList.getCurrentPage()));
    }

    private void createHeader() {
        final int padding = 10;
        final int minButtonWidth = 50;

        String kofiKey = "vulkanmod.options.buttons.kofi";
        int kofiButtonWidth = Math.max(this.font.width(Component.translatable(kofiKey)) + 2 * padding, minButtonWidth);
        this.kofiButton = new ButtonWidget(
                Dimension.ofInt(
                        this.headerDim.xLimit() - kofiButtonWidth, this.headerDim.y(),
                        kofiButtonWidth, this.headerDim.height()),
                Component.translatable(kofiKey),
                this::openDonationPage);

        Dimension<Integer> searchFieldDim = Dimension.ofInt(
                this.pageListDim.xLimit() + GuiConstants.WIDGET_MARGIN, this.headerDim.y(),
                this.headerDim.width() - this.pageListDim.width() - kofiButtonWidth - GuiConstants.WIDGET_MARGIN * 2, this.headerDim.height());

        this.searchField = new SearchFieldWidget(searchFieldDim, searchFieldTextModel);

        this.addRenderableWidget(searchField);
        this.addRenderableWidget(kofiButton);
    }

    private void createFooter() {
        final int padding = 10;
        final int minButtonWidth = 50;

        String doneKey = "gui.done";
        String applyKey = "vulkanmod.options.buttons.apply";
        String undoKey = "vulkanmod.options.buttons.undo";

        int doneButtonWidth = Math.max(this.font.width(Component.translatable(doneKey)) + 2 * padding, minButtonWidth);
        int applyButtonWidth = Math.max(this.font.width(Component.translatable(applyKey)) + 2 * padding, minButtonWidth);
        int undoButtonWidth = Math.max(this.font.width(Component.translatable(undoKey)) + 2 * padding, minButtonWidth);


        int x0 = this.footerDim.xLimit() - doneButtonWidth;
        this.doneButton = new ButtonWidget(
                Dimension.ofInt(
                        x0, this.footerDim.y(),
                        doneButtonWidth, this.footerDim.height()),
                Component.translatable(doneKey),
                this::onClose);

        x0 -= (applyButtonWidth + GuiConstants.WIDGET_MARGIN);
        this.applyButton = new ButtonWidget(
                Dimension.ofInt(
                        x0, this.footerDim.y(),
                        applyButtonWidth, this.footerDim.height()),
                Component.translatable(applyKey),
                this::applyChanges);

        x0 -= (undoButtonWidth + GuiConstants.WIDGET_MARGIN);
        this.undoButton = new ButtonWidget(
                Dimension.ofInt(
                        x0, this.footerDim.y(),
                        undoButtonWidth, this.footerDim.height()),
                Component.translatable(undoKey),
                this::undoChanges);

        this.applyButton.setActive(false);
        this.undoButton.setVisible(false);

        this.addRenderableWidget(undoButton);
        this.addRenderableWidget(applyButton);
        this.addRenderableWidget(doneButton);
    }

    private void applyChanges() {
        pageList.getCurrentPage().options().forEach(Option::apply);
        Initializer.CONFIG.write();
    }

    private void undoChanges() {
        pageList.getCurrentPage().options().forEach(Option::undo);
    }

    private void updateControls() {
        boolean hasChanges = this.optionPages.stream()
                .flatMap(page -> page.options().stream())
                .anyMatch(Option::isChanged);

        this.applyButton.setActive(hasChanges);
        this.undoButton.setVisible(hasChanges);

        if (!this.applyButton.isActive()) this.applyButton.setFocused(false);
        if (!this.undoButton.isActive()) this.undoButton.setFocused(false);
    }

    private void openDonationPage() {
        Util.getPlatform().openUri("https://ko-fi.com/xcollateral");
    }

    private void filterSearchResults(@NotNull String query) {
        Predicate<String> queryContains = string ->
                string != null && string.toLowerCase()
                        .replaceAll("\\s+", "")
                        .contains(query.toLowerCase()
                                .replaceAll("\\s+", ""));

        Predicate<Option<?>> nameFilter = option ->
                queryContains.test(option.name().getString());
        Predicate<Option<?>> tooltipFilter = option ->
                queryContains.test(option.tooltip() != null
                        ? option.tooltip().getString()
                        : null);
        Predicate<Option<?>> valueFilter = option ->
                queryContains.test(option.displayedValue().getString());

        currentOptionPages = optionPages.stream()
                .map(page -> page.filtered(nameFilter.or(tooltipFilter).or(valueFilter)))
                .filter(page -> !page.options().isEmpty())
                .toList();

        rebuildWidgets();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, button);
        if (clicked && (getFocused() instanceof PageList || // This is done so focus doesn't get "stuck" after clicking pageList buttons
                (getFocused() instanceof SearchFieldWidget searchFieldWidget
                        && !searchFieldWidget.isMouseOver(mouseX, mouseY)))) {
            setFocused(null);
        }

        return clicked;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((modifiers & (Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL)) != 0 && keyCode == GLFW.GLFW_KEY_L) {
            this.setFocused(searchField);

            return true;
        }

        if (!(getFocused() instanceof SearchFieldWidget)
                && keyCode == GLFW.GLFW_KEY_P
                && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this, Minecraft.getInstance(), Minecraft.getInstance().options));

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
