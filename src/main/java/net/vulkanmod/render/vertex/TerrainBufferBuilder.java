package net.vulkanmod.render.vertex;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.util.SortUtil;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.function.IntConsumer;

public class TerrainBufferBuilder implements VertexConsumer {
	protected static final float POS_CONV = 1900.0f;
	protected static final float UV_CONV = 65536.0f;

	private static final int GROWTH_SIZE = 2097152;
	private static final Logger LOGGER = LogUtils.getLogger();

	private ByteBuffer buffer;
	private int renderedBufferCount;
	private int renderedBufferPointer;
	protected int nextElementByte;
	private int vertices;
	@Nullable
	private VertexFormatElement currentElement;
	private int elementIndex;
	private VertexFormat format;
	private VertexFormat.Mode mode;
	private boolean fastFormat;
	private boolean fullFormat;
	private boolean building;
	@Nullable
	private Vector3f[] sortingPoints;
	private float sortX = Float.NaN;
	private float sortY = Float.NaN;
	private float sortZ = Float.NaN;
	private boolean indexOnly;

	protected long bufferPtr;
//    private long ptr;

	protected VertexBuilder vertexBuilder;

	public TerrainBufferBuilder(int i) {
		this.buffer = MemoryTracker.create(i * 6);
		this.bufferPtr = MemoryUtil.memAddress0(this.buffer);

		this.vertexBuilder = PipelineManager.TERRAIN_VERTEX_FORMAT == CustomVertexFormat.COMPRESSED_TERRAIN ? new CompressedVertexBuilder() : new DefaultVertexBuilder();
	}

	private void ensureVertexCapacity() {
		this.ensureCapacity(this.format.getVertexSize());
	}

	private void ensureCapacity(int i) {
		if (this.nextElementByte + i > this.buffer.capacity()) {
			int j = this.buffer.capacity();
			int k = j + roundUp(i);
			LOGGER.debug((String)"Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", (Object)j, (Object)k);
			ByteBuffer byteBuffer = MemoryTracker.resize(this.buffer, k);
			byteBuffer.rewind();
			this.buffer = byteBuffer;
			this.bufferPtr = MemoryUtil.memAddress0(this.buffer);
		}
	}

	private static int roundUp(int i) {
		int j = GROWTH_SIZE;
		if (i == 0) {
			return j;
		} else {
			if (i < 0) {
				j *= -1;
			}

			int k = i % j;
			return k == 0 ? i : i + j - k;
		}
	}

	public void setQuadSortOrigin(float f, float g, float h) {
		if (this.mode == VertexFormat.Mode.QUADS) {
			if (this.sortX != f || this.sortY != g || this.sortZ != h) {
				this.sortX = f;
				this.sortY = g;
				this.sortZ = h;
				if (this.sortingPoints == null) {
					this.sortingPoints = this.makeQuadSortingPoints();
				}
			}

		}
	}

	public SortState getSortState() {
		return new SortState(this.mode, this.vertices, this.sortingPoints, this.sortX, this.sortY, this.sortZ);
	}

	public void restoreSortState(SortState sortState) {
		this.buffer.rewind();
		this.mode = sortState.mode;
		this.vertices = sortState.vertices;
		this.nextElementByte = this.renderedBufferPointer;
		this.sortingPoints = sortState.sortingPoints;
		this.sortX = sortState.sortX;
		this.sortY = sortState.sortY;
		this.sortZ = sortState.sortZ;
		this.indexOnly = true;
	}

	public void begin(VertexFormat.Mode mode, VertexFormat vertexFormat) {
		if (this.building) {
			throw new IllegalStateException("Already building!");
		} else {
			this.building = true;
			this.mode = mode;
			this.switchFormat(vertexFormat);
			this.currentElement = (VertexFormatElement)vertexFormat.getElements().get(0);
			this.elementIndex = 0;
			this.buffer.rewind();
		}
	}

