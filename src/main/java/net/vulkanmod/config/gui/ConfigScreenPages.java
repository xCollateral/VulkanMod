package net.vulkanmod.config.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.gui.option.Option;
import net.vulkanmod.config.gui.option.OptionGroup;
import net.vulkanmod.config.gui.option.OptionPage;
import net.vulkanmod.config.gui.option.control.CyclingController;
import net.vulkanmod.config.gui.option.control.SliderController;
import net.vulkanmod.config.gui.option.control.SwitchController;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;

import java.util.List;
import java.util.stream.IntStream;

public class ConfigScreenPages {
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Window window = minecraft.getWindow();
    private static final Config config = Initializer.CONFIG;
    private static final Options minecraftOptions = minecraft.options;
    public static boolean fullscreenDirty = false;

    public static OptionPage video() {
        return OptionPage.createBuilder()
                .name(Component.translatable("vulkanmod.options.pages.video"))
                .group(OptionGroup.createBuilder()
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.fullscreen.resolution"))
                                .binding(() -> {
                                            int index;
                                            if (VideoModeManager.getSelectedVideoMode().equals(VideoModeManager.getOsVideoMode()))
                                                index = VideoModeManager.getVideoModes().size();
                                            else
                                                index = VideoModeManager.getVideoModes().indexOf(VideoModeManager.getSelectedVideoMode());

                                            return index != -1 ? index : VideoModeManager.getVideoModes().size() - 1;
                                        },
                                        value -> {
                                            if (value == VideoModeManager.getVideoModes().size())
                                                VideoModeManager.setSelectedVideoMode(VideoModeManager.getOsVideoMode());
                                            else
                                                VideoModeManager.setSelectedVideoMode(VideoModeManager.getVideoModes().get(value));

                                            if (minecraftOptions.fullscreen().get())
                                                fullscreenDirty = true;
                                        })
                                .controller(opt -> new SliderController(opt, 0, VideoModeManager.getVideoModes().size(), 1))
                                .active(VideoModeManager.getVideoModes().size() != 1)
                                .translator(value -> {
                                    VideoMode v = null;
                                    if (value != VideoModeManager.getVideoModes().size())
                                        v = VideoModeManager.getVideoModes().get(value);
                                    return Component.translatable(value == VideoModeManager.getVideoModes().size()
                                            ? "options.fullscreen.current"
                                            : "%sx%s@%s".formatted(v.getWidth(), v.getHeight(), v.getRefreshRate()));
                                })
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("options.fullscreen"))
                                .binding(() -> minecraft.options.fullscreen().get(),
                                        value -> {
                                            minecraft.options.fullscreen().set(value);
                                            fullscreenDirty = true;
                                        })
                                .controller(SwitchController::new)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("vulkanmod.options.windowedFullscreen"))
                                .binding(() -> config.windowedFullscreen,
                                        value -> {
                                            config.windowedFullscreen = value;
                                            fullscreenDirty = true;
                                        })
                                .controller(SwitchController::new)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.framerateLimit"))
                                .binding(() -> minecraftOptions.framerateLimit().get(),
                                        value -> {
                                            minecraftOptions.framerateLimit().set(value);
                                            window.setFramerateLimit(value);
                                        })
                                .controller(opt -> new SliderController(opt, 10, 260, 10))
                                .translator(value -> Component.nullToEmpty(value == 260
                                        ? Component.translatable("options.framerateLimit.max").getString()
                                        : String.valueOf(value)))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("options.vsync"))
                                .binding(() -> minecraftOptions.enableVsync().get(),
                                        value -> {
                                            minecraftOptions.enableVsync().set(value);
                                            window.updateVsync(value);
                                        })
                                .controller(SwitchController::new)
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.guiScale"))
                                .binding(() -> minecraftOptions.guiScale().get(),
                                        value -> {
                                            minecraftOptions.guiScale().set(value);
                                            minecraft.resizeDisplay();
                                        })
                                .controller(opt -> new SliderController(opt, 0, window.calculateScale(0, minecraft.isEnforceUnicode()), 1))
                                .translator(value -> Component.translatable((value == 0)
                                        ? "options.guiScale.auto"
                                        : String.valueOf(value)))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.gamma"))
                                .binding(() -> (int) (minecraftOptions.gamma().get() * 100),
                                        value -> minecraftOptions.gamma().set(value * 0.01))
                                .controller(opt -> new SliderController(opt, 0, 100, 1))
                                .translator(value -> Component.translatable(switch (value) {
                                    case 0 -> "options.gamma.min";
                                    case 50 -> "options.gamma.default";
                                    case 100 -> "options.gamma.max";
                                    default -> String.valueOf(value);
                                }))
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("options.viewBobbing"))
                                .binding(() -> minecraftOptions.bobView().get(),
                                        value -> minecraftOptions.bobView().set(value))
                                .controller(SwitchController::new)
                                .build())
                        .option(Option.<AttackIndicatorStatus>createBuilder()
                                .name(Component.translatable("options.attackIndicator"))
                                .binding(() -> minecraftOptions.attackIndicator().get(),
                                        value -> minecraftOptions.attackIndicator().set(value))
                                .controller(opt -> new CyclingController<>(opt, List.of(AttackIndicatorStatus.values())))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("options.autosaveIndicator"))
                                .binding(() -> minecraftOptions.showAutosaveIndicator().get(),
                                        value -> minecraftOptions.showAutosaveIndicator().set(value))
                                .controller(SwitchController::new)
                                .build())
                        .build())
                .build();
    }

    public static OptionPage graphics() {
        return OptionPage.createBuilder()
                .name(Component.translatable("vulkanmod.options.pages.graphics"))
                .group(OptionGroup.createBuilder()
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.renderDistance"))
                                .binding(() -> minecraftOptions.renderDistance().get(),
                                        value -> minecraftOptions.renderDistance().set(value))
                                .controller(opt -> new SliderController(opt, 2, 32, 1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.simulationDistance"))
                                .binding(() -> minecraftOptions.simulationDistance().get(),
                                        value -> minecraftOptions.simulationDistance().set(value))
                                .controller(opt -> new SliderController(opt, 5, 32, 1))
                                .build())
                        .option(Option.<PrioritizeChunkUpdates>createBuilder()
                                .name(Component.translatable("options.prioritizeChunkUpdates"))
                                .binding(() -> minecraftOptions.prioritizeChunkUpdates().get(),
                                        value -> minecraftOptions.prioritizeChunkUpdates().set(value))
                                .controller(opt -> new CyclingController<>(opt, List.of(PrioritizeChunkUpdates.values())))
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .option(Option.<GraphicsStatus>createBuilder()
                                .name(Component.translatable("options.graphics"))
                                .binding(() -> minecraftOptions.graphicsMode().get(),
                                        value -> minecraftOptions.graphicsMode().set(value))
                                .controller(opt -> new CyclingController<>(opt, List.of(GraphicsStatus.FAST, GraphicsStatus.FANCY)))
                                .build())
                        .option(Option.<ParticleStatus>createBuilder()
                                .name(Component.translatable("options.particles"))
                                .binding(() -> minecraftOptions.particles().get(),
                                        value -> minecraftOptions.particles().set(value))
                                .controller(opt -> new CyclingController<>(opt, Lists.reverse(List.of(ParticleStatus.values()))))
                                .build())
                        .option(Option.<CloudStatus>createBuilder()
                                .name(Component.translatable("options.renderClouds"))
                                .binding(() -> minecraftOptions.cloudStatus().get(),
                                        value -> minecraftOptions.cloudStatus().set(value))
                                .controller(opt -> new CyclingController<>(opt, List.of(CloudStatus.values())))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.ao"))
                                .tooltip(Component.translatable("vulkanmod.options.ao.subBlock.tooltip"))
                                .binding(() -> config.ambientOcclusion,
                                        value -> {
                                            if (value > LightMode.FLAT)
                                                minecraftOptions.ambientOcclusion().set(true);
                                            else
                                                minecraftOptions.ambientOcclusion().set(false);

                                            config.ambientOcclusion = value;

                                            minecraft.levelRenderer.allChanged();
                                        })
                                .controller(opt -> new CyclingController<>(opt, List.of(LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK)))
                                .translator(value -> Component.translatable(switch (value) {
                                    case LightMode.FLAT -> "options.off";
                                    case LightMode.SMOOTH -> "options.on";
                                    case LightMode.SUB_BLOCK -> "vulkanmod.options.ao.subBlock";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.biomeBlendRadius"))
                                .binding(() -> minecraftOptions.biomeBlendRadius().get(),
                                        value -> {
                                            minecraftOptions.biomeBlendRadius().set(value);
                                            minecraft.levelRenderer.allChanged();
                                        })
                                .controller(opt -> new SliderController(opt, 0, 7, 1))
                                .translator(value -> {
                                    int v = value * 2 + 1;
                                    return Component.literal("%d x %d".formatted(v, v));
                                })
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("options.entityShadows"))
                                .binding(() -> minecraftOptions.entityShadows().get(),
                                        value -> minecraftOptions.entityShadows().set(value))
                                .controller(SwitchController::new)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.entityDistanceScaling"))
                                .binding(() -> (int) (minecraftOptions.entityDistanceScaling().get() * 100),
                                        value -> minecraftOptions.entityDistanceScaling().set(value / 100.0))
                                .controller(opt -> new SliderController(opt, 50, 500, 25))
                                .translator(value -> Component.literal("%s%%".formatted(value)))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("options.mipmapLevels"))
                                .binding(() -> minecraftOptions.mipmapLevels().get(),
                                        value -> {
                                            minecraftOptions.mipmapLevels().set(value);
                                            minecraft.updateMaxMipLevel(value);
                                            minecraft.delayTextureReload();
                                        })
                                .controller(opt -> new SliderController(opt, 0, 4, 1))
                                .translator(value -> Component.translatable(value == 0
                                        ? "options.off"
                                        : String.valueOf(value)))
                                .build())
                        .build())
                .build();
    }

    public static OptionPage optimizations() {
        return OptionPage.createBuilder()
                .name(Component.translatable("vulkanmod.options.pages.optimizations"))
                .group(OptionGroup.createBuilder()
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("vulkanmod.options.advCulling"))
                                .tooltip(Component.translatable("vulkanmod.options.advCulling.tooltip"))
                                .binding(() -> config.advCulling,
                                        value -> config.advCulling = value)
                                .controller(opt -> new CyclingController<>(opt, List.of(1, 2, 3, 10)))
                                .translator(value -> Component.translatable(switch (value) {
                                    case 1 -> "vulkanmod.options.advCulling.aggressive";
                                    case 2 -> "vulkanmod.options.advCulling.normal";
                                    case 3 -> "vulkanmod.options.advCulling.conservative";
                                    case 10 -> "options.off";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("vulkanmod.options.entityCulling"))
                                .tooltip(Component.translatable("vulkanmod.options.entityCulling.tooltip"))
                                .binding(() -> config.entityCulling,
                                        value -> config.entityCulling = value)
                                .controller(SwitchController::new)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("vulkanmod.options.uniqueOpaqueLayer"))
                                .tooltip(Component.translatable("vulkanmod.options.uniqueOpaqueLayer.tooltip"))
                                .binding(() -> config.uniqueOpaqueLayer,
                                        value -> {
                                            config.uniqueOpaqueLayer = value;
                                            TerrainRenderType.updateMapping();
                                            minecraft.levelRenderer.allChanged();
                                        })
                                .controller(SwitchController::new)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("vulkanmod.options.backfaceCulling"))
                                .binding(() -> config.backFaceCulling,
                                        value -> {
                                            config.backFaceCulling = value;
                                            Minecraft.getInstance().levelRenderer.allChanged();
                                        })
                                .controller(SwitchController::new)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("vulkanmod.options.indirectDraw"))
                                .tooltip(Component.translatable("vulkanmod.options.indirectDraw.tooltip"))
                                .binding(() -> config.indirectDraw,
                                        value -> config.indirectDraw = value)
                                .controller(SwitchController::new)
                                .build())
                        .build())
                .build();
    }

    public static OptionPage other() {
        return OptionPage.createBuilder()
                .name(Component.translatable("vulkanmod.options.pages.other"))
                .group(OptionGroup.createBuilder()
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("vulkanmod.options.frameQueue"))
                                .tooltip(Component.translatable("vulkanmod.options.frameQueue.tooltip"))
                                .binding(() -> config.frameQueueSize,
                                        value -> {
                                            config.frameQueueSize = value;
                                            Renderer.scheduleSwapChainUpdate();
                                        })
                                .controller(opt -> new SliderController(opt, 2, 5, 1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("vulkanmod.options.deviceSelector"))
                                .tooltip(Component.nullToEmpty("%s: %s".formatted(
                                        Component.translatable("vulkanmod.options.deviceSelector.tooltip").getString(),
                                        DeviceManager.device.deviceName)))
                                .binding(() -> config.device,
                                        value -> config.device = value)
                                .controller(opt -> new CyclingController<>(opt, IntStream.range(-1, DeviceManager.suitableDevices.size()).boxed().toList()))
                                .translator(value -> Component.translatable((value == -1)
                                        ? "options.guiScale.auto"
                                        : DeviceManager.suitableDevices.get(value).deviceName))
                                .build())
                        .build())
                .build();
    }
}
