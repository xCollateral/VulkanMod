package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.ints.IntArrays;
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
    @Shadow private int elementIndex;
    @Shadow private @Nullable VertexFormatElement currentElement;
    @Shadow private VertexFormat format;

    @Shadow private int renderedBufferPointer;
    @Shadow private VertexFormat.Mode mode;
    @Shadow private int vertices;
    private Vector3f[] sortingPoints;
    private int sortX;
    private int sortY;
    private int sortZ;

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

    public void vertex(VertexUtil.GenericVertex v) {
        this.vertex(v.x(), v.y(), v.z(), v.packedColor(), v.u(), v.v(), v.overlay(), v.light(), v.packedNormal());
    }

    @Overwrite
    public void setQuadSortOrigin(float f, float g, float h) {
        if (this.mode == VertexFormat.Mode.QUADS) {
            setSortOrigin2((int) f, (int) g, (int) h);
        }
    }

    private void setSortOrigin2(int f, int g, int h) {
        if (this.sortX != f || this.sortY != g || this.sortZ != h) {
            this.sortX = f;
            this.sortY = g;
            this.sortZ = h;
            if (this.sortingPoints == null) {
                this.sortingPoints = this.makeQuadSortingPoints();
            }
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void putSortedQuadIndices(VertexFormat.IndexType indexType) {
        final int vertexCount = this.vertices / Integer.BYTES;
        int[] dist = new int[vertexCount];
        int[] indices = new int[vertexCount];

        for (int i = 0; i < vertexCount; indices[i] = i++) {
            int f = (int) this.sortingPoints[i].x() - this.sortX;
            int g = (int) this.sortingPoints[i].y() - this.sortY;
            int h = (int) this.sortingPoints[i].z() - this.sortZ;
            dist[i] = f * f + g * g + h * h;
        }

        IntArrays.unstableSort(indices, (ix, jx) -> Integer.compare(dist[jx], dist[ix]));
        int mutableInt = this.nextElementByte;


        for (final int quadSet : indices) {
            final int quadIdxSet = quadSet * Integer.BYTES;
            this.buffer.putInt(mutableInt, quadIdxSet + 1 << 16 | quadIdxSet);
            this.buffer.putInt(mutableInt+4, quadIdxSet + 2 << 16 | quadIdxSet + 2);
            this.buffer.putInt(mutableInt+8, quadIdxSet << 16 | quadIdxSet + 3);
            mutableInt+=12;
        }

    }

    @Overwrite
    private Vector3f[] makeQuadSortingPoints() {
        int vecPos = this.renderedBufferPointer / 4;
        final int vecSizeStride = 32/4;
        int k = vecSizeStride * Integer.BYTES;
        int l = this.vertices / Integer.BYTES;
        Vector3f[] vector3fs = new Vector3f[l];

        for (int m = 0; m < l; ++m) {
            vector3fs[m] = new Vector3f(
                    (int)(this.buffer.getFloat((vecPos + m * k)*4) + this.buffer.getFloat((vecPos + m * k + vecSizeStride * 2)*4)) / 2,
                    (int)(this.buffer.getFloat((vecPos + m * k + 1)*4) + this.buffer.getFloat((vecPos + m * k + vecSizeStride * 2 + 1)*4)) / 2,
                    (int)(this.buffer.getFloat((vecPos + m * k + 2)*4) + this.buffer.getFloat((vecPos + m * k + vecSizeStride * 2 + 2)*4)) / 2);
        }

        return vector3fs;
    }

    private float[] makeQuadSortingPoints2() {
        int vecPos = this.renderedBufferPointer / 4;
        final int vecSizeStride = 32/4;
        int k = vecSizeStride * Integer.BYTES;
        int l = this.vertices / Integer.BYTES;
        float[] vector3fs = new float[l*3];

        for (int m = 0; m < l; ++m) {
            vector3fs[m] =(this.buffer.getFloat((vecPos + m * k)*4) + this.buffer.getFloat((vecPos + m * k + vecSizeStride * 2)*4)) / 2;
            vector3fs[m+1] = (this.buffer.getFloat((vecPos + m * k + 1)*4) + this.buffer.getFloat((vecPos + m * k + vecSizeStride * 2 + 1)*4)) / 2;
            vector3fs[m+2] = (this.buffer.getFloat((vecPos + m * k + 2)*4) + this.buffer.getFloat((vecPos + m * k + vecSizeStride * 2 + 2)*4)) / 2;
        }

        return vector3fs;
    }

    public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
        this.ptr = this.nextElementPtr();

        if(this.format == DefaultVertexFormat.NEW_ENTITY) {
            MemoryUtil.memPutFloat(ptr, x);
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
        MemoryUtil.memPutFloat(ptr, x);
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
}