	private void switchFormat(VertexFormat vertexFormat) {
		if (this.format != vertexFormat) {
			this.format = vertexFormat;
			boolean bl = vertexFormat == DefaultVertexFormat.NEW_ENTITY;
			boolean bl2 = vertexFormat == DefaultVertexFormat.BLOCK;
			this.fastFormat = bl || bl2;
			this.fullFormat = bl;
		}
	}

	private IntConsumer intConsumer(int i, VertexFormat.IndexType indexType) {
		MutableInt mutableInt = new MutableInt(i);
		IntConsumer var10000;
		switch (indexType) {
			case SHORT:
				var10000 = (ix) -> {
					this.buffer.putShort(mutableInt.getAndAdd(2), (short)ix);
				};
				break;
			case INT:
				var10000 = (ix) -> {
					this.buffer.putInt(mutableInt.getAndAdd(4), ix);
				};
				break;
			default:
				throw new IncompatibleClassChangeError();
		}

		return var10000;
	}

	private Vector3f[] makeQuadSortingPoints() {
		FloatBuffer floatBuffer = this.buffer.asFloatBuffer();
		int i = this.renderedBufferPointer / 4;
		int j = this.format.getIntegerSize();
		int stride = j * this.mode.primitiveStride;
		int pointsNum = this.vertices / this.mode.primitiveStride;
		Vector3f[] vector3fs = new Vector3f[pointsNum];

		if (this.format == CustomVertexFormat.COMPRESSED_TERRAIN) {
			stride = this.format.getVertexSize() * this.mode.primitiveStride;
			j = this.format.getVertexSize();
			float invConv = 1.0f / POS_CONV;
			for(int m = 0; m < pointsNum; ++m) {
				long ptr = this.bufferPtr + this.renderedBufferPointer + (long) m * stride;

				short x1 = MemoryUtil.memGetShort(ptr + 0);
				short y1 = MemoryUtil.memGetShort(ptr + 2);
				short z1 = MemoryUtil.memGetShort(ptr + 4);
//				short x2 = MemoryUtil.memGetShort(ptr + j * 2 + 0);
//				short y2 = MemoryUtil.memGetShort(ptr + j * 2 + 2);
//				short z2 = MemoryUtil.memGetShort(ptr + j * 2 + 4);
				//Am I wrong?
				short x2 = MemoryUtil.memGetShort(ptr + j * 3 + 0);
				short y2 = MemoryUtil.memGetShort(ptr + j * 3 + 2);
				short z2 = MemoryUtil.memGetShort(ptr + j * 3 + 4);

				float q = ((x1 * invConv) + (x2 * invConv)) * 0.5f;
				float r = ((y1 * invConv) + (y2 * invConv)) * 0.5f;
				float s = ((z1 * invConv) + (z2 * invConv)) * 0.5f;
				vector3fs[m] = new Vector3f(q, r, s);
			}
		} else {
			stride = this.format.getVertexSize() * this.mode.primitiveStride;
			j = this.format.getVertexSize();
			for(int m = 0; m < pointsNum; ++m) {
				long ptr = this.bufferPtr + this.renderedBufferPointer + (long) m * stride;

				float x1 = MemoryUtil.memGetFloat(ptr + 0);
				float y1 = MemoryUtil.memGetFloat(ptr + 4);
				float z1 = MemoryUtil.memGetFloat(ptr + 8);
				float x2 = MemoryUtil.memGetFloat(ptr + j * 2 + 0);
				float y2 = MemoryUtil.memGetFloat(ptr + j * 2 + 4);
				float z2 = MemoryUtil.memGetFloat(ptr + j * 2 + 8);

				float q = (x1 + x2) * 0.5f;
				float r = (y1 + y2) * 0.5f;
				float s = (z1 + z2) * 0.5f;
				vector3fs[m] = new Vector3f(q, r, s);
			}
		}

		return vector3fs;
	}

