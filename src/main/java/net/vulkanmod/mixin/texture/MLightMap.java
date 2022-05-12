package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightmapTextureManager.class)
public class MLightMap {

    @Shadow @Final private Identifier textureIdentifier;

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private NativeImageBackedTexture texture;

    /**
     * @author
     */
    @Overwrite
    public void enable() {
//        RenderSystem.setShaderTexture(2, this.textureIdentifier);
//        this.client.getTextureManager().bindTexture(this.textureIdentifier);
//        RenderSystem.texParameter(3553, 10241, 9729);
//        RenderSystem.texParameter(3553, 10240, 9729);
        VTextureSelector.setLightTexture(((VAbstractTextureI)this.texture).getVulkanImage());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
