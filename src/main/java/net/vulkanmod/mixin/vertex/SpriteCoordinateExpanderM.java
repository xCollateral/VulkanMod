package net.vulkanmod.mixin.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO move
@Mixin(SpriteCoordinateExpander.class)
public class SpriteCoordinateExpanderM implements ExtendedVertexBuilder {

    @Shadow @Final private VertexConsumer delegate;

    @Shadow @Final private TextureAtlasSprite sprite;
    private ExtendedVertexBuilder extDelegate;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void getExtBuilder(VertexConsumer vertexConsumer, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        this.extDelegate = (ExtendedVertexBuilder) vertexConsumer;
    }

    @Override
    public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
        this.extDelegate.vertex(x, y, z, packedColor, this.sprite.getU(u * 16.0F), this.sprite.getV(v * 16.0F), overlay, light, packedNormal);
    }
}
