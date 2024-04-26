package net.vulkanmod.config;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

import static net.vulkanmod.Initializer.LOGGER;
import static org.lwjgl.glfw.GLFW.*;

public abstract class Platform {
    private static final int activePlat = getSupportedPlat();
    private static final String activeDE = determineDE();

    public static void init() {
        GLFW.glfwInitHint(GLFW_PLATFORM, activePlat);
        LOGGER.info("Selecting Platform: {}", getStringFromPlat(activePlat));
        LOGGER.info("GLFW: {}", GLFW.glfwGetVersionString());
        GLFW.glfwInit();
    }

    //Actually detect the currently active Display Server (if both Wayland and X11 are present on the system and/or GLFW is compiled to support both)
    private static int determineDisplayServer() {

        //Return Null platform if not on Linux (i.e. no X11 or Wayland)
        String xdgSessionType = System.getenv("XDG_SESSION_TYPE");
        if (xdgSessionType == null) return GLFW_ANY_PLATFORM; //Likely Android
        return switch (xdgSessionType) {
            case "wayland" -> GLFW_PLATFORM_WAYLAND; //Wayland
            case "x11" -> GLFW_PLATFORM_X11; //X11
            default -> GLFW_ANY_PLATFORM; //Either unknown Platform or Display Server
        };
    }

    private static int getSupportedPlat() {
        //Switch statement would be ideal, but couldn't find a good way of implementing it, so fell back to basic if statements/branches
        if (SystemUtils.IS_OS_WINDOWS) return GLFW_PLATFORM_WIN32;
        if (SystemUtils.IS_OS_MAC_OSX) return GLFW_PLATFORM_COCOA;
        if (SystemUtils.IS_OS_LINUX) return determineDisplayServer(); //Linux Or Android

        return GLFW_ANY_PLATFORM; //Unknown platform
    }

    private static String getStringFromPlat(int plat) {
        return switch (plat) {
            case GLFW_PLATFORM_WIN32 -> "WIN32";
            case GLFW_PLATFORM_WAYLAND -> "WAYLAND";
            case GLFW_PLATFORM_X11 -> "X11";
            case GLFW_PLATFORM_COCOA -> "MACOS";
            case GLFW_ANY_PLATFORM -> "ANDROID";
            default -> throw new IllegalStateException("Unexpected value: " + plat);
        };
    }

    private static String determineDE() {
        String xdgSessionDesktop = System.getenv("XDG_SESSION_DESKTOP");
        String xdgCurrentDesktop = System.getenv("XDG_CURRENT_DESKTOP");
        if (xdgSessionDesktop != null)
            return xdgSessionDesktop.toLowerCase();
        if (xdgCurrentDesktop != null)
            return xdgCurrentDesktop.toLowerCase();
        return "N/A";
    }

    public static int getActivePlat() {
        return activePlat;
    }

    //Allows platform specific checks to be handled
    public static boolean isWayLand() {
        return activePlat == GLFW_PLATFORM_WAYLAND;
    }

    public static boolean isX11() {
        return activePlat == GLFW_PLATFORM_X11;
    }

    public static boolean isWindows() {
        return activePlat == GLFW_PLATFORM_WIN32;
    }

    public static boolean isMacOS() {
        return activePlat == GLFW_PLATFORM_COCOA;
    }

    public static boolean isAndroid() {
        return activePlat == GLFW_ANY_PLATFORM;
    }

    //Desktop Environment Names: https://wiki.archlinux.org/title/Xdg-utils#Usage
    public static boolean isGnome() {
        return activeDE.contains("gnome");
    }

    public static boolean isWeston() {
        return activeDE.contains("weston");
    }

    public static boolean isGeneric() {
        return activeDE.contains("generic");
    }
}
