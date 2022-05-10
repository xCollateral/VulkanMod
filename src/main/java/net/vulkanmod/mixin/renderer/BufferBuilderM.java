package net.vulkanmod.mixin.renderer;

import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderM extends FixedColorVertexConsumer implements BufferVertexConsumer{


    @Shadow private boolean textured;

    @Shadow public abstract void putByte(int index, byte value);

    @Shadow public abstract void putShort(int index, short value);

    @Shadow public abstract void putFloat(int index, float value);

    @Shadow private boolean hasOverlay;

    @Shadow public abstract void next();

    @Shadow private int elementOffset;

    /**
     * @author
     */
    @Overwrite
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        VertexFormatElement vertexFormatElement = this.getCurrentElement();
        if (vertexFormatElement.getType() != VertexFormatElement.Type.COLOR) {
            return this;
        }
        if (vertexFormatElement.getDataType() != VertexFormatElement.DataType.FLOAT || vertexFormatElement.getLength() != 4) {
            throw new IllegalStateException();
        }
        this.putFloat(0, red / 255.0F);
        this.putFloat(4, green / 255.0F);
        this.putFloat(8, blue / 255.0F);
        this.putFloat(12, alpha / 255.0F);
        this.nextElement();
        return this;
    }

    /**
     * @author
     */
    @Overwrite
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        if (this.colorFixed) {
            throw new IllegalStateException();
        }
        if (this.textured) {
            int i;
            this.putFloat(0, x);
            this.putFloat(4, y);
            this.putFloat(8, z);
//            this.putByte(12, (byte)(red * 255.0f));
//            this.putByte(13, (byte)(green * 255.0f));
//            this.putByte(14, (byte)(blue * 255.0f));
//            this.putByte(15, (byte)(alpha * 255.0f));
//            this.putFloat(16, u);
//            this.putFloat(20, v);
//            if (this.hasOverlay) {
//                this.putShort(24, (short)(overlay & 0xFFFF));
//                this.putShort(26, (short)(overlay >> 16 & 0xFFFF));
//                i = 28;
//            } else {
//                i = 24;
//            }

            this.putFloat(12, red);
            this.putFloat(16, green);
            this.putFloat(20, blue);
            this.putFloat(24, alpha);
            this.putFloat(28, u);
            this.putFloat(32, v);
            if (this.hasOverlay) {
                this.putShort(36, (short)(overlay & '\uffff'));
                this.putShort(38, (short)(overlay >> 16 & '\uffff'));
                i = 40;
            } else {
                i = 36;
            }

            this.putShort(i + 0, (short)(light & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 0xFF0F)));
            this.putShort(i + 2, (short)(light >> 16 & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 0xFF0F)));
            this.putByte(i + 4, BufferVertexConsumer.packByte(normalX));
            this.putByte(i + 5, BufferVertexConsumer.packByte(normalY));
            this.putByte(i + 6, BufferVertexConsumer.packByte(normalZ));
            this.elementOffset += i + 8;
            this.next();
            return;
        }
        super.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
    }
}
