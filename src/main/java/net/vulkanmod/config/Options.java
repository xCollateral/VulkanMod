package net.vulkanmod.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import net.minecraft.client.render.ChunkBuilderMode;
import net.minecraft.client.util.Window;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Drawer;

public class Options {
    static GameOptions minecraftOptions = MinecraftClient.getInstance().options;
    static Config config = Initializer.CONFIG;
    static Window window = MinecraftClient.getInstance().getWindow();
    public static boolean fullscreenDirty = false;


    public static Option<?>[] getVideoOpts() {
        return new Option[] {
                new CyclingOption<>("Resolution",
                        VideoResolution.getVideoResolutions(),
                        resolution -> new LiteralText(resolution.toString()),
                        (value) -> {
                            config.resolution = value;
                            fullscreenDirty = true;
                        },
                        () -> config.resolution)
                        .setTooltip(Text.of("Only works on fullscreen")),
                new SwitchOption("Windowed Fullscreen",
                        value -> {
                            config.windowedFullscreen = value;
                            fullscreenDirty = true;
                        },
                        () -> config.windowedFullscreen)
                        .setTooltip(Text.of("Might not work properly")),
                new SwitchOption("Fullscreen",
                        value -> {
                            minecraftOptions.fullscreen = value;
                            window.toggleFullscreen();
                            fullscreenDirty = true;
                        },
                        () -> minecraftOptions.fullscreen),
                new RangeOption("Max Framerate", 10, 260, 10,
                        value -> value == 260 ? "Unlimited" : String.valueOf(value),
                        value -> {
                            minecraftOptions.maxFps = value;
                            window.setFramerateLimit(value);
                        },
                        () -> minecraftOptions.maxFps),
                new SwitchOption("VSync",
                        value -> {
                            minecraftOptions.enableVsync = value;
                            if (MinecraftClient.getInstance().getWindow() != null) {
                                MinecraftClient.getInstance().getWindow().setVsync(value);
                            }
                        },
                        () -> minecraftOptions.enableVsync),
                new CyclingOption<>("Gui Scale",
                        new Integer[]{0, 1, 2, 3, 4},
                        value -> value == 0 ? new LiteralText("Auto") : new LiteralText(value.toString()),
                        (value) -> {
                            minecraftOptions.guiScale = value;
                            MinecraftClient.getInstance().onResolutionChanged();
                        },
                        () -> minecraftOptions.guiScale),
                new RangeOption("Brightness", 0, 100, 1,
                        value -> {
                          if(value == 0) return new TranslatableText("options.gamma.min").getString();
                          else if(value == 50) return new TranslatableText("options.gamma.default").getString();
                          else if(value == 100) return new TranslatableText("options.gamma.max").getString();
                          return value.toString();
                        },
                        value -> minecraftOptions.gamma = value * 0.01,
                        () -> (int) (minecraftOptions.gamma * 100.0)),
                new CyclingOption<>("Smooth Lighting",
                        AoMode.values(),
                        value -> new TranslatableText(value.getTranslationKey()),
                        value -> minecraftOptions.ao = value,
                        () -> minecraftOptions.ao),
                new SwitchOption("View Bobbing",
                        (value) -> minecraftOptions.bobView = value,
                        () -> minecraftOptions.bobView),
                new CyclingOption<>("Attack Indicator",
                        AttackIndicator.values(),
                        value -> new TranslatableText(value.getTranslationKey()),
                        value -> minecraftOptions.attackIndicator = value,
                        () -> minecraftOptions.attackIndicator),
                new SwitchOption("Autosave Indicator",
                        value -> minecraftOptions.showAutosaveIndicator = value,
                        () -> minecraftOptions.showAutosaveIndicator),
                new RangeOption("Distortion Effects", 0, 100, 1,
                        value -> minecraftOptions.distortionEffectScale = value * 0.01f,
                        () -> (int)(minecraftOptions.distortionEffectScale * 100.0f))
                        .setTooltip(new TranslatableText("options.screenEffectScale.tooltip")),
                new RangeOption("FOV Effects", 0, 100, 1,
                        value -> minecraftOptions.fovEffectScale = value * 0.01f,
                        () -> (int)(minecraftOptions.fovEffectScale * 100.0f))
                        .setTooltip(new TranslatableText("options.fovEffectScale.tooltip"))
        };
    }

