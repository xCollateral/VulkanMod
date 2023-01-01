package net.vulkanmod.config;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Drawer;

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
                            if (Minecraft.getInstance().getWindow() != null) {
                                Minecraft.getInstance().getWindow().updateVsync(value);
                            }
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
                        AmbientOcclusionStatus.values(),
                        value -> Component.translatable(value.getKey()),
                        value -> minecraftOptions.ambientOcclusion().set(value),
                        () -> minecraftOptions.ambientOcclusion().get()),
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
                            Minecraft.getInstance().levelRenderer.needsUpdate();
                        },
                        () -> minecraftOptions.renderDistance().get()),
                new RangeOption("Simulation Distance", 2, 32, 1,
                        (value) -> {
                            minecraftOptions.simulationDistance().set(value);
                        },
                        () -> minecraftOptions.simulationDistance().get()),
                new CyclingOption<>("Clouds",
                        CloudStatus.values(),
                        value -> Component.translatable(value.getKey()),
                        value -> minecraftOptions.cloudStatus().set(value),
                        () -> minecraftOptions.cloudStatus().get()),
                new SwitchOption("Entity Shadows",
                        value -> minecraftOptions.entityShadows().set(value),
                        () -> minecraftOptions.entityShadows().get()),
                new RangeOption("Entity Distance", 50, 500, 25,
                        value -> minecraftOptions.entityDistanceScaling().set(value * 0.01),
                        () -> minecraftOptions.entityDistanceScaling().get().intValue() * 100),
                new CyclingOption<>("Particles",
                        new ParticleStatus[]{ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL},
                        particlesMode -> Component.translatable(particlesMode.getKey()),
                        value -> minecraftOptions.particles().set(value),
                        () -> minecraftOptions.particles().get()),
                new CyclingOption<>("Mipmap Levels",
                        new Integer[]{0, 1, 2, 3, 4},
                        value -> Component.nullToEmpty(value.toString()),
                        value -> minecraftOptions.mipmapLevels().set(value),
                        () -> minecraftOptions.mipmapLevels().get())
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
                        .setTooltip(Component.nullToEmpty("Restart is needed to take effect")),
                new SwitchOption("Gui Optimizations",
                        value -> config.guiOptimizations = value,
                        () -> config.guiOptimizations)
                        .setTooltip(Component.nullToEmpty("""
                        Enable Gui optimizations (Stats bar, Chat, Debug Hud)
                        Might break mod compatibility
                        Restart is needed to take effect""")),
                new CyclingOption<>("Advanced Chunk Culling",
                        new Integer[]{1, 2, 10},
                        value -> {
                            String t = switch (value) {
                                case 1 -> "Aggressive";
                                case 2 -> "Conservative";
                                case 10 -> "Off";
                                default -> "Unk";
                            };
                            return Component.nullToEmpty(t);
                        },
                        value -> config.advCulling = value,
                        () -> config.advCulling),
                new RangeOption("baseAlignSize", 16384,
                        131072, 4096,
                        value -> {

                            Config.baseAlignSize = value;
                            WorldRenderer.allChanged(0);
                        }, () -> Config.baseAlignSize)
                        .setTooltip(Component.nullToEmpty("Adjust Base Modifier Size for Chunk/VBO Allocations")),
                new RangeOption("vboAlignment", 0,
                        16, 1,
                        value -> {
                            /*Config.prevValue = Config.prevValue < Config.vboAlignment ? value >> 1 : value << 1;*/
                            Config.vboAlignment=value;
                            Config.vboAlignmentActual=1<<value;

                            WorldRenderer.allChanged(0);
                        }, () -> Config.vboAlignment)
                        .setTooltip(Component.nullToEmpty("Current Value: "+ (1<<Config.vboAlignment)+" \n (Refreshes if Exiting/Reentering options Screen)"+"\n\n"+"""
                        Use to Adjust VBO Alignment
                        Can only be Adjusted in Powers of 2
                        Value is Equal to 2^Value (e.g. 2^16=65536)
                        Larger values increase memory usage but may reduce Fragmentation when suballocating VBOs
                        """)),
                new SwitchOption("noFog",
                        value -> {

                            Config.noFog=value;

                        }, () -> Config.noFog)
                        .setTooltip(Component.nullToEmpty("""
                        Disables all forms of Fog when enabled
                        Uses an alternate shader with all fog effects removed
                        May increase performance slightly""")),
                new SwitchOption("multiDrawIndirect",
                        value -> {

                            Config.drawIndirect=value;

                        }, () -> Config.drawIndirect)
                        .setTooltip(Component.nullToEmpty("""
                        Enable GPUMultiDrawIndirect
                        May improve or worsen performance: Depends on hardware""")),
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
