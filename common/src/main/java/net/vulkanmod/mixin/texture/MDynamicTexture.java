package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DynamicTexture.class)
public abstract class MDynamicTexture extends AbstractTexture {

    @Shadow private NativeImage pixels;

    @Shadow public abstract void upload();

    @Redirect(method = "<init>(IIZ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(III)V"))
    private void redirect(int id, int width, int height) {
        createTexture();
    }

    @Redirect(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(III)V"))
    private void redirect2(int id, int width, int height) {
        createTexture();
    }

//    @Redirect(method = "<init>*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(III)V"))
//    private void redirect(int id, int width, int height) {
//
//        createTexture();
//
//        //debug
//        System.out.println("Dynamic texture mixin");
//    }

//    @Redirect(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;recordRenderCall(Lcom/mojang/blaze3d/pipeline/RenderCall;)V"))
//    private void redirect2(RenderCall renderCall) {
//
//        createTexture();
//    }

    @Redirect(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;recordRenderCall(Lcom/mojang/blaze3d/pipeline/RenderCall;)V"))
    private void redirect2(RenderCall renderCall) {

        RenderSystem.recordRenderCall(() -> {
            createTexture();
            this.upload();
        });

    }

    private void createTexture() {
        VAbstractTextureI texture = ((VAbstractTextureI)(this));

        VulkanImage vulkanImage = new VulkanImage.Builder(this.pixels.getWidth(), this.pixels.getHeight()).createVulkanImage();
        texture.setVulkanImage(vulkanImage);
        texture.bindTexture();
//        texture.setId(TextureMap.getId(vulkanImage));
    }

}
