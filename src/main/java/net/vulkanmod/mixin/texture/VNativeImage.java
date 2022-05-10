package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.NativeImage;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(NativeImage.class)
public abstract class VNativeImage {

    @Shadow private long pointer;
    @Shadow private long sizeBytes;

    @Shadow public abstract void close();


    @Shadow @Final private NativeImage.Format format;

    @Shadow public abstract int getWidth();
    @Shadow public abstract int getColor(int x, int y);
    @Shadow public abstract void setColor(int x, int y, int color);

    @Shadow @Final private int width;
    @Shadow @Final private int height;

    @Shadow public abstract int getHeight();

    private ByteBuffer buffer;

    @Inject(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V", at = @At("RETURN"))
    private void constr(NativeImage.Format format, int width, int height, boolean useStb, CallbackInfo ci) {
        if(this.pointer != 0) {
            buffer = MemoryUtil.memByteBuffer(this.pointer, (int)this.sizeBytes);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZJ)V", at = @At("RETURN"))
    private void constr(NativeImage.Format format, int width, int height, boolean useStb, long pointer, CallbackInfo ci) {
        if(this.pointer != 0) {
            buffer = MemoryUtil.memByteBuffer(this.pointer, (int)this.sizeBytes);
        }
    }

    /**
     * @author
     */
    @Overwrite
    private void uploadInternal(int level, int xOffset, int yOffset, int unpackSkipPixels, int unpackSkipRows, int widthIn, int heightIn, boolean blur, boolean clamp, boolean mipmap, boolean autoClose) {
        RenderSystem.assertOnRenderThreadOrInit();

        VTextureSelector.uploadSubTexture(level, widthIn, heightIn, xOffset, yOffset, this.format.getChannelCount(), unpackSkipRows, unpackSkipPixels, this.getWidth(), this.buffer);

        if (autoClose) {
            this.close();
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void loadFromTextureImage(int level, boolean removeAlpha) {
        RenderSystem.assertOnRenderThread();

        VulkanImage.downloadTexture(this.width, this.height, 4, this.buffer, Vulkan.getSwapChainImages().get(Drawer.getCurrentFrame()));

        if (removeAlpha && this.format.hasAlpha()) {
            for (int i = 0; i < this.height; ++i) {
                for (int j = 0; j < this.getWidth(); ++j) {
                    this.setColor(j, i, this.getColor(j, i) | 255 << this.format.getAlphaOffset());
                }
            }
        }

    }

}
