package net.vulkanmod.config;

import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.vulkanmod.Initializer.LOGGER;
import static org.lwjgl.glfw.GLFW.*;

public class VideoResolution {
    private static VideoResolution[] videoResolutions;

    //Make sure Wayland is ALWAYS preferred in 100% of cases (and only fallback to X11 if Wayland isn't available)
    private static final int[] plats = new int[]{
            GLFW_PLATFORM_WIN32,
            GLFW_PLATFORM_WAYLAND,
            GLFW_PLATFORM_X11};

    private static final int activePlat = getSupportedPlat();

    int width;
    int height;
    int refreshRate;

    private List<VideoMode> videoModes;

    public VideoResolution(int width, int height) {
        this.width = width;
        this.height = height;
        this.videoModes = new ArrayList<>(6);
    }

    public void addVideoMode(VideoMode videoMode) {
        videoModes.add(videoMode);
    }

    public String toString() {
        return this.width + " x " + this.height;
    }

    public VideoMode getVideoMode() {
        VideoMode videoMode;
        for(VideoResolution resolution : videoResolutions) {
            if(this.width == resolution.width && this.height == resolution.height) return resolution.videoModes.get(0);
        }
        return null;
    }

    public int[] refreshRates() {
        int[] arr = new int[videoModes.size()];

        for(int i = 0; i < arr.length; ++i) {
            arr[i] = videoModes.get(i).getRefreshRate();
        }

        return arr;
    }
    //Prioritise Wayland over X11 if xWayland (if correct) is present

    public static void init() {
        RenderSystem.assertOnRenderThread();
        GLFW.glfwInitHint(GLFW_PLATFORM, activePlat);
        GLFW.glfwInit();
        videoResolutions = populateVideoResolutions(GLFW.glfwGetPrimaryMonitor());
    }

    private static int getSupportedPlat() {

        for (int plat : plats) {
            if(GLFW.glfwPlatformSupported(plat))
            {
                LOGGER.info("Selecting Platform: "+getStringFromPlat(plat));
                return plat;
            }
        }
        throw new RuntimeException("No Supported Platforms Present!");
    }

    private static String getStringFromPlat(int plat) {
        return switch (plat)
        {
            case GLFW_PLATFORM_WIN32 -> "WIN32";
            case GLFW_PLATFORM_WAYLAND -> "WAYLAND";
            case GLFW_PLATFORM_X11 -> "X11";
            default -> throw new IllegalStateException("Unexpected value: " + plat);
        };
    }

    public static int getActivePlat() { return activePlat; }
    //Allows platform specific checks to be handles (is missing macOS, however that may not be important due to macOS only version))
    public static boolean isWayLand() { return activePlat == GLFW_PLATFORM_WAYLAND; }
    public static boolean isX11() { return activePlat == GLFW_PLATFORM_X11; }
    public static boolean isWindows() { return activePlat == GLFW_PLATFORM_WIN32; }

    public static VideoResolution[] getVideoResolutions() {
        return videoResolutions;
    }

    public static VideoResolution getFirstAvailable() {
        if(videoResolutions != null) return videoResolutions[0];
        else return new VideoResolution(-1, -1);
    }

    public static VideoResolution[] populateVideoResolutions(long monitor) {
        GLFWVidMode.Buffer buffer = GLFW.glfwGetVideoModes(monitor);
//        VideoMode[] videoModes = new VideoMode[buffer.limit()];
//        for (int i = buffer.limit() - 1; i >= 0; --i) {
//            buffer.position(i);
//            VideoMode videoMode = new VideoMode(buffer);
//            if (videoMode.getRedBits() < 8 || videoMode.getGreenBits() < 8 || videoMode.getBlueBits() < 8) continue;
//            videoModes[i] = (videoMode);
//        }

        List<VideoResolution> videoResolutions = new ArrayList<>();
        for (int i = buffer.limit() - 1; i >= 0; --i) {
            buffer.position(i);
            VideoMode videoMode = new VideoMode(buffer);
            if (buffer.redBits() < 8 || buffer.greenBits() < 8 || buffer.blueBits() < 8) continue;

            int width = buffer.width();
            int height = buffer.height();

            Optional<VideoResolution> resolution = videoResolutions.stream()
                    .filter(videoResolution -> videoResolution.width == width && videoResolution.height == height)
                    .findAny();

            if(resolution.isEmpty()) {
                VideoResolution newResoultion = new VideoResolution(width, height);
                videoResolutions.add(newResoultion);
                resolution = Optional.of(newResoultion);
            }

            resolution.get().addVideoMode(videoMode);

        }

        VideoResolution[] arr = new VideoResolution[videoResolutions.size()];
        videoResolutions.toArray(arr);

        return arr;
    }

}
