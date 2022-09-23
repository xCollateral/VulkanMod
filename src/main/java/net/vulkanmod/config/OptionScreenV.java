package net.vulkanmod.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.vulkanmod.Initializer;

public class OptionScreenV extends Screen {
    private OptionsList list;
    private Option[] options;
    private Screen parent;

    private ButtonWidget doneButton;
    private ButtonWidget applyButton;

    public OptionScreenV(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.options = Options.getOptions(Initializer.CONFIG);

        this.list = new OptionsList(this.client, this.width, this.height, 32, this.height - 32, 25);
//        this.list.addSingleOptionEntry(new FullscreenOption(this.client.getWindow()));
//        this.list.addSingleOptionEntry(Option.BIOME_BLEND_RADIUS);
//        this.list.addAll(OPTIONS);
        this.list.addAll(this.options);

        this.addSelectableChild(this.list);

        this.addDrawableChild(this.applyButton = new ButtonWidget(this.width / 2 + 90, this.height - 27, 50, 20, Text.of("Apply"), button -> {
            Options.applyOptions(Initializer.CONFIG, this.options);
        }));
        this.addDrawableChild(this.doneButton = new ButtonWidget(this.width / 2 + 150, this.height - 27, 50, 20, ScreenTexts.DONE, button -> {
            this.client.setScreen(this.parent);
        }));

        this.applyButton.active = false;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.updateStatus();

        this.renderBackground(matrices);
        this.list.render(matrices, mouseX, mouseY, delta);
        VideoOptionsScreen.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
//        List<OrderedText> list = VideoOptionsScreen.getHoveredButtonTooltip(this.list, mouseX, mouseY);
//        if (list != null) {
//            this.renderOrderedTooltip(matrices, list, mouseX, mouseY);
//        }
    }

    public void updateStatus() {

        boolean modified = false;
        for(Option<?> option: this.options) {
            if(option.isModified()) modified = true;
        }

        this.applyButton.active = modified;
    }
    
}
