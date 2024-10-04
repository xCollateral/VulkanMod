package net.vulkanmod.render.vertex;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.PipelineManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class TerrainBufferBuilder {
    private static final Logger LOGGER = Initializer.LOGGER;
    private static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);

    private int capacity;
    private int vertexSize;

    protected long bufferPtr;

    protected int nextElementByte;
    int vertices;

    private VertexBuilder vertexBuilder;

//    private int renderedBufferCount;
//    private int renderedBufferPointer;

    public TerrainBufferBuilder(int size, int vertexSize, VertexBuilder vertexBuilder) {
        this.bufferPtr = ALLOCATOR.malloc(size);
        this.capacity = size;
        this.vertexSize = vertexSize;

//        this.format = PipelineManager.TERRAIN_VERTEX_FORMAT;
        this.vertexBuilder = PipelineManager.TERRAIN_VERTEX_FORMAT == CustomVertexFormat.COMPRESSED_TERRAIN
                ? new VertexBuilder.CompressedVertexBuilder() : new VertexBuilder.DefaultVertexBuilder();
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
//        this.buffer.limit(this.nextElementByte);
    }

    public void clear() {
        this.nextElementByte = 0;
        this.vertices = 0;
//        this.buffer.clear();
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
}
