package net.vulkanmod.config;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.widget.CustomButtonWidget;
import net.vulkanmod.config.widget.OptionWidget;
import net.vulkanmod.vulkan.util.VUtil;

import java.util.ArrayList;
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

    private ButtonWidget doneButton;
    private ButtonWidget applyButton;

    public OptionScreenV(Text title, Screen parent) {
        super(title);
        this.parent = parent;

        this.optionLists = new ArrayList<>();
        this.videoOpts = Options.getVideoOpts();
        this.graphicsOpts = Options.getGraphicsOpts();
        this.otherOpts = Options.getOtherOpts();

        this.videoButton = new CustomButtonWidget(20, 6, 60, 20, Text.of("Video"), button -> this.setOptionList(button, optionLists.get(0)));
        this.graphicsButton = new CustomButtonWidget(81, 6, 60, 20, Text.of("Graphics"), button -> this.setOptionList(button, optionLists.get(1)));
        this.otherButton = new CustomButtonWidget(142, 6, 60, 20, Text.of("Other"), button -> this.setOptionList(button, optionLists.get(2)));

        this.videoButton.setSelected(true);

    }

    @Override
    protected void init() {

//        this.list = new OptionList2(this.client, this.width, this.height, 32, this.height - 40, 25);
////        this.list.addSingleOptionEntry(new FullscreenOption(this.client.getWindow()));
////        this.list.addSingleOptionEntry(Option.BIOME_BLEND_RADIUS);
////        this.list.addAll(OPTIONS);
//        this.list.addAll(this.options);
//
//        this.addSelectableChild(this.list);

        this.currentList = null;

        int top = 28;
        int bottom = 32;

        this.optionLists.clear();
        OptionList2 optionList = new OptionList2(this.client, this.width, this.height, top, this.height - bottom, 25);
        optionList.addAll(this.videoOpts);
        this.optionLists.add(optionList);
        optionList = new OptionList2(this.client, this.width, this.height, top, this.height - bottom, 25);
        optionList.addAll(this.graphicsOpts);
        this.optionLists.add(optionList);
        optionList = new OptionList2(this.client, this.width, this.height, top, this.height - bottom, 25);
        optionList.addAll(this.otherOpts);
        this.optionLists.add(optionList);

        buildPage();

//        OptionWidget widget = new OptionWidget(this.width / 8, this.height / 2, 100, 20, Text.of("TEST")){};
//
//        this.addSelectableChild(widget);
//        this.addDrawableChild(widget);

        this.applyButton.active = false;
    }

    private void buildPage() {
        this.clearChildren();

        if(this.currentList == null) this.currentList = optionLists.get(0);

        this.addSelectableChild(currentList);

        this.buildHeader();

        this.addButtons();
    }

    private void buildHeader() {
        this.addDrawableChild(this.videoButton);
        this.addDrawableChild(this.graphicsButton);
        this.addDrawableChild(this.otherButton);
    }

    private void addButtons() {
        int buttonX = (int) (this.width - 150);
        int buttonGap = 55;
        this.applyButton = new CustomButtonWidget(buttonX, this.height - 27, 50, 20, Text.of("Apply"), button -> {
            Options.applyOptions(Initializer.CONFIG, new Option[][]{this.videoOpts, this.graphicsOpts, this.otherOpts});
        });
        this.doneButton = new CustomButtonWidget(buttonX + buttonGap, this.height - 27, 50, 20, ScreenTexts.DONE, button -> {
            this.client.setScreen(this.parent);
        });

        this.addDrawableChild(this.applyButton);
        this.addDrawableChild(this.doneButton);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Element element : this.children()) {
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
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.updateStatus();

        this.renderBackground(matrices);
        this.currentList.render(matrices, mouseX, mouseY, delta);
//        fill(matrices, 0, 0, width, height, VUtil.packColor(0.6f,0.2f, 0.2f, 0.5f));

//        VideoOptionsScreen.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
        List<OrderedText> list = getHoveredButtonTooltip(this.currentList, mouseX, mouseY);
        if (list != null) {
            this.renderOrderedTooltip(matrices, list, mouseX, mouseY);
        }
    }

    private List<OrderedText> getHoveredButtonTooltip(OptionList2 buttonList, int mouseX, int mouseY) {
        Optional<OptionWidget> optional = buttonList.getHoveredButton(mouseX, mouseY);
        if (optional.isPresent()) {
            if(optional.get().getTooltip() == null) return null;
            return this.client.textRenderer.wrapLines(optional.get().getTooltip(), 200);
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

    public void setOptionList(ButtonWidget button, OptionList2 optionList) {
        this.currentList = optionList;

        this.buildPage();

        this.videoButton.setSelected(false);
        this.graphicsButton.setSelected(false);
        this.otherButton.setSelected(false);

        ((CustomButtonWidget)button).setSelected(true);
    }
}
