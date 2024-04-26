package net.vulkanmod.config.video;

import net.vulkanmod.Initializer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public abstract class VideoModeManager {
    private static VideoModeSet.VideoMode osVideoMode;
    private static VideoModeSet[] videoModeSets;

    public static VideoModeSet.VideoMode selectedVideoMode;

    public static void init() {
        long monitor = glfwGetPrimaryMonitor();
        osVideoMode = getCurrentVideoMode(monitor);
        videoModeSets = populateVideoResolutions(GLFW.glfwGetPrimaryMonitor());
    }

    public static void applySelectedVideoMode() {
        Initializer.CONFIG.videoMode = selectedVideoMode;
    }

    public static VideoModeSet[] getVideoResolutions() {
        return videoModeSets;
    }

    public static VideoModeSet getFirstAvailable() {
        if(videoModeSets != null)
            return videoModeSets[videoModeSets.length - 1];
        else
            return VideoModeSet.getDummy();
    }

    public static VideoModeSet.VideoMode getOsVideoMode() {
        return osVideoMode;
    }

    public static VideoModeSet.VideoMode getCurrentVideoMode(long monitor){
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);

        if (vidMode == null)
            throw new NullPointerException("Unable to get current video mode");

        return new VideoModeSet.VideoMode(vidMode.width(), vidMode.height(), vidMode.redBits(), vidMode.refreshRate());
    }

    public static VideoModeSet[] populateVideoResolutions(long monitor) {
        GLFWVidMode.Buffer buffer = GLFW.glfwGetVideoModes(monitor);

        List<VideoModeSet> videoModeSets = new ArrayList<>();

        int currWidth = 0, currHeight = 0, currBitDepth = 0;
        VideoModeSet videoModeSet = null;

        for (int i = 0; i < buffer.limit(); i++) {
            buffer.position(i);
            int bitDepth = buffer.redBits();
            if (buffer.redBits() < 8 || buffer.greenBits() != bitDepth || buffer.blueBits() != bitDepth)
                continue;

            int width = buffer.width();
            int height = buffer.height();
            int refreshRate = buffer.refreshRate();

            if (currWidth != width || currHeight != height || currBitDepth != bitDepth) {
                currWidth = width;
                currHeight = height;
                currBitDepth = bitDepth;

                videoModeSet = new VideoModeSet(currWidth, currHeight, currBitDepth);
                videoModeSets.add(videoModeSet);
            }

            videoModeSet.addRefreshRate(refreshRate);
        }

        VideoModeSet[] arr = new VideoModeSet[videoModeSets.size()];
        videoModeSets.toArray(arr);

        return arr;
    }

    public static VideoModeSet getFromVideoMode(VideoModeSet.VideoMode videoMode) {
        for (var set : videoModeSets) {
            if (set.width == videoMode.width && set.height == videoMode.height)
                return set;
        }

        return null;
    }
}