	private void putSortedQuadIndices(VertexFormat.IndexType indexType) {
		float[] distances = new float[this.sortingPoints.length];
		int[] is = new int[this.sortingPoints.length];

		for(int i = 0; i < this.sortingPoints.length; is[i] = i++) {
			float f = this.sortingPoints[i].x() - this.sortX;
			float g = this.sortingPoints[i].y() - this.sortY;
			float h = this.sortingPoints[i].z() - this.sortZ;
			distances[i] = f * f + g * g + h * h;
		}

		SortUtil.mergeSort(is, distances);
		IntConsumer intConsumer = this.intConsumer(this.nextElementByte, indexType);

		for(int i = 0; i < is.length; ++i) {
			int j = is[i];
			intConsumer.accept(j * this.mode.primitiveStride + 0);
			intConsumer.accept(j * this.mode.primitiveStride + 1);
			intConsumer.accept(j * this.mode.primitiveStride + 2);
			intConsumer.accept(j * this.mode.primitiveStride + 2);
			intConsumer.accept(j * this.mode.primitiveStride + 3);
			intConsumer.accept(j * this.mode.primitiveStride + 0);
		}
	}

	public boolean isCurrentBatchEmpty() {
		return this.vertices == 0;
	}

	@Nullable
	public RenderedBuffer endOrDiscardIfEmpty() {
		this.ensureDrawing();
		if (this.isCurrentBatchEmpty()) {
			this.reset();
			return null;
		} else {
			RenderedBuffer renderedBuffer = this.storeRenderedBuffer();
			this.reset();
			return renderedBuffer;
		}
	}

	public RenderedBuffer end() {
		this.ensureDrawing();
		RenderedBuffer renderedBuffer = this.storeRenderedBuffer();
		this.reset();
		return renderedBuffer;
	}

	private void ensureDrawing() {
		if (!this.building) {
			throw new IllegalStateException("Not building!");
		}
	}

	private RenderedBuffer storeRenderedBuffer() {
		int i = this.mode.indexCount(this.vertices);
		int j = !this.indexOnly ? this.vertices * this.format.getVertexSize() : 0;
		VertexFormat.IndexType indexType = VertexFormat.IndexType.least(i);
		boolean bl;
		int l;
		int k;
		if (this.sortingPoints != null) {
			k = Mth.roundToward(i * indexType.bytes, 4);
			this.ensureCapacity(k);
			this.putSortedQuadIndices(indexType);
			bl = false;
			this.nextElementByte += k;
			l = j + k;
		} else {
			bl = true;
			l = j;
		}

		int ptr = this.renderedBufferPointer;
		this.renderedBufferPointer += l;
		++this.renderedBufferCount;
		DrawState drawState = new DrawState(this.format, this.vertices, i, this.mode, indexType, this.indexOnly, bl);
		return new RenderedBuffer(ptr, drawState);
	}

	private void reset() {
		this.building = false;
		this.vertices = 0;
		this.currentElement = null;
		this.elementIndex = 0;
		this.sortingPoints = null;
		this.sortX = Float.NaN;
		this.sortY = Float.NaN;
		this.sortZ = Float.NaN;
		this.indexOnly = false;
	}

	public void endVertex() {
		if (this.elementIndex != 0) {
			throw new IllegalStateException("Not filled all elements of the vertex");
		} else {
			++this.vertices;
			this.ensureVertexCapacity();
			if (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP) {
				int i = this.format.getVertexSize();
				this.buffer.put(this.nextElementByte, this.buffer, this.nextElementByte - i, i);
				this.nextElementByte += i;
				++this.vertices;
				this.ensureVertexCapacity();
			}

		}
	}

