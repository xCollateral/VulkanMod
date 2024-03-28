package net.vulkanmod.render.model.quad;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public class ModelQuad implements QuadView {
    public static final int VERTEX_SIZE = 8;

    private final int[] data = new int[4 * VERTEX_SIZE];

    Direction direction;
    TextureAtlasSprite sprite;

    private int flags;
    
    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx)]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + 2]);
    }

    @Override
    public int getColor(int idx) {
        return this.data[vertexOffset(idx) + 3];
    }

    @Override
    public float getU(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + 4]);
    }

    @Override
    public float getV(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + 5]);
    }

    @Override
    public int getColorIndex() {
        return -1;
    }

    @Override
    public Direction getFacingDirection() {
        return this.direction;
    }

    public float setX(int idx, float f) {
        return this.data[vertexOffset(idx)] = Float.floatToRawIntBits(f);
    }

    public float setY(int idx, float f) {
        return this.data[vertexOffset(idx) + 1] = Float.floatToRawIntBits(f);
    }

    public float setZ(int idx, float f) {
        return this.data[vertexOffset(idx) + 2] = Float.floatToRawIntBits(f);

    }

    public float setU(int idx, float f) {
        return this.data[vertexOffset(idx) + 4] = Float.floatToRawIntBits(f);

    }

    public float setV(int idx, float f) {
        return this.data[vertexOffset(idx) + 5] = Float.floatToRawIntBits(f);

    }

    public void setFlags(int f) {
        this.flags = f;
    }

    public void setSprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    private static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }
}
