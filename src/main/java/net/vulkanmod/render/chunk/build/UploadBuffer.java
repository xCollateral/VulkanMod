package net.vulkanmod.render.chunk.build;

import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.chunk.util.BufferUtil;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainBuilder;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class UploadBuffer {
    public final int indexCount;
    public final boolean autoIndices;
    public final boolean indexOnly;
    private final ByteBuffer[] vertexBuffers;
    private final ByteBuffer indexBuffer;

    public UploadBuffer(TerrainBuilder terrainBuilder, TerrainBuilder.DrawState drawState) {
        this.indexCount = drawState.indexCount();
        this.autoIndices = drawState.sequentialIndex();
        this.indexOnly = drawState.indexOnly();

        if (!this.indexOnly) {
            this.vertexBuffers = new ByteBuffer[QuadFacing.COUNT];

            for (int i = 0; i < QuadFacing.COUNT; i++) {
                var bufferBuilder = terrainBuilder.getBufferBuilder(i);

                if (bufferBuilder.getVertices() > 0) {
                    this.vertexBuffers[i] = BufferUtil.clone(bufferBuilder.getBuffer());
                }
            }
        }
        else {
            this.vertexBuffers = null;
        }

        if (!drawState.sequentialIndex()) {
            this.indexBuffer = BufferUtil.clone(terrainBuilder.getIndexBuffer());
        }
        else {
            this.indexBuffer = null;
        }
    }

    public int indexCount() {
        return indexCount;
    }

    public ByteBuffer[] getVertexBuffers() {
        return vertexBuffers;
    }

    public ByteBuffer getIndexBuffer() {
        return indexBuffer;
    }

    public void release() {
        if (vertexBuffers != null)
            for (var vertexBuffer : vertexBuffers) {
                if (vertexBuffer != null)
                    MemoryUtil.memFree(vertexBuffer);
            }

        if (indexBuffer != null)
            MemoryUtil.memFree(indexBuffer);
    }
}
