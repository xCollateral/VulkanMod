package net.vulkanmod.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DialogScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.FullscreenOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.Option;
import net.minecraft.client.resource.VideoWarningManager;
import net.minecraft.client.util.OrderableTooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VideoSettingsScreen extends GameOptionsScreen {
    private static final Text GRAPHICS_FABULOUS_TEXT = new TranslatableText("options.graphics.fabulous").formatted(Formatting.ITALIC);
    private static final Text GRAPHICS_WARNING_MESSAGE_TEXT = new TranslatableText("options.graphics.warning.message", GRAPHICS_FABULOUS_TEXT, GRAPHICS_FABULOUS_TEXT);
    private static final Text GRAPHICS_WARNING_TITLE_TEXT = new TranslatableText("options.graphics.warning.title").formatted(Formatting.RED);
    private static final Text GRAPHICS_WARNING_ACCEPT_TEXT = new TranslatableText("options.graphics.warning.accept");
    private static final Text GRAPHICS_WARNING_CANCEL_TEXT = new TranslatableText("options.graphics.warning.cancel");
    private static final Text NEWLINE_TEXT = new LiteralText("\n");
    private static final net.minecraft.client.option.Option[] OPTIONS = new net.minecraft.client.option.Option[]{net.minecraft.client.option.Option.GRAPHICS, net.minecraft.client.option.Option.RENDER_DISTANCE, net.minecraft.client.option.Option.CHUNK_BUILDER_MODE, net.minecraft.client.option.Option.SIMULATION_DISTANCE, net.minecraft.client.option.Option.AO, net.minecraft.client.option.Option.FRAMERATE_LIMIT, net.minecraft.client.option.Option.VSYNC, net.minecraft.client.option.Option.VIEW_BOBBING, net.minecraft.client.option.Option.GUI_SCALE, net.minecraft.client.option.Option.ATTACK_INDICATOR, net.minecraft.client.option.Option.GAMMA, net.minecraft.client.option.Option.CLOUDS, net.minecraft.client.option.Option.FULLSCREEN, net.minecraft.client.option.Option.PARTICLES, net.minecraft.client.option.Option.MIPMAP_LEVELS, net.minecraft.client.option.Option.ENTITY_SHADOWS, net.minecraft.client.option.Option.DISTORTION_EFFECT_SCALE, net.minecraft.client.option.Option.ENTITY_DISTANCE_SCALING, net.minecraft.client.option.Option.FOV_EFFECT_SCALE, Option.SHOW_AUTOSAVE_INDICATOR};
    private OptionsList list;
//    private final VideoWarningManager warningManager;
    private final int mipmapLevels;

    private static Text ModSceenTitle = Text.of("VulkanMod Settings");

    public VideoSettingsScreen(Screen parent, GameOptions options) {
        super(parent, options, new TranslatableText("options.videoTitle"));
//        this.warningManager = this.client.getVideoWarningManager();
//        this.warningManager.reset();
//        if (options.graphicsMode == GraphicsMode.FABULOUS) {
//            this.warningManager.acceptAfterWarnings();
//        }
        this.mipmapLevels = options.mipmapLevels;
    }

    @Override
    protected void init() {
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
    public void removed() {
        if (this.gameOptions.mipmapLevels != this.mipmapLevels) {
            this.client.setMipmapLevels(this.gameOptions.mipmapLevels);
            this.client.reloadResourcesConcurrently();
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button2) {
        int i = this.gameOptions.guiScale;
        if (super.mouseClicked(mouseX, mouseY, button2)) {
            if (this.gameOptions.guiScale != i) {
                this.client.onResolutionChanged();
            }
//            if (this.warningManager.shouldWarn()) {
//                String string3;
//                String string2;
//                ArrayList<Text> list = Lists.newArrayList(GRAPHICS_WARNING_MESSAGE_TEXT, NEWLINE_TEXT);
//                String string = this.warningManager.getRendererWarning();
//                if (string != null) {
//                    list.add(NEWLINE_TEXT);
//                    list.add(new TranslatableText("options.graphics.warning.renderer", string).formatted(Formatting.GRAY));
//                }
//                if ((string2 = this.warningManager.getVendorWarning()) != null) {
//                    list.add(NEWLINE_TEXT);
//                    list.add(new TranslatableText("options.graphics.warning.vendor", string2).formatted(Formatting.GRAY));
//                }
//                if ((string3 = this.warningManager.getVersionWarning()) != null) {
//                    list.add(NEWLINE_TEXT);
//                    list.add(new TranslatableText("options.graphics.warning.version", string3).formatted(Formatting.GRAY));
//                }
////                this.client.setScreen(new DialogScreen(GRAPHICS_WARNING_TITLE_TEXT, list, ImmutableList.of(new DialogScreen.ChoiceButton(GRAPHICS_WARNING_ACCEPT_TEXT, button -> {
////                    this.gameOptions.graphicsMode = GraphicsMode.FABULOUS;
////                    MinecraftClient.getInstance().worldRenderer.reload();
////                    this.warningManager.acceptAfterWarnings();
////                    this.client.setScreen(this);
////                }), new DialogScreen.ChoiceButton(GRAPHICS_WARNING_CANCEL_TEXT, button -> {
////                    this.warningManager.cancelAfterWarnings();
////                    this.client.setScreen(this);
////                }))));
//            }
            return true;
        }
        return false;
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

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.list.render(matrices, mouseX, mouseY, delta);
        VideoOptionsScreen.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
        List<OrderedText> list = getHoveredButtonTooltip(this.list, mouseX, mouseY);
        if (list != null) {
            this.renderOrderedTooltip(matrices, list, mouseX, mouseY);
        }
    }

    private static List<OrderedText> getHoveredButtonTooltip(OptionsList buttonList, int mouseX, int mouseY) {
        Optional<ClickableWidget> optional = buttonList.getHoveredButton(mouseX, mouseY);
        if (optional.isPresent() && optional.get() instanceof OrderableTooltip) {
            return ((OrderableTooltip)((Object)optional.get())).getOrderedTooltip();
        }
        return ImmutableList.of();
    }
}
