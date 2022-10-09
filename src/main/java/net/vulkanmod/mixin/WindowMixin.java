package net.vulkanmod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.*;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Options;
import net.vulkanmod.config.VideoResolution;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Final @Shadow private long handle;
    @Shadow private boolean vsync;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private boolean fullscreen;
    @Shadow @Final private MonitorTracker monitorTracker;
    @Shadow private Optional<VideoMode> videoMode;
    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private boolean currentFullscreen;

    @Shadow protected abstract void updateFullscreen(boolean vsync);

    @Shadow public abstract int getRefreshRate();

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void redirect(int hint, int value) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
    private void redirect2(long window) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"))
    private GLCapabilities redirect2() {
        return null;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private void vulkanHint(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void getHandle(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
        VRenderSystem.setWindow(this.handle);
    }

    /**
     * @author
     */
    @Overwrite
    public void setVsync(boolean vsync) {
        this.vsync = vsync;
        Vulkan.setVsync(vsync);
    }

    /**
     * @author
     */
    @Overwrite
    public void toggleFullscreen() {
        this.fullscreen = !this.fullscreen;
        Options.fullscreenDirty = true;
    }

    /**
     * @author
     */
    @Overwrite
    public void swapBuffers() {
        RenderSystem.flipFrame(this.handle);
//        if (this.fullscreen != this.currentFullscreen) {
//            this.currentFullscreen = this.fullscreen;
//            this.updateFullscreen(this.vsync);
//        }
        if (Options.fullscreenDirty) {
            Options.fullscreenDirty = false;
            this.updateFullscreen(this.vsync);
        }
    }

    /**
     * @author
     */
    @Overwrite
    private void updateWindowRegion() {
//        boolean bl;
//        RenderSystem.assertInInitPhase();
//        boolean bl2 = bl = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
//
//        if (this.fullscreen) {
//            Monitor monitor = this.monitorTracker.getMonitor(this);
//            if (monitor == null) {
//                LOGGER.warn("Failed to find suitable monitor for fullscreen mode");
//                this.fullscreen = false;
//            } else {
//                if (MinecraftClient.IS_SYSTEM_MAC) {
//                    MacWindowUtil.toggleFullscreen(this.handle);
//                }
//                VideoMode videoMode = monitor.findClosestVideoMode(this.videoMode);
//                if (!bl) {
//                    this.windowedX = this.x;
//                    this.windowedY = this.y;
//                    this.windowedWidth = this.width;
//                    this.windowedHeight = this.height;
//                }
//                this.x = 0;
//                this.y = 0;
//                this.width = videoMode.getWidth();
//                this.height = videoMode.getHeight();
//                GLFW.glfwSetWindowMonitor(this.handle, monitor.getHandle(), this.x, this.y, this.width, this.height, videoMode.getRefreshRate());
//            }
//        } else {
//            this.x = this.windowedX;
//            this.y = this.windowedY;
//            this.width = this.windowedWidth;
//            this.height = this.windowedHeight;
//            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
//        }

        Config config = Initializer.CONFIG;

        long monitor =  GLFW.glfwGetWindowMonitor(this.handle);
        monitor = GLFW.glfwGetPrimaryMonitor();

        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
        if(this.fullscreen) {
            {
                VideoMode videoMode = config.resolution.getVideoMode();
                if(videoMode == null) {
                    LOGGER.error("Not supported resolution, fallback to first supported");
                    videoMode = VideoResolution.getVideoResolutions()[0].getVideoMode();
                }
                if (MinecraftClient.IS_SYSTEM_MAC) {
                    MacWindowUtil.toggleFullscreen(this.handle);
                }
//                VideoMode videoMode = monitor.findClosestVideoMode(this.videoMode);
//                if (!bl) {
//                    this.windowedX = this.x;
//                    this.windowedY = this.y;
//                    this.windowedWidth = this.width;
//                    this.windowedHeight = this.height;
//                }
                this.windowedX = this.x;
                this.windowedY = this.y;
                this.windowedWidth = this.width;
                this.windowedHeight = this.height;

                this.x = 0;
                this.y = 0;
                this.width = videoMode.getWidth();
                this.height = videoMode.getHeight();
                GLFW.glfwSetWindowMonitor(this.handle, monitor, this.x, this.y, this.width, this.height, videoMode.getRefreshRate());
            }
        }
        else if(config.windowedFullscreen) {

            this.x = 0;
            this.y = 0;
            assert vidMode != null;
            this.width = vidMode.width();
            this.height = vidMode.height();
            GLFW.glfwSetWindowAttrib(this.handle, GLFW_DECORATED, GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;
            GLFW.glfwSetWindowAttrib(this.handle, GLFW_DECORATED, GLFW_TRUE);
            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
        }
    }
}
