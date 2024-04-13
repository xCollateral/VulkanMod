package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightTexture.class)
public class MLightTexture {

    @Shadow @Final private DynamicTexture lightTexture;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void turnOffLightLayer() {

    }

    /**
     * @author
     */
    @Overwrite
    public void turnOnLightLayer() {
//        RenderSystem.setShaderTexture(2, this.textureIdentifier);
//        this.client.getTextureManager().bindTexture(this.textureIdentifier);
//        RenderSystem.texParameter(3553, 10241, 9729);
//        RenderSystem.texParameter(3553, 10240, 9729);
        VTextureSelector.setLightTexture(((VAbstractTextureI)this.lightTexture).getVulkanImage());
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
