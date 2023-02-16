package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderM extends DefaultedVertexConsumer
        implements BufferVertexConsumer {

    @Shadow public abstract void endVertex();

//    /**
//     * @author
//     */
//    @Overwrite
//    public VertexConsumer color(int red, int green, int blue, int alpha) {
//        VertexFormatElement vertexFormatElement = this.getCurrentElement();
//        if (vertexFormatElement.getType() != VertexFormatElement.Type.COLOR) {
//            return this;
//        }
//        if (vertexFormatElement.getDataType() != VertexFormatElement.DataType.FLOAT || vertexFormatElement.getLength() != 4) {
//            throw new IllegalStateException();
//        }
//        this.putFloat(0, red / 255.0F);
//        this.putFloat(4, green / 255.0F);
//        this.putFloat(8, blue / 255.0F);
//        this.putFloat(12, alpha / 255.0F);
//        this.nextElement();
//        return this;
//    }
//
    @Shadow private ByteBuffer buffer;

    @Shadow private int nextElementByte;
    @Shadow private boolean fastFormat;
    @Shadow private boolean fullFormat;
    private long ptr;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setPtrC(int initialCapacity, CallbackInfo ci) {
        this.ptr = MemoryUtil.memAddress0(this.buffer);
    }

    @Inject(method = "ensureCapacity", at = @At(value = "RETURN"))
    private void setPtr(int initialCapacity, CallbackInfo ci) {
        this.ptr = MemoryUtil.memAddress0(this.buffer);
    }

//    /**
//     * @author
//     */
//    @Overwrite
//    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
//        if (this.textured) {
//            this.putFloat(0, x);
//            this.putFloat(4, y);
//            this.putFloat(8, z);
//            this.putByte(12, (byte)((int)(red * 255.0F)));
//            this.putByte(13, (byte)((int)(green * 255.0F)));
//            this.putByte(14, (byte)((int)(blue * 255.0F)));
//            this.putByte(15, (byte)((int)(alpha * 255.0F)));
//            this.putFloat(16, u);
//            this.putFloat(20, v);
//            byte i;
//            if (this.hasOverlay) {
//                this.putShort(24, (short)(overlay & '\uffff'));
//                this.putShort(26, (short)(overlay >> 16 & '\uffff'));
//
//                this.putShort(28, (short)(light & '\uffff'));
//                this.putShort(30, (short)(light >> 16 & '\uffff'));
//                this.putByte(32, BufferVertexConsumer.packByte(normalX));
//                this.putByte(33, BufferVertexConsumer.packByte(normalY));
//                this.putByte(34, BufferVertexConsumer.packByte(normalZ));
//                this.elementOffset += 36;
//            } else {
//
//                this.putShort(24, (short)(light & '\uffff'));
//                this.putShort(26, (short)(light >> 16 & '\uffff'));
//                this.putByte(28, BufferVertexConsumer.packByte(normalX));
//                this.putByte(29, BufferVertexConsumer.packByte(normalY));
//                this.putByte(30, BufferVertexConsumer.packByte(normalZ));
//                this.elementOffset += 32;
//            }
//
////            this.putShort(i + 0, (short)(light & '\uffff'));
////            this.putShort(i + 2, (short)(light >> 16 & '\uffff'));
////            this.putByte(i + 4, BufferVertexConsumer.packByte(normalX));
////            this.putByte(i + 5, BufferVertexConsumer.packByte(normalY));
////            this.putByte(i + 6, BufferVertexConsumer.packByte(normalZ));
////            this.elementOffset += i + 8;
//            this.next();
//        } else {
//            super.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
//        }
//    }
//TODO--->!
    /**
     * @author
     */
    @Overwrite
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        if (this.fastFormat) {
            this.putFloat(0, x);
            this.putFloat(4, y);
            this.putFloat(8, z);
            int R = ((int) (red*255)&255);
            int G = ((int) (green*255)<<8);
            int B = ((int) (blue*255)<<16);
            int A = ((int)(alpha*255)<<24);
            this.putInt(12, R | G | B | A);


            this.putFloat(16, u);
            this.putFloat(20, v);
            int i=24;
            if (this.fullFormat) {
                this.putInt(24, overlay);
                i = 28;
            }

            this.putInt(i, (light));
            this.putInt(i + 4, (int) (normalX * 127) & 255 | ((int) (normalY * 127) & 255) <<8 | ((int) (normalZ * 127) & 255) << 16);
            this.nextElementByte += i + 8;
            this.endVertex();
        } else {
            super.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }

    private void putLong(long index, long value) {
        MemoryUtil.memPutLong(this.ptr + this.nextElementByte + index, value);
    }
    private void putInt(int index, int value) {
        MemoryUtil.memPutInt(this.ptr + this.nextElementByte + index, value);
    }

    public void putByte(int index, byte value) {
        MemoryUtil.memPutByte(this.ptr + this.nextElementByte + index, value);
    }

    public void putShort(int index, short value) {
        MemoryUtil.memPutShort(this.ptr + this.nextElementByte + index, value);
    }

    public void putFloat(int index, float value) {
        MemoryUtil.memPutFloat(this.ptr + this.nextElementByte + index, value);
    }
}
