package net.vulkanmod.config.option;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.gui.OptionBlock;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;

import java.util.stream.IntStream;

public abstract class Options {
    private static net.minecraft.client.Options minecraftOptions = Minecraft.getInstance().options;
    static Config config = Initializer.CONFIG;
    private static final Window window = Minecraft.getInstance().getWindow();
    public static boolean fullscreenDirty = false;

    public static OptionBlock[] getVideoOpts() {
        var videoMode = config.videoMode;
        var videoModeSet = VideoModeManager.getFromVideoMode(videoMode);

        if (videoModeSet == null) {
            videoModeSet = VideoModeSet.getDummy();
            videoMode = videoModeSet.getVideoMode(-1);
        }

        VideoModeManager.selectedVideoMode = videoMode;
        var refreshRates = videoModeSet.getRefreshRates();

        CyclingOption<Integer> RefreshRate = (CyclingOption<Integer>) new CyclingOption<>(
                Component.translatable("Refresh Rate"),
                refreshRates.toArray(new Integer[0]),
                (value) -> {
                    VideoModeManager.selectedVideoMode.refreshRate = value;
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> VideoModeManager.selectedVideoMode.refreshRate)
                .setTranslator(refreshRate -> Component.nullToEmpty(refreshRate.toString()));

        Option<VideoModeSet> resolutionOption = new CyclingOption<>(
                Component.translatable("Resolution"),
                VideoModeManager.getVideoResolutions(),
                (value) -> {
                    VideoModeManager.selectedVideoMode = value.getVideoMode(RefreshRate.getNewValue());
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> {
                    var videoMode1 = VideoModeManager.selectedVideoMode;
                    var videoModeSet1 = VideoModeManager.getFromVideoMode(videoMode1);

                    if (videoModeSet1 == null) {
                        videoModeSet1 = VideoModeSet.getDummy();
                    }

                    return videoModeSet1;
                })
                .setTranslator(resolution -> Component.nullToEmpty(resolution.toString()));

        resolutionOption.setOnChange(() -> {
            var videoMode1 = resolutionOption.getNewValue();
            var refreshRates1 = videoMode1.getRefreshRates();
            RefreshRate.setValues(refreshRates1.toArray(new Integer[0]));
            RefreshRate.setNewValue(refreshRates1.get(refreshRates1.size() - 1));
        });

        return new OptionBlock[] {
                new OptionBlock("", new Option<?>[]{
                        resolutionOption,
                        RefreshRate,
                        new SwitchOption(Component.translatable("vulkanmod.options.windowedFullscreen"),
                                value -> {
                                    config.windowedFullscreen = value;
                                    fullscreenDirty = true;
                                },
                                () -> config.windowedFullscreen),
                        new SwitchOption(Component.translatable("Fullscreen"),
                                value -> {
                                    minecraftOptions.fullscreen().set(value);
//                            window.toggleFullScreen();
                                    fullscreenDirty = true;
                                },
                                () -> minecraftOptions.fullscreen().get()),
                        new RangeOption(Component.translatable("Max Framerate"),
                                10, 260, 10,
                                value -> Component.nullToEmpty(value == 260 ? "Unlimited" : String.valueOf(value)),
                                value -> {
                                    minecraftOptions.framerateLimit().set(value);
                                    window.setFramerateLimit(value);
                                },
                                () -> minecraftOptions.framerateLimit().get()),
                        new SwitchOption(Component.translatable("VSync"),
                                value -> {
                                    minecraftOptions.enableVsync().set(value);
                                    Minecraft.getInstance().getWindow().updateVsync(value);
                                },
                                () -> minecraftOptions.enableVsync().get()),
                }),
                new OptionBlock("", new Option<?>[]{
                        new CyclingOption<>(Component.translatable("Gui Scale"),
                                getGuiScaleValues(),
                                (value) -> {
                                    minecraftOptions.guiScale().set(value);
                                    Minecraft.getInstance().resizeDisplay();
                                },
                                () -> minecraftOptions.guiScale().get())
                                .setTranslator(value -> value == 0 ? Component.literal("Auto") : Component.literal(value.toString())),
                        new RangeOption(Component.translatable("Brightness"),
                                0, 100, 1,
                                value -> switch (value) {
                                    case 0 -> Component.translatable("options.gamma.min");
                                    case 50 -> Component.translatable("options.gamma.default");
                                    case 100 -> Component.translatable("options.gamma.max");
                                    default -> Component.literal(String.valueOf(value));
                                },
                                value -> minecraftOptions.gamma().set(value * 0.01),
                                () -> (int) (minecraftOptions.gamma().get() * 100.0)),
                }),
                new OptionBlock("", new Option<?>[]{
                        new SwitchOption(Component.translatable("View Bobbing"),
                                (value) -> minecraftOptions.bobView().set(value),
                                () -> minecraftOptions.bobView().get()),
                        new CyclingOption<>(Component.translatable("Attack Indicator"),
                                AttackIndicatorStatus.values(),
                                value -> minecraftOptions.attackIndicator().set(value),
                                () -> minecraftOptions.attackIndicator().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                        new SwitchOption(Component.translatable("Autosave Indicator"),
                                value -> minecraftOptions.showAutosaveIndicator().set(value),
                                () -> minecraftOptions.showAutosaveIndicator().get()),
                })
        };
    }

    public static OptionBlock[] getGraphicsOpts() {
        return new OptionBlock[] {
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(Component.translatable("Render Distance"),
                                2, 32, 1,
                                (value) -> {
                                    minecraftOptions.renderDistance().set(value);
                                },
                                () -> minecraftOptions.renderDistance().get()),
                        new RangeOption(Component.translatable("Simulation Distance"),
                                5, 32, 1,
                                (value) -> {
                                    minecraftOptions.simulationDistance().set(value);
                                },
                                () -> minecraftOptions.simulationDistance().get()),
                        new CyclingOption<>(Component.translatable("Chunk Builder Mode"),

                                PrioritizeChunkUpdates.values(),
                                value -> minecraftOptions.prioritizeChunkUpdates().set(value),
                                () -> minecraftOptions.prioritizeChunkUpdates().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                }),
                new OptionBlock("", new Option<?>[]{
                        new CyclingOption<>(Component.translatable("Graphics"),
                                new GraphicsStatus[]{GraphicsStatus.FAST, GraphicsStatus.FANCY},
                                value -> minecraftOptions.graphicsMode().set(value),
                                () -> minecraftOptions.graphicsMode().get())
                                .setTranslator(graphicsMode -> Component.translatable(graphicsMode.getKey())),
                        new CyclingOption<>(Component.translatable("Particles"),
                                new ParticleStatus[]{ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL},
                                value -> minecraftOptions.particles().set(value),
                                () -> minecraftOptions.particles().get())
                                .setTranslator(particlesMode -> Component.translatable(particlesMode.getKey())),
                        new CyclingOption<>(Component.translatable("Clouds"),
                                CloudStatus.values(),
                                value -> minecraftOptions.cloudStatus().set(value),
                                () -> minecraftOptions.cloudStatus().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                        new CyclingOption<>(Component.translatable("Smooth Lighting"),
                                new Integer[]{LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK},
                                (value) -> {
                                    if (value > LightMode.FLAT)
                                        minecraftOptions.ambientOcclusion().set(true);
                                    else
                                        minecraftOptions.ambientOcclusion().set(false);

                                    Initializer.CONFIG.ambientOcclusion = value;

                                    Minecraft.getInstance().levelRenderer.allChanged();
                                },
                                () -> Initializer.CONFIG.ambientOcclusion)
                                .setTranslator(value -> switch (value) {
                                    case LightMode.FLAT -> Component.literal("Off");
                                    case LightMode.SMOOTH -> Component.literal("On");
                                    case LightMode.SUB_BLOCK -> Component.literal("On (Sub-block)");
                                    default -> Component.literal("Unk");
                                })
                                .setTooltip(Component.nullToEmpty("""
                                On (Sub-block): Enables smooth lighting for non full block (experimental).""")),
                        new SwitchOption(Component.translatable("Unique opaque layer"),
                                value -> {
                                    config.uniqueOpaqueLayer = value;
                                    Minecraft.getInstance().levelRenderer.allChanged();
                                },
                                () -> config.uniqueOpaqueLayer)
                                .setTooltip(Component.translatable("vulkanmod.options.uniqueOpaqueLayer.tooltip")),
                        new RangeOption(Component.translatable("Biome Blend Radius"),
                                0, 7, 1,
                                value -> {
                                    int v = value * 2 + 1;
                                    return Component.nullToEmpty("%d x %d".formatted(v, v));
                                },
                                (value) -> {
                                    minecraftOptions.biomeBlendRadius().set(value);
                                    Minecraft.getInstance().levelRenderer.allChanged();
                                },
                                () -> minecraftOptions.biomeBlendRadius().get()),
                        new SwitchOption(Component.translatable("Animations"),
                                (value) -> config.animations = value,
                                () -> config.animations),
                        new SwitchOption(Component.translatable("RenderSky"),
                                (value) -> config.renderSky = value,
                                () -> config.renderSky)
                }),
                new OptionBlock("", new Option<?>[]{
                        new SwitchOption(Component.translatable("Entity Shadows"),
                                value -> minecraftOptions.entityShadows().set(value),
                                () -> minecraftOptions.entityShadows().get()),
                        new RangeOption(Component.translatable("Entity Distance"),
                                50, 500, 25,
                                value -> minecraftOptions.entityDistanceScaling().set(value * 0.01),
                                () -> minecraftOptions.entityDistanceScaling().get().intValue() * 100),
                        new CyclingOption<>(Component.translatable("Mipmap Levels"),
                                new Integer[]{0, 1, 2, 3, 4},
                                value -> {
                                    minecraftOptions.mipmapLevels().set(value);
                                    Minecraft.getInstance().updateMaxMipLevel(value);
                                    Minecraft.getInstance().delayTextureReload();
                                    Renderer.getDescriptorSetArray().setSampler(value);
                                    Renderer.getDescriptorSetArray().forceDescriptorUpdate();
                                },
                                () -> minecraftOptions.mipmapLevels().get())
                                .setTranslator(value -> Component.nullToEmpty(value.toString()))
                })
        };
    }

    public static OptionBlock[] getOptimizationOpts() {
        return new OptionBlock[] {
                new OptionBlock("", new Option[] {
                        new CyclingOption<>(Component.translatable("Advanced Chunk Culling"),
                                new Integer[]{1, 2, 3, 10},
                                value -> config.advCulling = value,
                                () -> config.advCulling)
                                .setTranslator(value -> {
                                    String t = switch (value) {
                                        case 1 -> "Aggressive";
                                        case 2 -> "Normal";
                                        case 3 -> "Conservative";
                                        case 10 -> "Off";
                                        default -> "Unk";
                                    };
                                    return Component.nullToEmpty(t);
                                })
                                .setTooltip(Component.translatable("vulkanmod.options.advCulling.tooltip")),
                        new SwitchOption(Component.translatable("Entity Culling"),
                                value -> config.entityCulling = value,
                                () -> config.entityCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.entityCulling.tooltip")),
                        new SwitchOption(Component.translatable("Indirect Draw"),
                                value -> config.indirectDraw = value,
                                () -> config.indirectDraw)
                                .setTooltip(Component.translatable("vulkanmod.options.indirectDraw.tooltip")),
                })
        };

    }

    public static OptionBlock[] getOtherOpts() {
        return new OptionBlock[] {
                new OptionBlock("", new Option[] {
                        new RangeOption(Component.translatable("Render queue size"),
                                2, 5, 1,
                                value -> {
                                    config.frameQueueSize = value;
                                    Renderer.scheduleSwapChainUpdate();
                                }, () -> config.frameQueueSize)
                                .setTooltip(Component.translatable("vulkanmod.options.frameQueue.tooltip")),
                        new CyclingOption<>(Component.translatable("Device selector"),
                                IntStream.range(-1, DeviceManager.suitableDevices.size()).boxed().toArray(Integer[]::new),
                                value -> config.device = value,
                                () -> config.device)
                                .setTranslator(value -> {
                                    String t;

                                    if (value == -1)
                                        t = "Auto";
                                    else
                                        t = DeviceManager.suitableDevices.get(value).deviceName;

                                    return Component.nullToEmpty(t);
                                })
                                .setTooltip(Component.nullToEmpty(
                                String.format("Current device: %s", DeviceManager.device.deviceName)))
                })
        };

    }
    public static byte getMipmaps()
    {
        return (byte) (int) minecraftOptions.mipmapLevels().get();
    }
    static Integer[] getGuiScaleValues() {
        int max = window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode());

        Integer[] values = new Integer[max];

        for (int i = 0; i < max; i++) {
            values[i] = i;
        }

        return values;
    }
}
