package net.vulkanmod.config;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.chat.Component;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.vulkan.DeviceManager;
import net.vulkanmod.vulkan.Renderer;

import java.util.stream.IntStream;

public class Options {
    static net.minecraft.client.Options minecraftOptions = Minecraft.getInstance().options;
    static Config config = Initializer.CONFIG;
    static Window window = Minecraft.getInstance().getWindow();
    public static boolean fullscreenDirty = false;

    public static Option<?>[] getVideoOpts() {
        return new Option[] {
                new CyclingOption<>("Resolution",
                        VideoResolution.getVideoResolutions(),
                        resolution -> Component.literal(resolution.toString()),
                        (value) -> {
                            config.resolution = value;
                            fullscreenDirty = true;
                        },
                        () -> config.resolution)
                        .setTooltip(Component.literal("Only works on fullscreen")),
                new SwitchOption("Windowed Fullscreen",
                        value -> {
                            config.windowedFullscreen = value;
                            fullscreenDirty = true;
                        },
                        () -> config.windowedFullscreen)
                        .setTooltip(Component.nullToEmpty("Might not work properly")),
                new SwitchOption("Fullscreen",
                        value -> {
                            minecraftOptions.fullscreen().set(value);
//                            window.toggleFullScreen();
                            fullscreenDirty = true;
                        },
                        () -> minecraftOptions.fullscreen().get()),
                new RangeOption("Max Framerate", 10, 260, 10,
                        value -> value == 260 ? "Unlimited" : String.valueOf(value),
                        value -> {
                            minecraftOptions.framerateLimit().set(value);
                            window.setFramerateLimit(value);
                        },
                        () -> minecraftOptions.framerateLimit().get()),
                new SwitchOption("VSync",
                        value -> {
                            minecraftOptions.enableVsync().set(value);
                            Minecraft.getInstance().getWindow().updateVsync(value);
                        },
                        () -> minecraftOptions.enableVsync().get()),
                new CyclingOption<>("Gui Scale",
                        new Integer[]{0, 1, 2, 3, 4},
                        value -> value == 0 ? Component.literal("Auto") : Component.literal(value.toString()),
                        (value) -> {
                            minecraftOptions.guiScale().set(value);
                            Minecraft.getInstance().resizeDisplay();
                        },
                        () -> minecraftOptions.guiScale().get()),
                new RangeOption("Brightness", 0, 100, 1,
                        value -> {
                          if(value == 0) return Component.translatable("options.gamma.min").getString();
                          else if(value == 50) return Component.translatable("options.gamma.default").getString();
                          else if(value == 100) return Component.translatable("options.gamma.max").getString();
                          return value.toString();
                        },
                        value -> minecraftOptions.gamma().set(value * 0.01),
                        () -> (int) (minecraftOptions.gamma().get() * 100.0)),
                new CyclingOption<>("Smooth Lighting",
                        new Integer[]{LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK},
                        value -> switch (value) {
                            case LightMode.FLAT -> Component.literal("Off");
                            case LightMode.SMOOTH -> Component.literal("On");
                            case LightMode.SUB_BLOCK -> Component.literal("On (Sub-block)");
                            default -> Component.literal("Unk");
                        },
                        (value) -> {
                            if(value > LightMode.FLAT)
                                minecraftOptions.ambientOcclusion().set(true);
                            else
                                minecraftOptions.ambientOcclusion().set(false);

                            Initializer.CONFIG.ambientOcclusion = value;

                            Minecraft.getInstance().levelRenderer.allChanged();
                        },
                        () -> Initializer.CONFIG.ambientOcclusion)
                        .setTooltip(Component.nullToEmpty("""
                        On (Sub-block): Enables smooth lighting for non full block (experimental).""")),
                new SwitchOption("View Bobbing",
                        (value) -> minecraftOptions.bobView().set(value),
                        () -> minecraftOptions.bobView().get()),
                new CyclingOption<>("Attack Indicator",
                        AttackIndicatorStatus.values(),
                        value -> Component.translatable(value.getKey()),
                        value -> minecraftOptions.attackIndicator().set(value),
                        () -> minecraftOptions.attackIndicator().get()),
                new SwitchOption("Autosave Indicator",
                        value -> minecraftOptions.showAutosaveIndicator().set(value),
                        () -> minecraftOptions.showAutosaveIndicator().get()),
                new RangeOption("Distortion Effects", 0, 100, 1,
                        value -> minecraftOptions.screenEffectScale().set(value * 0.01),
                        () -> (int)(minecraftOptions.screenEffectScale().get() * 100.0f))
                        .setTooltip(Component.translatable("options.screenEffectScale.tooltip")),
                new RangeOption("FOV Effects", 0, 100, 1,
                        value -> minecraftOptions.fovEffectScale().set(value * 0.01),
                        () -> (int)(minecraftOptions.fovEffectScale().get() * 100.0f))
                        .setTooltip(Component.translatable("options.fovEffectScale.tooltip"))
        };
    }

