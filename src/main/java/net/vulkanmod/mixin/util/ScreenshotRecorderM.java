package net.vulkanmod.mixin.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotRecorderM {

    @Shadow
    private static File getFile(File file) {
        return null;
    }

    @Shadow @Final private static Logger LOGGER;

    @Overwrite
    private static void _grab(File file, @Nullable String string, RenderTarget renderTarget, Consumer<Component> consumer) {


        Util.ioPool().execute(() -> {
            try (NativeImage nativeImage = takeScreenshot(renderTarget)) {
                File file2 = new File(file, "screenshots");
                file2.mkdir();

                File file3 = string == null ? getFile(file2) : new File(file2, string);
                nativeImage.writeToFile(file3);
                Component component = Component.literal(file3.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle((style) ->
                        style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file3.getAbsolutePath())));
                consumer.accept(Component.translatable("screenshot.success", component));
            } catch (Exception var7) {
                LOGGER.warn("Couldn't save screenshot", var7);
                consumer.accept(Component.translatable("screenshot.failure", var7.getMessage()));
            }

        });
    }

    /**
     * @author
     */
    @Overwrite
    public static NativeImage takeScreenshot(RenderTarget framebuffer) {

        final NativeImage nativeimage = new NativeImage(framebuffer.width, framebuffer.height, false);
        //RenderSystem.bindTexture(p_92282_.getColorTextureId());
        VulkanImage.downloadTextureAsync(framebuffer.width, framebuffer.height, nativeimage.pixels, Vulkan.getSwapChainImages().get(Drawer.getCurrentFrame()));
        //nativeimage.flipY();
        return nativeimage;
    }
}
