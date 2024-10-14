package net.vulkanmod.config.video;

import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class VideoModeManager {
    private static final Window window = Minecraft.getInstance().getWindow();
    private static final List<VideoMode> videoModes = populateVideoModes(GLFW.glfwGetPrimaryMonitor());
    private static final VideoMode osVideoMode = getCurrentVideoMode(GLFW.glfwGetPrimaryMonitor());

    public static VideoMode getSelectedVideoMode() {
        return window.getPreferredFullscreenVideoMode().orElse(getOsVideoMode());
    }

    public static void setSelectedVideoMode(VideoMode videoMode) {
        window.setPreferredFullscreenVideoMode(Optional.of(videoMode));
    }

    public static List<VideoMode> getVideoModes() {
        return videoModes;
    }

    public static VideoMode getFirstAvailable() {
        return videoModes.getLast();
    }

    public static VideoMode getOsVideoMode() {
        return osVideoMode;
    }

    public static @NotNull VideoMode getCurrentVideoMode(long monitor) {
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);

        if (vidMode == null)
            throw new NullPointerException("Unable to get current VideoMode");
        int retinaScale = Minecraft.ON_OSX ? 2 : 1;
        return new VideoMode(
                vidMode.width() * retinaScale,
                vidMode.height() * retinaScale,
                vidMode.redBits(),
                vidMode.greenBits(),
                vidMode.blueBits(),
                vidMode.refreshRate());
    }

    public static @NotNull List<VideoMode> populateVideoModes(long monitor) {
        GLFWVidMode.Buffer videoModes = GLFW.glfwGetVideoModes(monitor);
        if (videoModes == null) return Collections.emptyList();

        return videoModes.stream()
                .filter(mode -> {
                    int bitDepth = mode.redBits();
                    return bitDepth >= 8 && mode.greenBits() == bitDepth && mode.blueBits() == bitDepth;
                })
                .map(VideoMode::new)
                .toList();
    }
}
