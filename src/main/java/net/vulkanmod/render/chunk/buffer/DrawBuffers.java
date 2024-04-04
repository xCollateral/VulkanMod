package net.vulkanmod.render.chunk.buffer;

import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.EnumMap;

import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {

    private static final int VERTEX_SIZE = PipelineManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;
    private final int index;
    private final Vector3i origin;
    private final int minHeight;

    private boolean allocated = false;
    AreaBuffer indexBuffer;
    private final EnumMap<TerrainRenderType, AreaBuffer> vertexBuffers = new EnumMap<>(TerrainRenderType.class);

    //Need ugly minHeight Parameter to fix custom world heights (exceeding 384 Blocks in total)
    public DrawBuffers(int index, Vector3i origin, int minHeight) {
        this.index = index;
        this.origin = origin;
        this.minHeight = minHeight;
    }

    public void upload(RenderSection section, UploadBuffer buffer, TerrainRenderType renderType) {
        DrawParameters drawParameters = section.getOrAllocDrawParameters(renderType);
        int vertexOffset = drawParameters.vertexOffset;
        int firstIndex = 0;

        final boolean b = !buffer.indexOnly;
        final boolean b1 = !buffer.autoIndices;
        if (b) {
            AreaBuffer.Segment segment = this.getAreaBufferOrAlloc(renderType).upload(buffer.getVertexBuffer(), vertexOffset, drawParameters);
            vertexOffset = segment.offset / VERTEX_SIZE;

            drawParameters.baseInstance = encodeSectionOffset(section.xOffset(), section.yOffset(), section.zOffset());
        }

        if (b1) {
            if (this.indexBuffer == null)
                this.indexBuffer = new AreaBuffer(AreaBuffer.Usage.INDEX, 786432 /*RenderType.SMALL_BUFFER_SIZE*/, INDEX_SIZE);

            AreaBuffer.Segment segment = this.indexBuffer.upload(buffer.getIndexBuffer(), drawParameters.firstIndex, drawParameters);
            firstIndex = segment.offset / INDEX_SIZE;
        }

        drawParameters.indexCount = buffer.indexCount;
        drawParameters.instanceCount = vertexOffset == -1 ? 0 : 1;
        drawParameters.firstIndex = firstIndex;
        drawParameters.vertexOffset = vertexOffset;

        buffer.release();
    }

    private AreaBuffer getAreaBufferOrAlloc(TerrainRenderType renderType) {
        this.allocated = true;

        return this.vertexBuffers.computeIfAbsent(
                renderType, renderType1 -> new AreaBuffer(AreaBuffer.Usage.VERTEX, renderType.initialSize, VERTEX_SIZE));
    }

    public AreaBuffer getAreaBuffer(TerrainRenderType r) {
        return this.vertexBuffers.get(r);
    }

    private boolean hasRenderType(TerrainRenderType r) {
        return this.vertexBuffers.containsKey(r);
    }

    private int encodeSectionOffset(int xOffset, int yOffset, int zOffset) {
        final int xOffset1 = (xOffset & 127);
        final int zOffset1 = (zOffset & 127);
        final int yOffset1 = (yOffset - this.minHeight & 127);
        return yOffset1 << 16 | zOffset1 << 8 | xOffset1;
    }

    private void updateChunkAreaOrigin(VkCommandBuffer commandBuffer, Pipeline pipeline, double camX, double camY, double camZ, MemoryStack stack) {
        float xOffset = (float) (camX - (this.origin.x));
        float yOffset = (float) (camY - (this.origin.y));
        float zOffset = (float) (camZ - (this.origin.z));

        ByteBuffer byteBuffer = stack.malloc(12);

        byteBuffer.putFloat(0, -xOffset);
        byteBuffer.putFloat(4, -yOffset);
        byteBuffer.putFloat(8, -zOffset);

        vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, byteBuffer);
    }

    public void buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, StaticQueue<DrawParameters> queue, TerrainRenderType terrainRenderType) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            ByteBuffer byteBuffer = stack.malloc(20 * queue.size());
            long bufferPtr = MemoryUtil.memAddress0(byteBuffer);

            boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

            int drawCount = 0;
            for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {

                final DrawParameters drawParameters = iterator.next();
                long ptr = bufferPtr + (drawCount * 20L);
                MemoryUtil.memPutInt(ptr, drawParameters.indexCount);
                MemoryUtil.memPutInt(ptr + 4, 1);
                MemoryUtil.memPutInt(ptr + 8, drawParameters.firstIndex == -1 ? 0 : drawParameters.firstIndex);
                MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexOffset);
                MemoryUtil.memPutInt(ptr + 16, drawParameters.baseInstance);

                drawCount++;
            }

            if (drawCount == 0) return;

            indirectBuffer.recordCopyCmd(byteBuffer.position(0));


            vkCmdDrawIndexedIndirect(Renderer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, 20);
        }


    }

    public void buildDrawBatchesDirect(StaticQueue<DrawParameters> queue, TerrainRenderType renderType) {
        boolean isTranslucent = renderType == TerrainRenderType.TRANSLUCENT;
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {
            final DrawParameters drawParameters = iterator.next();

            final int firstIndex = drawParameters.firstIndex == -1 ? 0 : drawParameters.firstIndex;
            vkCmdDrawIndexed(commandBuffer, drawParameters.indexCount, 1, firstIndex, drawParameters.vertexOffset, drawParameters.baseInstance);
        }
    }

    public void bindBuffers(VkCommandBuffer commandBuffer, Pipeline pipeline, TerrainRenderType terrainRenderType, double camX, double camY, double camZ) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var vertexBuffer = getAreaBuffer(terrainRenderType);
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, stack.npointer(vertexBuffer.getId()), stack.npointer(0));
            updateChunkAreaOrigin(commandBuffer, pipeline, camX, camY, camZ, stack);
        }

        if (terrainRenderType == TerrainRenderType.TRANSLUCENT) {
            vkCmdBindIndexBuffer(commandBuffer, this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }

    }

    public void releaseBuffers() {
        if (!this.allocated)
            return;

        this.vertexBuffers.values().forEach(AreaBuffer::freeBuffer);
        this.vertexBuffers.clear();

        if (this.indexBuffer != null)
            this.indexBuffer.freeBuffer();
        this.indexBuffer = null;

        this.allocated = false;
    }

    public boolean isAllocated() {
        return !this.vertexBuffers.isEmpty();
    }

    public EnumMap<TerrainRenderType, AreaBuffer> getVertexBuffers() {
        return vertexBuffers;
    }

    public AreaBuffer getIndexBuffer() {
        return indexBuffer;
    }

    public static class DrawParameters {
        int indexCount = 0;
        int firstIndex = -1;
        int vertexOffset = -1;
        int baseInstance;

        public DrawParameters() {}

        public void reset(ChunkArea chunkArea, TerrainRenderType r) {
            int segmentOffset = vertexOffset * VERTEX_SIZE;
            AreaBuffer areaBuffer = chunkArea.getDrawBuffers().getAreaBuffer(r);
            if (areaBuffer != null && segmentOffset != -1) {
                areaBuffer.setSegmentFree(segmentOffset);
            }

            this.indexCount = 0;
            this.firstIndex = -1;
            this.vertexOffset = -1;
        }
    }

}
