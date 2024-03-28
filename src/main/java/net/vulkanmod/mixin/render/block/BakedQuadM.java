package net.vulkanmod.mixin.render.block;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.model.quad.ModelQuadFlags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vulkanmod.render.model.quad.ModelQuad.VERTEX_SIZE;

@Mixin(BakedQuad.class)
public class BakedQuadM implements QuadView {

    @Shadow @Final protected int[] vertices;
    @Shadow @Final protected Direction direction;
    @Shadow @Final protected int tintIndex;
    private int flags;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite textureAtlasSprite, boolean shade, CallbackInfo ci) {
        this.flags = ModelQuadFlags.getQuadFlags(vertices, direction);
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 0]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 2]);
    }

    @Override
    public int getColor(int idx) {
        return this.vertices[vertexOffset(idx) + 3];
    }

    @Override
    public float getU(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 4]);
    }

    @Override
    public float getV(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 5]);
    }

    @Override
    public int getColorIndex() {
        return this.tintIndex;
    }

    @Override
    public Direction getFacingDirection() {
        return this.direction;
    }

    @Override
    public boolean isTinted() {
        return this.tintIndex != -1;
    }

    private static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }
}