	public void nextElement() {
		ImmutableList<VertexFormatElement> immutableList = this.format.getElements();
		this.elementIndex = (this.elementIndex + 1) % immutableList.size();
		this.nextElementByte += this.currentElement.getByteSize();
		VertexFormatElement vertexFormatElement = immutableList.get(this.elementIndex);
		this.currentElement = vertexFormatElement;
		if (vertexFormatElement.getUsage() == VertexFormatElement.Usage.PADDING) {
			this.nextElement();
		}

	}

	public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
//		this.defaultVertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
//		this.compressedVertex(x, y, z, red, green, blue, alpha, u, v, light);
		this.vertexBuilder.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
	}

	public void setBlockAttributes(BlockState blockState) {}

	@Override
	public VertexConsumer vertex(double d, double e, double f) {
		throw new UnsupportedOperationException();
	}

	@Override
	public VertexConsumer color(int i, int j, int k, int l) {
		throw new UnsupportedOperationException();
	}

	@Override
	public VertexConsumer uv(float f, float g) {
		throw new UnsupportedOperationException();
	}

	@Override
	public VertexConsumer overlayCoords(int i, int j) {
		throw new UnsupportedOperationException();
	}

	@Override
	public VertexConsumer uv2(int i, int j) {
		throw new UnsupportedOperationException();
	}

	@Override
	public VertexConsumer normal(float f, float g, float h) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void defaultColor(int i, int j, int k, int l) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unsetDefaultColor() {
		throw new UnsupportedOperationException();
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

	void releaseRenderedBuffer() {
		if (this.renderedBufferCount > 0 && --this.renderedBufferCount == 0) {
			this.clear();
		}

	}

	public void clear() {
		if (this.renderedBufferCount > 0) {
			LOGGER.warn("Clearing BufferBuilder with unused batches");
		}

		this.discard();
	}

	public void discard() {
		this.renderedBufferCount = 0;
		this.renderedBufferPointer = 0;
		this.nextElementByte = 0;
	}

	public VertexFormatElement currentElement() {
		if (this.currentElement == null) {
			throw new IllegalStateException("BufferBuilder not started");
		} else {
			return this.currentElement;
		}
	}

	public boolean building() {
		return this.building;
	}

	ByteBuffer bufferSlice(int i, int j) {
		return MemoryUtil.memSlice(this.buffer, i, j - i);
	}

	public static class SortState {
		final VertexFormat.Mode mode;
		final int vertices;
		@Nullable
		final Vector3f[] sortingPoints;
		final float sortX;
		final float sortY;
		final float sortZ;

		SortState(VertexFormat.Mode mode, int i, @Nullable Vector3f[] vector3fs, float f, float g, float h) {
			this.mode = mode;
			this.vertices = i;
			this.sortingPoints = vector3fs;
			this.sortX = f;
			this.sortY = g;
			this.sortZ = h;
		}
	}

	public class RenderedBuffer {
		private final int pointer;
		private final DrawState drawState;
		private boolean released;

		RenderedBuffer(int pointer, DrawState drawState) {
			this.pointer = pointer;
			this.drawState = drawState;
		}

		public ByteBuffer vertexBuffer() {
			int i = this.pointer + this.drawState.vertexBufferStart();
			int j = this.pointer + this.drawState.vertexBufferEnd();
			return TerrainBufferBuilder.this.bufferSlice(i, j);
		}

		public ByteBuffer indexBuffer() {
			int i = this.pointer + this.drawState.indexBufferStart();
			int j = this.pointer + this.drawState.indexBufferEnd();
			return TerrainBufferBuilder.this.bufferSlice(i, j);
		}

		public DrawState drawState() {
			return this.drawState;
		}

		public boolean isEmpty() {
			return this.drawState.vertexCount == 0;
		}

		public void release() {
			if (this.released) {
				throw new IllegalStateException("Buffer has already been released!");
			} else {
				TerrainBufferBuilder.this.releaseRenderedBuffer();
				this.released = true;
			}
		}
	}

	public record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType, boolean indexOnly, boolean sequentialIndex) {

		public int vertexBufferSize() {
			return this.vertexCount * this.format.getVertexSize();
		}

		public int vertexBufferStart() {
			return 0;
		}

		public int vertexBufferEnd() {
			return this.vertexBufferSize();
		}

		public int indexBufferStart() {
			return this.indexOnly ? 0 : this.vertexBufferEnd();
		}

		public int indexBufferEnd() {
			return this.indexBufferStart() + this.indexBufferSize();
		}

		private int indexBufferSize() {
			return this.sequentialIndex ? 0 : this.indexCount * this.indexType.bytes;
		}

		public int bufferSize() {
			return this.indexBufferEnd();
		}

		public VertexFormat format() {
			return this.format;
		}

		public int vertexCount() {
			return this.vertexCount;
		}

		public int indexCount() {
			return this.indexCount;
		}

		public VertexFormat.Mode mode() {
			return this.mode;
		}

		public VertexFormat.IndexType indexType() {
			return this.indexType;
		}

		public boolean indexOnly() {
			return this.indexOnly;
		}

		public boolean sequentialIndex() {
			return this.sequentialIndex;
		}
	}

	public interface VertexBuilder {
		void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);
	}

	class DefaultVertexBuilder implements VertexBuilder {

		public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
			putFloat(0, x);
			putFloat(4, y);
			putFloat(8, z);
			putByte(12, (byte)((int)(red * 255.0F)));
			putByte(13, (byte)((int)(green * 255.0F)));
			putByte(14, (byte)((int)(blue * 255.0F)));
			putByte(15, (byte)((int)(alpha * 255.0F)));
			putFloat(16, u);
			putFloat(20, v);
			byte i;
			i = 24;

			putShort(i, (short)(light & '\uffff'));
			putShort(i + 2, (short)(light >> 16 & '\uffff'));
			putByte(i + 4, BufferVertexConsumer.normalIntValue(normalX));
			putByte(i + 5, BufferVertexConsumer.normalIntValue(normalY));
			putByte(i + 6, BufferVertexConsumer.normalIntValue(normalZ));
			nextElementByte += i + 8;
			endVertex();
		}
	}

	class CompressedVertexBuilder implements VertexBuilder {

		public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
			long ptr = bufferPtr + nextElementByte;

			short sX = (short) (x * POS_CONV + 0.1f);
			short sY = (short) (y * POS_CONV + 0.1f);
			short sZ = (short) (z * POS_CONV + 0.1f);

			//		short sX = (short) (x * 1.0001f * POS_CONV + 0.1f);
			//		short sY = (short) (y * 1.0001f * POS_CONV + 0.1f);
			//		short sZ = (short) (z * 1.0001f * POS_CONV + 0.1f);
			//		short sX = (short) (x * 0.99999999f * POS_CONV);
			//		short sY = (short) (y * 0.99999999f * POS_CONV);
			//		short sZ = (short) (z * 0.99999999f * POS_CONV);

			//Debug
			//		short x1 = (short) Math.round((x) * POS_CONV);
			//		float y1 = (short) Math.round((y) * POS_CONV);
			//		float z1 = (short) Math.round((z) * POS_CONV);
			//
			//		if(x1 != sX || y1 != sY || z1 != sZ)
			//			System.nanoTime();

			MemoryUtil.memPutShort(ptr + 0, sX);
			MemoryUtil.memPutShort(ptr + 2, sY);
			MemoryUtil.memPutShort(ptr + 4, sZ);

			int temp = VertexUtil.packColor(red, green, blue, alpha);
			MemoryUtil.memPutInt(ptr + 8, temp);

			MemoryUtil.memPutShort(ptr + 12, (short) (u * UV_CONV));
			MemoryUtil.memPutShort(ptr + 14, (short) (v * UV_CONV));

			MemoryUtil.memPutInt(ptr + 16, light);

			nextElementByte += 20;
			endVertex();
		}
	}
}