    public static Option<?>[] getGraphicsOpts() {
        return new Option[] {
                new CyclingOption<>("Graphics",
                        new GraphicsMode[]{GraphicsMode.FAST, GraphicsMode.FANCY},
                        graphicsMode -> new TranslatableText(graphicsMode.getTranslationKey()),
                        value -> minecraftOptions.graphicsMode = value,
                        () -> minecraftOptions.graphicsMode
                ),
                new RangeOption("Biome Blend Radius", 0, 7, 1,
                        value -> {
                    int v = value * 2 + 1;
                    return v + " x " + v;
                        },
                        (value) -> {
                            minecraftOptions.biomeBlendRadius = value;
                            MinecraftClient.getInstance().worldRenderer.reload();
                        },
                        () -> minecraftOptions.biomeBlendRadius),
                new CyclingOption<>("Chunk Builder Mode",
                        ChunkBuilderMode.values(),
                        value -> new TranslatableText(value.getName()),
                        value -> minecraftOptions.chunkBuilderMode = value,
                        () -> minecraftOptions.chunkBuilderMode),
                new RangeOption("Render Distance", 2, 32, 1,
                        (value) -> {
                            minecraftOptions.viewDistance = value;
                            MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate();
                        },
                        () -> minecraftOptions.viewDistance),
                new RangeOption("Simulation Distance", 2, 32, 1,
                        (value) -> {
                            minecraftOptions.simulationDistance = value;
                        },
                        () -> minecraftOptions.simulationDistance),
                new CyclingOption<>("Clouds",
                        CloudRenderMode.values(),
                        value -> new TranslatableText(value.getTranslationKey()),
                        value -> minecraftOptions.cloudRenderMode = value,
                        () -> minecraftOptions.cloudRenderMode),
                new SwitchOption("Entity Shadows",
                        value -> minecraftOptions.entityShadows = value,
                        () -> minecraftOptions.entityShadows),
                new RangeOption("Entity Distance", 50, 500, 25,
                        value -> minecraftOptions.entityDistanceScaling = value * 0.01f,
                        () -> (int)minecraftOptions.entityDistanceScaling * 100),
                new CyclingOption<>("Particles",
                        new ParticlesMode[]{ParticlesMode.MINIMAL, ParticlesMode.DECREASED, ParticlesMode.ALL},
                        particlesMode -> new TranslatableText(particlesMode.getTranslationKey()),
                        value -> minecraftOptions.particles = value,
                        () -> minecraftOptions.particles),
                new CyclingOption<>("Mipmap Levels",
                        new Integer[]{0, 1, 2, 3, 4},
                        value -> Text.of(value.toString()),
                        value -> minecraftOptions.mipmapLevels = value,
                        () -> minecraftOptions.mipmapLevels)
        };
    }

    public static Option<?>[] getOtherOpts() {
        return new Option[] {
                new RangeOption("RenderFrameQueue", 2,
                        5, 1,
                        value -> {
                            Drawer.rebuild = true;
                            config.frameQueueSize = value;
                        }, () -> config.frameQueueSize)
                        .setTooltip(Text.of("Restart is needed to take effect")),
                new SwitchOption("Gui Optimizations",
                        value -> config.guiOptimizations = value,
                        () -> config.guiOptimizations)
                        .setTooltip(Text.of("""
                        Enable Gui optimizations (Stats bar, Chat, Debug Hud)
                        Might break mod compatibility
                        Restart is needed to take effect"""))
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
