package net.vulkanmod.mixin.window;

import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Options;
import net.vulkanmod.config.VideoResolution;
import net.vulkanmod.vulkan.Renderer;
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

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Final @Shadow private long window;

    @Shadow private boolean vsync;

    @Shadow protected abstract void updateFullscreen(boolean bl);

    @Shadow private boolean fullscreen;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;

    @Shadow @Final private WindowEventHandler eventHandler;

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getHeight();

    @Shadow private int framebufferWidth;

    @Shadow private int framebufferHeight;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void redirect(int hint, int value) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
    private void redirect2(long window) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"))
    private GLCapabilities redirect2() {
        return null;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private void vulkanHint(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        //Fix Gnome Client-Side Decorators
        GLFW.glfwWindowHint(GLFW_DECORATED, VideoResolution.isGNOME() && VideoResolution.isWayLand() ? GLFW_FALSE : GLFW_TRUE);
//        GLFW.glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
//        GLFW.glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void getHandle(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        VRenderSystem.setWindow(this.window);
    }

    /**
     * @author
     */
    @Overwrite
    public void updateVsync(boolean vsync) {
        this.vsync = vsync;
        Vulkan.setVsync(vsync);
    }

    /**
     * @author
     */
    @Overwrite
    public void toggleFullScreen() {
        this.fullscreen = !this.fullscreen;
        Options.fullscreenDirty = true;
    }

    /**
     * @author
     */
    @Overwrite
    public void updateDisplay() {
        RenderSystem.flipFrame(this.window);
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
    private void setMode() {
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

        long monitor =  GLFW.glfwGetWindowMonitor(this.window);
        monitor = GLFW.glfwGetPrimaryMonitor();

        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
        if(this.fullscreen) {
            {
                VideoMode videoMode = config.resolution.getVideoMode();
                if(videoMode == null) {
                    LOGGER.error("Not supported resolution, fallback to first supported");
                    videoMode = VideoResolution.getVideoResolutions()[0].getVideoMode();
                }
//                if (Minecraft.ON_OSX) {
//                    MacosUtil.toggleFullscreen(this.window);
//                }
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
                GLFW.glfwSetWindowMonitor(this.window, monitor, this.x, this.y, this.width, this.height, videoMode.getRefreshRate());
            }
        }
        else if(config.windowedFullscreen) {

            this.x = 0;
            this.y = 0;
            assert vidMode != null;
            this.width = vidMode.width();
            this.height = vidMode.height();
            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;
            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_TRUE);
            GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void onFramebufferResize(long window, int width, int height) {
        if (window == this.window) {
            int prevWidth = this.getWidth();
            int prevHeight = this.getHeight();

            if(width > 0 && height > 0) {
                this.framebufferWidth = width;
                this.framebufferHeight = height;
//                if (this.framebufferWidth != prevWidth || this.framebufferHeight != prevHeight) {
//                    this.eventHandler.resizeDisplay();
//                }

                Renderer.scheduleSwapChainUpdate();
            }

        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void onResize(long window, int width, int height) {
//        System.out.printf("onResize: %d %d%n", width, height);
        this.width = width;
        this.height = height;

        if(width > 0 && height > 0)
            Renderer.scheduleSwapChainUpdate();
    }

}
