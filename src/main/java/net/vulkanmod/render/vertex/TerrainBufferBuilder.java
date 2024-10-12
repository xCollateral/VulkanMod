package net.vulkanmod.render.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.vertex.format.I32_SNorm;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class TerrainBufferBuilder implements VertexConsumer {
    private static final Logger LOGGER = Initializer.LOGGER;
    private static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);

    private int capacity;
    private int vertexSize;

    protected long bufferPtr;

    protected int nextElementByte;
    int vertices;

	private long elementPtr;

    private VertexBuilder vertexBuilder;

    public TerrainBufferBuilder(int size, int vertexSize, VertexBuilder vertexBuilder) {
        this.bufferPtr = ALLOCATOR.malloc(size);
        this.capacity = size;
        this.vertexSize = vertexSize;
        this.vertexBuilder = vertexBuilder;
    }

    public void ensureCapacity() {
        this.ensureCapacity(this.vertexSize * 4);
    }

    private void ensureCapacity(int size) {
        if (this.nextElementByte + size > this.capacity) {
            int capacity = this.capacity;
            int newSize = (capacity + size) * 2;
            this.resize(newSize);
        }
    }

    private void resize(int i) {
        this.bufferPtr = ALLOCATOR.realloc(this.bufferPtr, i);
        LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.capacity, i);
        if (this.bufferPtr == 0L) {
            throw new OutOfMemoryError("Failed to resize buffer from " + this.capacity + " bytes to " + i + " bytes");
        } else {
            this.capacity = i;
        }
    }

    public void endVertex() {
        this.nextElementByte += this.vertexSize;
        ++this.vertices;
    }

    public void vertex(float x, float y, float z, int color, float u, float v, int light, int packedNormal) {
        final long ptr = this.bufferPtr + this.nextElementByte;
        this.vertexBuilder.vertex(ptr, x, y, z, color, u, v, light, packedNormal);
        this.endVertex();
    }

    public void end() {
    }

    public void clear() {
        this.nextElementByte = 0;
        this.vertices = 0;
    }

    public ByteBuffer getBuffer() {
        return MemoryUtil.memByteBuffer(this.bufferPtr, this.vertices * this.vertexSize);
    }

    public long getPtr() {
        return bufferPtr;
    }

    public int getVertices() {
        return vertices;
    }

    public int getNextElementByte() {
        return nextElementByte;
    }

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		this.elementPtr = this.bufferPtr + this.nextElementByte;
		this.endVertex();

		this.vertexBuilder.position(this.elementPtr, x, y, z);

		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		int color = (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);

		this.vertexBuilder.color(this.elementPtr, color);

		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		this.vertexBuilder.uv(this.elementPtr, u, v);

		return this;
	}

	public VertexConsumer setLight(int i) {
		this.vertexBuilder.light(this.elementPtr, i);

		return this;
	}

	@Override
	public VertexConsumer setNormal(float f, float g, float h) {
		int packedNormal = I32_SNorm.packNormal(f, g, h);

		this.vertexBuilder.normal(this.elementPtr, packedNormal);

		return this;
	}

	@Override
	public VertexConsumer setUv1(int i, int j) {
		return this;
	}

	@Override
	public VertexConsumer setUv2(int i, int j) {
		return this;
	}
}
