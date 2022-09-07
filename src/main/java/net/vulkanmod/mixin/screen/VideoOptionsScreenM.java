package net.vulkanmod.mixin.screen;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.FullscreenOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Option;
import net.minecraft.client.util.OrderableTooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.vulkanmod.config.OptionScreenV;
import net.vulkanmod.config.OptionsList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Optional;

@Mixin(VideoOptionsScreen.class)
public class VideoOptionsScreenM extends GameOptionsScreen {

    private OptionsList list;

    @Shadow @Final private static Option[] OPTIONS;

    private static Text ModSceenTitle = Text.of("VulkanMod Settings");

    public VideoOptionsScreenM(Screen parent, GameOptions options) {
        super(parent, options, new TranslatableText("options.videoTitle"));
//        this.warningManager = parent.client.getVideoWarningManager();
//        this.warningManager.reset();
//        if (options.graphicsMode == GraphicsMode.FABULOUS) {
//            this.warningManager.acceptAfterWarnings();
//        }
//        this.mipmapLevels = options.mipmapLevels;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void init() {
        this.list = new OptionsList(this.client, this.width, this.height, 32, this.height - 32, 25);
        this.list.addSingleOptionEntry(new FullscreenOption(this.client.getWindow()));
        this.list.addSingleOptionEntry(Option.BIOME_BLEND_RADIUS);
        this.list.addAll(OPTIONS);

        this.list.addButton(new ButtonWidget(this.width / 2 - 155, 0, 150, 20, ModSceenTitle, button -> {
            this.client.options.write();
//            this.client.getWindow().applyVideoMode();
            this.client.setScreen(new OptionScreenV(ModSceenTitle, this));
        }));

        this.addSelectableChild(this.list);
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height - 27, 200, 20, ScreenTexts.DONE, button -> {
            this.client.options.write();
            this.client.getWindow().applyVideoMode();
            this.client.setScreen(this.parent);
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.list.render(matrices, mouseX, mouseY, delta);
        VideoOptionsScreen.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
        List<OrderedText> list = VideoOptionsScreenM.getHoveredButtonTooltip(this.list, mouseX, mouseY);
        if (list != null) {
            this.renderOrderedTooltip(matrices, list, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int i = this.gameOptions.guiScale;
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (this.list.mouseReleased(mouseX, mouseY, button)) {
            if (this.gameOptions.guiScale != i) {
                this.client.onResolutionChanged();
            }
            return true;
        }
        return false;
    }

    private static List<OrderedText> getHoveredButtonTooltip(OptionsList buttonList, int mouseX, int mouseY) {
        Optional<ClickableWidget> optional = buttonList.getHoveredButton(mouseX, mouseY);
        if (optional.isPresent() && optional.get() instanceof OrderableTooltip) {
            return ((OrderableTooltip)((Object)optional.get())).getOrderedTooltip();
        }
        return ImmutableList.of();
    }
}
