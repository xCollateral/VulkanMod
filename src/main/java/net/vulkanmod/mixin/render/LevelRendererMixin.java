package net.vulkanmod.mixin.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow private @Nullable PostChain entityEffect;

    /**
     * @author
     */
    @Overwrite
    public void initOutline() {
        if (this.entityEffect != null) {
            this.entityEffect.close();
        }
//        Identifier identifier = new Identifier("shaders/post/entity_outline.json");
//        try {
//            this.entityOutlineShader = new ShaderEffect(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getFramebuffer(), identifier);
//            this.entityOutlineShader.setupDimensions(this.minecraft.getWindow().getFramebufferWidth(), this.minecraft.getWindow().getFramebufferHeight());
//            this.entityOutlinesFramebuffer = this.entityOutlineShader.getSecondaryTarget("final");
//        }
//        catch (IOException iOException) {
//            LOGGER.warn("Failed to load shader: {}", (Object)identifier, (Object)iOException);
//            this.entityOutlineShader = null;
//            this.entityOutlinesFramebuffer = null;
//        }
//        catch (JsonSyntaxException iOException) {
//            LOGGER.warn("Failed to parse shader: {}", (Object)identifier, (Object)iOException);
//            this.entityOutlineShader = null;
//            this.entityOutlinesFramebuffer = null;
//        }
    }
}
