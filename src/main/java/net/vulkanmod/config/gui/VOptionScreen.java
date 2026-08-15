package net.vulkanmod.config.gui;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.SearchHelper;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.config.gui.widget.*;
import net.vulkanmod.config.option.*;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class VOptionScreen extends Screen {
    public final static int MARGIN = 10;

    private final Screen parent;

    private final List<ModSettingsEntry> modSettingsEntries;

    private final List<OptionPage> optionPages;
    private OptionPage searchResultsPage;

    private int currentListIdx = 0;
    private boolean isSearchActive = false;

    private int tooltipX;
    private int tooltipY;
    private int tooltipWidth;

    private VButtonWidget applyButton;
    private VButtonWidget undoButton;

    private VTextInputWidget searchField;

    private VPageList pageList;

    private final List<VButtonWidget> pageButtons = Lists.newArrayList();


    public VOptionScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;

        this.optionPages = new ArrayList<>();
        this.modSettingsEntries = new ArrayList<>(ModSettingsRegistry.INSTANCE.getModEntries());
    }

    @Override
    protected void init() {
        this.initOptionsPages();

        if (this.optionPages.isEmpty()) {
            throw new IllegalStateException("Default Options weren't added!");
        }

        int top = 32;
        int bottom = 60;
        int itemHeight = 20;

        int leftMargin = MARGIN + VGuiConstants.PAGE_BUTTON_WIDTH + 6;
        int listWidth = Math.min(this.width - leftMargin - MARGIN, 420);
        int listHeight = this.height - top - bottom;

        this.buildLists(leftMargin, top, listWidth, listHeight, itemHeight);

        this.searchField = createSearchField();

        int x = leftMargin + listWidth + 6;
        int tooltipWidth = Math.min(this.width - x - 10, 420);
        int y = top + itemHeight + 6;

        if (tooltipWidth < 200) {
            x = leftMargin + 3;
            tooltipWidth = listWidth;
            y = this.height - bottom + 10;
        }

        this.tooltipX = x;
        this.tooltipY = y;
        this.tooltipWidth = tooltipWidth;

        this.buildPage();

        this.applyButton.active = false;
        this.undoButton.visible = false;
    }

    private void initOptionsPages() {
        this.optionPages.clear();

        for (var modPageSet : this.modSettingsEntries) {
            modPageSet.initPages();

            this.optionPages.addAll(modPageSet.getPages());
        }
    }

    private VTextInputWidget createSearchField() {
        int rightMargin = 10;
        int padding = 10;
        int kofiWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + padding;
        int topBarRight = this.width - kofiWidth - rightMargin;

        if (UpdateChecker.isUpdateAvailable()) {
            int updateWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.update_available")) + padding;
            topBarRight -= updateWidth + VGuiConstants.WIDGET_MARGIN;
        }


        int leftMargin = VGuiConstants.PAGE_BUTTON_WIDTH + MARGIN + 6;
        int width = Math.min(topBarRight - leftMargin - 4, 413);

        return new VTextInputWidget(
                leftMargin, 4,
                width, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.searchFieldPlaceholder"),
                widget -> performSearch(widget.getInput())
        );
    }

    private void buildLists(int left, int top, int listWidth, int listHeight, int itemHeight) {
        for (OptionPage page : this.optionPages) {
            page.createList(left, top, listWidth, listHeight, itemHeight);
            page.updateOptionStates();
        }
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            isSearchActive = false;
            this.currentListIdx = 0;
            buildPage();
            return;
        }

        String searchTerm = query.toLowerCase().trim();
        List<OptionBlock> searchResults = new ArrayList<>();

        for (OptionPage page : this.optionPages) {
            List<Option<?>> matchingOptions = new ArrayList<>();

            for (OptionBlock block : page.optionBlocks) {
                for (Option<?> option : block.options()) {
                    boolean matches = false;

                    String optionName = option.getName().getString().toLowerCase();
                    String optionTooltip = option.getTooltip() != null ?
                            option.getTooltip().getString().toLowerCase() : "";
                    String displayedValue = option.getDisplayedValue().getString().toLowerCase();

                    if (optionName.contains(searchTerm) ||
                            optionTooltip.contains(searchTerm) ||
                            displayedValue.contains(searchTerm)) {
                        matches = true;
                    }

                    else if (option instanceof CyclingOption<?> cycling) {
                        if (SearchHelper.matchesAnyValue(cycling, searchTerm)) {
                            matches = true;
                        }
                    }

                    if (matches) {
                        matchingOptions.add(option);
                    }
                }
            }

            if (!matchingOptions.isEmpty()) {
                searchResults.add(new OptionBlock("§l" + page.name,
                        matchingOptions.toArray(new Option<?>[0])));
                searchResults.add(new OptionBlock("", new Option<?>[0]));
            }
        }

        searchResultsPage = new OptionPage(
                "Search Results",
                searchResults.toArray(new OptionBlock[0])
        );

        int top = 32;
        int bottom = 60;
        int itemHeight = 20;
        int leftMargin = MARGIN + VGuiConstants.PAGE_BUTTON_WIDTH + 6;
        int listWidth = Math.min(this.width - leftMargin - MARGIN, 420);
        int listHeight = this.height - top - bottom;

        searchResultsPage.createList(leftMargin, top, listWidth, listHeight, itemHeight);

        isSearchActive = true;
        buildPage();
    }

    private void buildPage() {
        this.pageButtons.clear();

        String savedInput = this.searchField != null ? this.searchField.getInput() : "";
        boolean savedFocused = this.searchField != null && this.searchField.focused;
        boolean savedSelected = this.searchField != null && this.searchField.selected;

        this.clearWidgets();

        this.pageList = new VPageList(MARGIN, 4, VGuiConstants.PAGE_BUTTON_WIDTH, this.height - MARGIN, this::setOptionList);
        this.pageList.addAll(this.modSettingsEntries);
        this.addWidget(this.pageList);

        if (!isSearchActive) {
            this.pageList.buttons.get(this.currentListIdx).setSelected(true);
            VOptionList currentList = this.optionPages.get(this.currentListIdx).getOptionList();
            this.addWidget(currentList);
        } else {
            if (searchResultsPage != null) {
                VOptionList searchList = searchResultsPage.getOptionList();
                this.addWidget(searchList);
                searchResultsPage.updateOptionStates();
            }
        }

        this.addButtonsWithSearchBar();

        this.searchField.setInput(savedInput);
        if (savedFocused) {
            this.searchField.setFocused(true);
            this.searchField.setSelected(savedSelected);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void addButtonsWithSearchBar() {
        int rightMargin = 10;
        int padding = 10;
        int buttonWidth = Minecraft.getInstance().font.width(CommonComponents.GUI_DONE) + 2 * padding;
        int x0 = (this.width - buttonWidth - rightMargin);
        int y0 = this.height - VGuiConstants.WIDGET_HEIGHT - 7;

        VButtonWidget doneButton = new VButtonWidget(x0, y0, buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                CommonComponents.GUI_DONE, button -> Minecraft.getInstance().setScreen(this.parent));

        buttonWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.apply")) + 2 * padding;
        x0 -= (buttonWidth + VGuiConstants.WIDGET_MARGIN);
        this.applyButton = new VButtonWidget(x0, y0, buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.apply"), button -> this.applyOptions());

        buttonWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.undo")) + 2 * padding;
        x0 -= (buttonWidth + VGuiConstants.WIDGET_MARGIN);
        this.undoButton = new VButtonWidget(x0, y0, buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.undo"), button -> undo());

        int kofiWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + padding;

        int kofiX = this.width - kofiWidth - rightMargin;
        VButtonWidget supportButton = new VButtonWidget(kofiX, 4, kofiWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.kofi"),
                button -> Util.getPlatform().openUri("https://ko-fi.com/xcollateral"));

        this.pageButtons.add(this.applyButton);
        this.pageButtons.add(doneButton);
        this.pageButtons.add(supportButton);
        this.pageButtons.add(this.undoButton);

        this.addWidget(this.applyButton);
        this.addWidget(doneButton);
        this.addWidget(supportButton);
        this.addWidget(this.undoButton);
        this.addWidget(this.searchField);

        if (UpdateChecker.isUpdateAvailable()) {
            assert minecraft != null;
            int updateWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.update_available")) + padding;
            var updateButton = new VButtonWidget(
                    kofiX - updateWidth - VGuiConstants.WIDGET_MARGIN, 4,
                    updateWidth, VGuiConstants.WIDGET_HEIGHT,
                    Component.translatable("vulkanmod.options.buttons.update_available").withStyle(ChatFormatting.UNDERLINE),
                    button -> Util.getPlatform().openUri("https://modrinth.com/mod/vulkanmod")
            );
            this.pageButtons.add(updateButton);
            this.addWidget(updateButton);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        for (GuiEventListener element : this.children()) {
            if (element.mouseClicked(event, bl)) {
                this.setFocused(element);
                if (event.button() == 0) {
                    this.setDragging(true);
                }

                this.updateState();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.setDragging(false);
        this.updateState();
        return this.getChildAt(event.x(), event.y())
                   .filter(guiEventListener -> guiEventListener.mouseReleased(event))
                   .isPresent();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiRenderer.guiGraphics = guiGraphics;
        VRenderSystem.enableBlend();

        VOptionList currentList;
        if (isSearchActive && searchResultsPage != null) {
            currentList = searchResultsPage.getOptionList();
        } else {
            currentList = this.optionPages.get(this.currentListIdx).getOptionList();
        }

        currentList.updateState(mouseX, mouseY);
        currentList.renderWidget(mouseX, mouseY);

        pageList.updateState(mouseX, mouseY);
        pageList.renderWidget(mouseX, mouseY);

        for (VButtonWidget button : pageButtons) {
            button.updateState(mouseX, mouseY);
            button.render(mouseX, mouseY);
        }

        searchField.updateState(mouseX, mouseY);
        searchField.render(mouseX, mouseY);

        VAbstractWidget hoveredWidget = null;

        for (VButtonWidget button : pageButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                hoveredWidget = button;
                break;
            }
        }

        if (hoveredWidget == null) {
            hoveredWidget = currentList.getHoveredWidget(mouseX, mouseY);
        }

        if (hoveredWidget != null) {
            this.renderTooltip(hoveredWidget, this.tooltipX, this.tooltipY);
        }
    }

    private void renderTooltip(VAbstractWidget widget, int x, int y) {
        var list = this.getWidgetTooltip(widget);

        if (list.isEmpty()) {
            return;
        }

        int lines = list.size();

        int padding = 3;
        int width = GuiRenderer.getMaxTextWidth(this.font, list);
        int height = lines * 10;
        float intensity = 0.05f;
        int color = ColorUtil.ARGB.pack(intensity, intensity, intensity, 0.6f);
        GuiRenderer.fill(x - padding, y - padding, x + width + padding, y + height + padding, color);

        color = VGuiConstants.COLOR_RED;
        GuiRenderer.renderBorder(x - padding, y - padding, x + width + padding, y + height + padding, 1, color);

        int yOffset = 0;
        for (var text : list) {
            GuiRenderer.drawString(this.font, text, x, y + yOffset, 0xffffffff);
            yOffset += 10;
        }
    }

    private List<FormattedCharSequence> getWidgetTooltip(VAbstractWidget widget) {
        var tooltip = widget.getTooltip();
        var impact = widget.getImpact();

        List<FormattedCharSequence> textList = new ArrayList<>();
        if (tooltip != null) {
            textList.addAll(this.font.split(tooltip, this.tooltipWidth));
        }

        if (impact != null) {
            textList.addAll(this.font.split(Component.translatable("Performance Impact: %s", impact.component()), this.tooltipWidth));
        }

        return textList;
    }

    private void updateState() {
        if (this.applyButton == null | this.undoButton == null) return;
        boolean modified = false;
        for (var page : this.optionPages) {
            modified |= page.optionChanged();
        }

        if (modified) {
            for (var page : this.optionPages) {
                page.optionChanged();
            }
        }

        this.applyButton.active = modified;
        this.undoButton.visible = modified;
    }

    private void setOptionList(int i) {
        this.currentListIdx = i;
        this.isSearchActive = false;

        this.searchField.setInput("");
        this.searchField.setFocused(false);

        this.buildPage();

        this.pageList.buttons.get(i).setSelected(true);
    }

    private void undo() {
        for (OptionPage page : this.optionPages) {
            page.resetToOriginalState();
            page.updateOptionStates();
        }

        buildPage();
    }

    private void applyOptions() {
        List<OptionPage> pages = List.copyOf(this.optionPages);
        for (var page : pages) {
            page.applyOptionChanges();
            page.updateOptionStates();
        }

        for (var modEntry : this.modSettingsEntries) {
            modEntry.runOnApply();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_L) {
            this.setFocused(searchField);
            searchField.setFocused(true);
            searchField.setSelected(true);

            return true;
        }

        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE && this.isSearchActive) {
            this.isSearchActive = false;
            this.searchField.setInput("");
            this.searchField.setFocused(false);
            this.buildPage();
            this.pageButtons.get(this.currentListIdx).setSelected(true);
            return true;
        }


        if (!this.searchField.focused
                && keyEvent.key() == GLFW.GLFW_KEY_P
                && keyEvent.hasShiftDown()) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this, Minecraft.getInstance(), Minecraft.getInstance().options));

            return false;
        }

        return super.keyPressed(keyEvent);
    }
}