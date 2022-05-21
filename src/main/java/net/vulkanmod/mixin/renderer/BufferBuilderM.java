package net.vulkanmod.mixin.renderer;

import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderM extends FixedColorVertexConsumer implements BufferVertexConsumer{
    private static boolean useFloat = false;

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
        //if (vertexFormatElement.getDataType() != VertexFormatElement.DataType.FLOAT || vertexFormatElement.getLength() != 4) {
            //throw new IllegalStateException();
        //}
        if (vertexFormatElement.getDataType()  == VertexFormatElement.DataType.FLOAT) {
            this.putFloat(0, red / 255.0F);
            this.putFloat(4, green / 255.0F);
            this.putFloat(8, blue / 255.0F);
            this.putFloat(12, alpha / 255.0F);
        } else {
            this.putByte(0, (byte)red );
            this.putByte(1, (byte)green);
            this.putByte(2, (byte)blue);
            this.putByte(3, (byte)alpha);
        }
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
            this.putFloat(0, x);
            this.putFloat(4, y);
            this.putFloat(8, z);
            int i = 12;

            if (useFloat) {
                this.putFloat(i+0, red);
                this.putFloat(i+4, green);
                this.putFloat(i+8, blue);
                this.putFloat(i+12, alpha);
                i+=16;
            } else {
                this.putByte(i+0, (byte) (red*255.f));
                this.putByte(i+1, (byte) (green*255.f));
                this.putByte(i+2, (byte) (blue*255.f));
                this.putByte(i+3, (byte) (alpha*255.f));
                i+=4;
            }
            this.putFloat(i, u);
            this.putFloat(i+4, v);
            i+=8;
            if (this.hasOverlay) {
                this.putShort(i, (short)(overlay & '\uffff'));
                this.putShort(i+2, (short)(overlay >> 16 & '\uffff'));
                i+=4;
            } else {
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
