package net.vulkanmod.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.widget.CustomButtonWidget;
import net.vulkanmod.config.widget.OptionWidget;
import net.vulkanmod.vulkan.util.VUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class OptionScreenV extends Screen {
    private final List<OptionList2> optionLists;
    private Option<?>[] videoOpts;
    private Option<?>[] graphicsOpts;
    private Option<?>[] otherOpts;
    private final Screen parent;
    private OptionList2 currentList;

    private CustomButtonWidget videoButton;
    private CustomButtonWidget graphicsButton;
    private CustomButtonWidget otherButton;

    private CustomButtonWidget supportButton;

    private CustomButtonWidget doneButton;
    private CustomButtonWidget applyButton;

    private final List<CustomButtonWidget> buttons = Lists.newArrayList();

    public OptionScreenV(Component title, Screen parent) {
        super(title);
        this.parent = parent;

        this.optionLists = new ArrayList<>();
        this.videoOpts = Options.getVideoOpts();
        this.graphicsOpts = Options.getGraphicsOpts();
        this.otherOpts = Options.getOtherOpts();

    }

    @Override
    protected void init() {
        this.currentList = null;

        int top = 30;
        int bottom = 32;

        this.optionLists.clear();
        OptionList2 optionList = new OptionList2(this.minecraft, this.width, this.height, top, this.height - bottom, 25);
        optionList.addAll(this.videoOpts);
        this.optionLists.add(optionList);
        optionList = new OptionList2(this.minecraft, this.width, this.height, top, this.height - bottom, 25);
        optionList.addAll(this.graphicsOpts);
        this.optionLists.add(optionList);
        optionList = new OptionList2(this.minecraft, this.width, this.height, top, this.height - bottom, 25);
        optionList.addAll(this.otherOpts);
        this.optionLists.add(optionList);

        this.videoButton = new CustomButtonWidget(20, 6, 60, 20, Component.literal("Video"), button -> this.setOptionList(button, optionLists.get(0)));
        this.graphicsButton = new CustomButtonWidget(81, 6, 60, 20, Component.literal("Graphics"), button -> this.setOptionList(button, optionLists.get(1)));
        this.otherButton = new CustomButtonWidget(142, 6, 60, 20, Component.literal("Other"), button -> this.setOptionList(button, optionLists.get(2)));

        this.supportButton = new CustomButtonWidget(this.width - 80, 6, 70, 20, Component.literal("Support me"),
                button -> Util.getPlatform().openUri("https://ko-fi.com/xcollateral"));

        this.videoButton.setSelected(true);

        buildPage();

//        OptionWidget widget = new OptionWidget(this.width / 8, this.height / 2, 100, 20, Component.of("TEST")){};
//
//        this.addSelectableChild(widget);
//        this.addDrawableChild(widget);

        this.applyButton.active = false;
    }

    private void buildPage() {
        this.buttons.clear();
        this.clearWidgets();

        if(this.currentList == null)
            this.currentList = optionLists.get(0);

        this.addWidget(currentList);

        this.buildHeader();

        this.addButtons();
    }

    private void buildHeader() {
        buttons.add(this.videoButton);
        buttons.add(this.graphicsButton);
        buttons.add(this.otherButton);
        buttons.add(this.supportButton);

        this.addWidget(this.videoButton);
        this.addWidget(this.graphicsButton);
        this.addWidget(this.otherButton);
        this.addWidget(this.supportButton);
    }

    private void addButtons() {
        int buttonX = (this.width - 150);
        int buttonGap = 55;
        this.applyButton = new CustomButtonWidget(buttonX, this.height - 27, 50, 20, Component.literal("Apply"), button -> {
            Options.applyOptions(Initializer.CONFIG, new Option[][]{this.videoOpts, this.graphicsOpts, this.otherOpts});
        });
        this.doneButton = new CustomButtonWidget(buttonX + buttonGap, this.height - 27, 50, 20, CommonComponents.GUI_DONE, button -> {
            this.minecraft.setScreen(this.parent);
        });

        buttons.add(this.applyButton);
        buttons.add(this.doneButton);

        this.addWidget(this.applyButton);
        this.addWidget(this.doneButton);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener element : this.children()) {
            if (!element.mouseClicked(mouseX, mouseY, button)) continue;
            this.setFocused(element);
            if (button == 0) {
                this.setDragging(true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.updateStatus();

        this.currentList.render(guiGraphics, mouseX, mouseY, delta);
        renderButtons(guiGraphics, mouseX, mouseY, delta);

        List<FormattedCharSequence> list = getHoveredButtonTooltip(this.currentList, mouseX, mouseY);
        if (list != null) {
            guiGraphics.renderTooltip(this.minecraft.font, list, mouseX, mouseY);
        }
    }

    public void renderButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        for (Renderable renderable : buttons) {
            renderable.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    private List<FormattedCharSequence> getHoveredButtonTooltip(OptionList2 buttonList, int mouseX, int mouseY) {
        Optional<OptionWidget> optional = buttonList.getHoveredButton(mouseX, mouseY);
        if (optional.isPresent()) {
            if(optional.get().getTooltip() == null) return null;
            return this.minecraft.font.split(optional.get().getTooltip(), 200);
        }
        return ImmutableList.of();
    }

    public void updateStatus() {

        boolean modified = false;
        for(Option<?> option: this.videoOpts) {
            if(option.isModified()) modified = true;
        }
        for(Option<?> option: this.graphicsOpts) {
            if(option.isModified()) modified = true;
        }
        for(Option<?> option: this.otherOpts) {
            if(option.isModified()) modified = true;
        }

        this.applyButton.active = modified;
    }

    public void setOptionList(CustomButtonWidget button, OptionList2 optionList) {
        this.currentList = optionList;

        this.buildPage();

        this.videoButton.setSelected(false);
        this.graphicsButton.setSelected(false);
        this.otherButton.setSelected(false);

        button.setSelected(true);
    }
}