    public static Option<?>[] getGraphicsOpts() {
        return new Option[] {
                new CyclingOption<>("Graphics",
                        new GraphicsStatus[]{GraphicsStatus.FAST, GraphicsStatus.FANCY},
                        graphicsMode -> Component.translatable(graphicsMode.getKey()),
                        value -> minecraftOptions.graphicsMode().set(value),
                        () -> minecraftOptions.graphicsMode().get()
                ),
                new CyclingOption<>("Particles",
                        new ParticleStatus[]{ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL},
                        particlesMode -> Component.translatable(particlesMode.getKey()),
                        value -> minecraftOptions.particles().set(value),
                        () -> minecraftOptions.particles().get()),
                new CyclingOption<>("Clouds",
                        CloudStatus.values(),
                        value -> Component.translatable(value.getKey()),
                        value -> minecraftOptions.cloudStatus().set(value),
                        () -> minecraftOptions.cloudStatus().get()),
                new SwitchOption("Unique opaque layer",
                        value -> {
                            config.uniqueOpaqueLayer = value;
                            Minecraft.getInstance().levelRenderer.allChanged();
                        },
                        () -> config.uniqueOpaqueLayer)
                        .setTooltip(Component.nullToEmpty("""
                        Improves performance by using a unique render layer for opaque terrain rendering.
                        It changes distant grass aspect and may cause unexpected texture behaviour""")),
                new RangeOption("Biome Blend Radius", 0, 7, 1,
                        value -> {
                    int v = value * 2 + 1;
                    return v + " x " + v;
                        },
                        (value) -> {
                            minecraftOptions.biomeBlendRadius().set(value);
                            Minecraft.getInstance().levelRenderer.allChanged();
                        },
                        () -> minecraftOptions.biomeBlendRadius().get()),
                new CyclingOption<>("Chunk Builder Mode",
                        PrioritizeChunkUpdates.values(),
                        value -> Component.translatable(value.getKey()),
                        value -> minecraftOptions.prioritizeChunkUpdates().set(value),
                        () -> minecraftOptions.prioritizeChunkUpdates().get()),
                new RangeOption("Render Distance", 2, 32, 1,
                        (value) -> {
                            minecraftOptions.renderDistance().set(value);
                        },
                        () -> minecraftOptions.renderDistance().get()),
                new RangeOption("Simulation Distance", 5, 32, 1,
                        (value) -> {
                            minecraftOptions.simulationDistance().set(value);
                        },
                        () -> minecraftOptions.simulationDistance().get()),
                new SwitchOption("Entity Shadows",
                        value -> minecraftOptions.entityShadows().set(value),
                        () -> minecraftOptions.entityShadows().get()),
                new RangeOption("Entity Distance", 50, 500, 25,
                        value -> minecraftOptions.entityDistanceScaling().set(value * 0.01),
                        () -> minecraftOptions.entityDistanceScaling().get().intValue() * 100),
                new CyclingOption<>("Mipmap Levels",
                        new Integer[]{0, 1, 2, 3, 4},
                        value -> Component.nullToEmpty(value.toString()),
                        value -> {
                            minecraftOptions.mipmapLevels().set(value);
                            Minecraft.getInstance().updateMaxMipLevel(value);
                            Minecraft.getInstance().delayTextureReload();
                        },
                        () -> minecraftOptions.mipmapLevels().get())
        };
    }

    public static Option<?>[] getOtherOpts() {
        return new Option[] {
                new RangeOption("Render queue size", 2,
                        5, 1,
                        value -> {
                            config.frameQueueSize = value;
                            Renderer.scheduleSwapChainUpdate();
                        }, () -> config.frameQueueSize)
                        .setTooltip(Component.nullToEmpty("""
                        Higher values might help stabilize frametime
                        but will increase input lag""")),
                new SwitchOption("Gui Optimizations",
                        value -> config.guiOptimizations = value,
                        () -> config.guiOptimizations)
                        .setTooltip(Component.nullToEmpty("""
                        Enable Gui optimizations (Stats bar, Chat, Debug Hud)
                        Might break mod compatibility
                        Restart is needed to take effect""")),
                new CyclingOption<>("Advanced Chunk Culling",
                        new Integer[]{1, 2, 3, 10},
                        value -> {
                            String t = switch (value) {
                                case 1 -> "Aggressive";
                                case 2 -> "Normal";
                                case 3 -> "Conservative";
                                case 10 -> "Off";
                                default -> "Unk";
                            };
                            return Component.nullToEmpty(t);
                        },
                        value -> config.advCulling = value,
                        () -> config.advCulling)
                        .setTooltip(Component.nullToEmpty("""
                        Use a culling algorithm that might improve performance by
                        reducing the number of non visible chunk sections rendered.
                        """)),
                new SwitchOption("Entity Culling",
                        value -> config.entityCulling = value,
                        () -> config.entityCulling)
                        .setTooltip(Component.nullToEmpty("""
                        Enables culling for entities on not visible sections.""")),
                new SwitchOption("Indirect Draw",
                        value -> config.indirectDraw = value,
                        () -> config.indirectDraw)
                        .setTooltip(Component.nullToEmpty("""
                        Reduces CPU overhead but increases GPU overhead.
                        Enabling it might help in CPU limited systems.""")),
                new SwitchOption("Low VRAM Mode",
                        value -> {
                            config.perRenderTypeAreaBuffers = value;
                            Minecraft.getInstance().levelRenderer.allChanged();
                        },
                        () -> config.perRenderTypeAreaBuffers).setTooltip(Component.nullToEmpty("""
                        Reduces VRAM usage by approx 20%
                        May Increase/Decrease FPS: Depends on GPU architecture
                        (Can boost performance on Old Nvidia cards)""")),
                new CyclingOption<>("Device selector",
                        IntStream.range(-1, DeviceManager.suitableDevices.size()).boxed().toArray(Integer[]::new),
                        value -> {
                            String t;

                            if(value == -1)
                                t = "Auto";
                            else
                                t = DeviceManager.suitableDevices.get(value).deviceName;

                            return Component.nullToEmpty(t);
                        },
                        value -> config.device = value,
                        () -> config.device)
                        .setTooltip(Component.nullToEmpty(
                                String.format("Current device: %s", DeviceManager.deviceInfo.deviceName)))
        };

    }

    public static void applyOptions(Config config, Option<?>[][] optionPages) {
        for(Option<?>[] options : optionPages) {
            for(Option<?> option : options) {
                option.apply();
            }
        }

        config.write();
    }
}
