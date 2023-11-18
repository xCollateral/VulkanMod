package net.vulkanmod.config;

import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.vulkanmod.Initializer.LOGGER;
import static org.lwjgl.glfw.GLFW.*;

public class VideoResolution {
    private static VideoResolution[] videoResolutions;
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

    public static void init() {
        RenderSystem.assertOnRenderThread();
        GLFW.glfwInitHint(GLFW_PLATFORM, activePlat);
        LOGGER.info("Selecting Platform: "+getStringFromPlat(activePlat));
        LOGGER.info("GLFW: "+GLFW.glfwGetVersionString());
        GLFW.glfwInit();
        videoResolutions = populateVideoResolutions(GLFW.glfwGetPrimaryMonitor());
    }

    //Actually detect the currently active Display Server (if both Wayland and X11 are present on the system and/or GLFW is compiled to support both)
    private static int determineDisplayServer() {

        //Return Null platform if not on Linux (i.e. no X11 or Wayland)
        String xdgSessionType = System.getenv("XDG_SESSION_TYPE");
        if(xdgSessionType==null) return GLFW_ANY_PLATFORM; //Likely Android
        return switch (xdgSessionType) {
            case "wayland" -> GLFW_PLATFORM_WAYLAND; //Wayland
            case "x11" -> GLFW_PLATFORM_X11; //X11
            default -> GLFW_ANY_PLATFORM; //Either unknown Platform or Display Server
        };
    }

    private static int getSupportedPlat() {
        //Switch statement would be ideal, but couldn't find a good way of implementing it, so fell back to basic if statements/branches
        if(SystemUtils.IS_OS_WINDOWS) return GLFW_PLATFORM_WIN32;
        if(SystemUtils.IS_OS_MAC_OSX) return GLFW_PLATFORM_COCOA;
        if(SystemUtils.IS_OS_LINUX) return determineDisplayServer(); //Linux Or Android

        return GLFW_ANY_PLATFORM; //Unknown platform
    }

    private static String getStringFromPlat(int plat) {
        return switch (plat)
        {
            case GLFW_PLATFORM_WIN32 -> "WIN32";
            case GLFW_PLATFORM_WAYLAND -> "WAYLAND";
            case GLFW_PLATFORM_X11 -> "X11";
            case GLFW_PLATFORM_COCOA -> "MACOS";
            case GLFW_ANY_PLATFORM -> "ANDROID";
            default -> throw new IllegalStateException("Unexpected value: " + plat);
        };
    }

    public static int getActivePlat() { return activePlat; }

    //Allows platform specific checks to be handled
    public static boolean isWayLand() { return activePlat == GLFW_PLATFORM_WAYLAND; }
    public static boolean isX11() { return activePlat == GLFW_PLATFORM_X11; }
    public static boolean isWindows() { return activePlat == GLFW_PLATFORM_WIN32; }
    public static boolean isMacOS() { return activePlat == GLFW_PLATFORM_COCOA; }
    public static boolean isAndroid() { return activePlat == GLFW_ANY_PLATFORM; }

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
