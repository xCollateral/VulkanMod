package net.vulkanmod.config;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Optional;

public class VideoSettingsScreen extends OptionsSubScreen {
    private static final Component FABULOUS = Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC);
    private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", FABULOUS, FABULOUS);
    private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
    private static final Component BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
    private static final Component BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
    private final OptionInstance<?>[] OPTIONS = options(this.options);
    private OptionsList list;
//    private final VideoWarningManager warningManager;
    private final int mipmapLevels;

    private static Component ModSceenTitle = Component.literal("VulkanMod Settings");

    private static OptionInstance<?>[] options(Options options) {
        return new OptionInstance[]{options.graphicsMode(), options.renderDistance(), options.prioritizeChunkUpdates(), options.simulationDistance(), options.ambientOcclusion(), options.framerateLimit(), options.enableVsync(), options.bobView(), options.guiScale(), options.attackIndicator(), options.gamma(), options.cloudStatus(), options.fullscreen(), options.particles(), options.mipmapLevels(), options.entityShadows(), options.screenEffectScale(), options.entityDistanceScaling(), options.fovEffectScale(), options.showAutosaveIndicator()};
    }

    public VideoSettingsScreen(Screen parent, Options options) {
        super(parent, options, Component.translatable("options.videoTitle"));
//        this.warningManager = this.minecraft.getVideoWarningManager();
//        this.warningManager.reset();
//        if (options.graphicsMode == GraphicsMode.FABULOUS) {
//            this.warningManager.acceptAfterWarnings();
//        }
        this.mipmapLevels = options.mipmapLevels().get();
    }

    @Override
    protected void init() {
        this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
//        this.list.addSingleOptionEntry(new FullscreenOption(this.minecraft.getWindow()));
//        this.list.addSingleOptionEntry(Option.BIOME_BLEND_RADIUS);
//        this.list.addAll(OPTIONS);

        OptionInstance<Integer> optionInstance = new OptionInstance<Integer>("options.fullscreen.resolution", OptionInstance.noTooltip(), (component, integer) -> {
//            if (monitor == null) {
//                return Component.translatable("options.fullscreen.unavailable");
//            }
//            if (integer == -1) {
//                return Options.genericValueLabel(component, Component.translatable("options.fullscreen.current"));
//            }
//            return Options.genericValueLabel(component, Component.literal(monitor.getMode((int)integer).toString()));
            return Options.genericValueLabel(component, Component.translatable("options.fullscreen.current"));
        }, new OptionInstance.IntRange(0, 1), 0, integer -> {
//            if (monitor == null) {
//                return;
//            }
//            window.setPreferredFullscreenVideoMode(integer == -1 ? Optional.empty() : Optional.of(monitor.getMode((int)integer)));
        });
        this.list.addSingleOptionEntry(optionInstance);
        this.list.addSingleOptionEntry(this.options.biomeBlendRadius());
        this.list.addAll(OPTIONS);

        this.list.addButton(new Button(this.width / 2 - 155, 0, 150, 20, ModSceenTitle, button -> {
            this.minecraft.options.save();
//            this.minecraft.getWindow().applyVideoMode();
            this.minecraft.setScreen(new OptionScreenV(ModSceenTitle, this));
        }));

        this.addRenderableWidget(this.list);
        this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, button -> {
            this.minecraft.options.save();
            this.minecraft.getWindow().changeFullscreenVideoMode();
            this.minecraft.setScreen(this.lastScreen);
        }));
    }

    @Override
    public void removed() {
        if (this.options.mipmapLevels().get() != this.mipmapLevels) {
            this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
            this.minecraft.delayTextureReload();
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button2) {
        int i = this.options.guiScale().get();
        if (super.mouseClicked(mouseX, mouseY, button2)) {
            if (this.options.guiScale().get() != i) {
                this.minecraft.resizeDisplay();
            }
//            if (this.warningManager.shouldWarn()) {
//                String string3;
//                String string2;
//                ArrayList<Text> list = Lists.newArrayList(GRAPHICS_WARNING_MESSAGE_TEXT, NEWLINE_TEXT);
//                String string = this.warningManager.getRendererWarning();
//                if (string != null) {
//                    list.add(NEWLINE_TEXT);
//                    list.add(Component.translatable("options.graphics.warning.renderer", string).formatted(Formatting.GRAY));
//                }
//                if ((string2 = this.warningManager.getVendorWarning()) != null) {
//                    list.add(NEWLINE_TEXT);
//                    list.add(Component.translatable("options.graphics.warning.vendor", string2).formatted(Formatting.GRAY));
//                }
//                if ((string3 = this.warningManager.getVersionWarning()) != null) {
//                    list.add(NEWLINE_TEXT);
//                    list.add(Component.translatable("options.graphics.warning.version", string3).formatted(Formatting.GRAY));
//                }
////                this.minecraft.setScreen(new DialogScreen(GRAPHICS_WARNING_TITLE_TEXT, list, ImmutableList.of(new DialogScreen.ChoiceButton(GRAPHICS_WARNING_ACCEPT_TEXT, button -> {
////                    this.gameOptions.graphicsMode = GraphicsMode.FABULOUS;
////                    MinecraftClient.getInstance().worldRenderer.reload();
////                    this.warningManager.acceptAfterWarnings();
////                    this.minecraft.setScreen(this);
////                }), new DialogScreen.ChoiceButton(GRAPHICS_WARNING_CANCEL_TEXT, button -> {
////                    this.warningManager.cancelAfterWarnings();
////                    this.minecraft.setScreen(this);
////                }))));
//            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int i = this.options.guiScale().get();
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (this.list.mouseReleased(mouseX, mouseY, button)) {
            if (this.options.guiScale().get() != i) {
                this.minecraft.resizeDisplay();
            }
            return true;
        }
        return false;
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.list.render(matrices, mouseX, mouseY, delta);
        Screen.drawCenteredString(matrices, this.font, this.title, this.width / 2, 5, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
        List<FormattedCharSequence> list = getHoveredButtonTooltip(this.list, mouseX, mouseY);
        if (list != null) {
            this.renderTooltip(matrices, list, mouseX, mouseY);
        }
    }

    private static List<FormattedCharSequence> getHoveredButtonTooltip(OptionsList buttonList, int mouseX, int mouseY) {
        Optional<AbstractWidget> optional = buttonList.getHoveredButton(mouseX, mouseY);
        if (optional.isPresent() && optional.get() instanceof TooltipAccessor) {
            return ((TooltipAccessor)((Object)optional.get())).getTooltip();
        }
        return ImmutableList.of();
    }
}
