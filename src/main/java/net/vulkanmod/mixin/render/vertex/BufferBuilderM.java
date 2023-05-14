package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.*;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.render.vertex.VertexUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.List;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderM extends DefaultedVertexConsumer
        implements BufferVertexConsumer, ExtendedVertexBuilder {

    @Shadow public abstract void endVertex();

    @Shadow private ByteBuffer buffer;

    @Shadow private int nextElementByte;
    @Shadow private boolean fastFormat;
    @Shadow private boolean fullFormat;
    @Shadow private int elementIndex;
    @Shadow private @Nullable VertexFormatElement currentElement;
    @Shadow private VertexFormat format;

    private long bufferPtr;
    private long ptr;
    private int offset;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setPtrC(int initialCapacity, CallbackInfo ci) {
        this.bufferPtr = MemoryUtil.memAddress0(this.buffer);
    }

    @Inject(method = "ensureCapacity", at = @At(value = "RETURN"))
    private void setPtr(int initialCapacity, CallbackInfo ci) {
        this.bufferPtr = MemoryUtil.memAddress0(this.buffer);
    }

    public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
        this.ptr = this.nextElementPtr();

        if(this.format == DefaultVertexFormat.NEW_ENTITY) {
            MemoryUtil.memPutFloat(ptr + 0, x);
            MemoryUtil.memPutFloat(ptr + 4, y);
            MemoryUtil.memPutFloat(ptr + 8, z);

            MemoryUtil.memPutInt(ptr + 12, packedColor);

            MemoryUtil.memPutFloat(ptr + 16, u);
            MemoryUtil.memPutFloat(ptr + 20, v);

            MemoryUtil.memPutInt(ptr + 24, overlay);

            MemoryUtil.memPutInt(ptr + 28, light);
            MemoryUtil.memPutInt(ptr + 32, packedNormal);

            this.nextElementByte += 36;
            this.endVertex();

        }
        else {
            this.vertex(x, y, z);
            this.fastColor(packedColor);
            this.fastUv(u, v);
            this.fastOverlay(overlay);
            this.light(light);
            this.fastNormal(packedNormal);
            this.endVertex();
//            throw new RuntimeException("unaccepted format: " + this.format);
        }

    }

    public void vertex(float x, float y, float z) {
        MemoryUtil.memPutFloat(ptr + 0, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);

        this.nextElement();
    }

    public void fastColor(int packedColor) {
        if (this.currentElement.getUsage() != VertexFormatElement.Usage.COLOR) return;

        MemoryUtil.memPutFloat(ptr + 12, packedColor);

        this.nextElement();
    }

    public void fastUv(float u, float v) {
        if (this.currentElement.getUsage() != VertexFormatElement.Usage.UV) return;

        MemoryUtil.memPutFloat(ptr + 16, u);
        MemoryUtil.memPutFloat(ptr + 20, v);

        this.nextElement();
    }

    public void fastOverlay(int o) {
        if (this.currentElement.getUsage() != VertexFormatElement.Usage.UV) return;

        MemoryUtil.memPutInt(ptr + 24, o);

        this.nextElement();
    }

    public void light(int l) {
        if (this.currentElement.getUsage() != VertexFormatElement.Usage.UV) return;

        MemoryUtil.memPutInt(ptr + 28, l);

        this.nextElement();
    }

    public void fastNormal(int packedNormal) {
        if (this.currentElement.getUsage() != VertexFormatElement.Usage.NORMAL) return;

        MemoryUtil.memPutInt(ptr + 32, packedNormal);

        this.nextElement();
    }

    /**
     * @author
     */
    @Overwrite
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        if (this.fastFormat) {
            this.putFloat(0, x);
            this.putFloat(4, y);
            this.putFloat(8, z);
            this.putByte(12, (byte)((int)(red * 255.0F)));
            this.putByte(13, (byte)((int)(green * 255.0F)));
            this.putByte(14, (byte)((int)(blue * 255.0F)));
            this.putByte(15, (byte)((int)(alpha * 255.0F)));
            this.putFloat(16, u);
            this.putFloat(20, v);
            byte i;
            if (this.fullFormat) {
                this.putShort(24, (short)(overlay & '\uffff'));
                this.putShort(26, (short)(overlay >> 16 & '\uffff'));
                i = 28;
            } else {
                i = 24;
            }

            this.putShort(i, (short)(light & '\uffff'));
            this.putShort(i + 2, (short)(light >> 16 & '\uffff'));
            this.putByte(i + 4, BufferVertexConsumer.normalIntValue(normalX));
            this.putByte(i + 5, BufferVertexConsumer.normalIntValue(normalY));
            this.putByte(i + 6, BufferVertexConsumer.normalIntValue(normalZ));
            this.nextElementByte += i + 8;
            this.endVertex();
        } else {
            super.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void nextElement() {
        VertexFormatElement vertexFormatElement;
        List<VertexFormatElement> list = ((VertexFormatMixed)(this.format)).getFastList();

        this.elementIndex = (this.elementIndex + 1) % list.size();
        this.nextElementByte += this.currentElement.getByteSize();

        this.currentElement = vertexFormatElement = list.get(this.elementIndex);

        if (vertexFormatElement.getUsage() == VertexFormatElement.Usage.PADDING) {
            this.nextElement();
        }
        if (this.defaultColorSet && this.currentElement.getUsage() == VertexFormatElement.Usage.COLOR) {
            BufferVertexConsumer.super.color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
        }
    }

    public void putByte(int index, byte value) {
        MemoryUtil.memPutByte(this.bufferPtr + this.nextElementByte + index, value);
    }

    public void putShort(int index, short value) {
        MemoryUtil.memPutShort(this.bufferPtr + this.nextElementByte + index, value);
    }

    public void putFloat(int index, float value) {
        MemoryUtil.memPutFloat(this.bufferPtr + this.nextElementByte + index, value);
    }

    private long nextElementPtr() {
        return (this.bufferPtr + this.nextElementByte);
    }

    protected void setNextElementByte(int i) {
        this.nextElementByte = i;
    }
}
