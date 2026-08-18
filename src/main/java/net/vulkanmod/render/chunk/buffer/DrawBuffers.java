package net.vulkanmod.render.chunk.buffer;

import net.minecraft.util.Mth;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.shader.PipelineManager;
import net.vulkanmod.render.chunk.ChunkAreaManager;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.build.task.CompiledSection;
import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.IndirectBuffer;
import net.vulkanmod.vulkan.memory.buffer.UniformBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.EnumMap;

import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {
    public static final int INDEX_SIZE = Short.BYTES;
    public static final int UNDEFINED_FACING_IDX = QuadFacing.UNDEFINED.ordinal();
    public static final float POS_OFFSET = CustomVertexFormat.getPositionOffset();

    private static final int CMD_STRIDE = 32;

    private static final long cmdBufferPtr = MemoryUtil.nmemAlignedAlloc(CMD_STRIDE, (long) ChunkAreaManager.AREA_SIZE * QuadFacing.COUNT * CMD_STRIDE);

    private final int index;
    public final int vertexSize = PipelineManager.getTerrainVertexFormat().getVertexSize();
    private final Vector3i origin;
    private final int minHeight;

    private boolean allocated = false;
    AreaBuffer indexBuffer;
    private final EnumMap<TerrainRenderType, AreaBuffer> vertexBuffers = new EnumMap<>(TerrainRenderType.class);

    private final UniformBuffer sectionDataBuffer = new UniformBuffer(ChunkAreaManager.AREA_SIZE * 2 * 4, MemoryTypes.HOST_MEM);

    final long drawParamsPtr;
    final int[] sectionIndices = new int[512];
    final int[] masks = new int[512];

    final long[] buildTimes = new long[512];
    long latestBuildTime = 0;
    long lastFadeUpdate = -1;

    // Need ugly minHeight parameter to fix custom world heights (exceeding 384 Blocks in total)
    public DrawBuffers(int index, Vector3i origin, int minHeight) {
        this.index = index;
        this.origin = origin;
        this.minHeight = minHeight;

        this.drawParamsPtr = DrawParametersBuffer.allocateBuffer();
    }

    public void upload(RenderSection section, UploadBuffer buffer, TerrainRenderType renderType) {
        var vertexBuffers = buffer.getVertexBuffers();

        if (buffer.indexOnly) {
            long paramsPtr = DrawParametersBuffer.getParamsPtr(this.drawParamsPtr, section.inAreaIndex, renderType.ordinal(), QuadFacing.UNDEFINED.ordinal());

            int firstIndex = DrawParametersBuffer.getFirstIndex(paramsPtr);
            int indexCount = DrawParametersBuffer.getIndexCount(paramsPtr);

            int oldOffset = indexCount > 0 ? firstIndex : -1;
            AreaBuffer.Segment segment = this.indexBuffer.upload(buffer.getIndexBuffer(), oldOffset, paramsPtr);
            firstIndex = segment.offset / INDEX_SIZE;

            DrawParametersBuffer.setFirstIndex(paramsPtr, firstIndex);

            buffer.release();
            return;
        }

        int oldOffset = -1;
        int size = 0;
        for (int i = 0; i < QuadFacing.COUNT; i++) {
            long paramPtr = DrawParametersBuffer.getParamsPtr(this.drawParamsPtr, section.inAreaIndex, renderType.ordinal(), i);
            int vertexOffset = DrawParametersBuffer.getVertexOffset(paramPtr);

            // Only need to get first used offset, as it identifies the whole segment that will be freed
            if (oldOffset == -1) {
                oldOffset = vertexOffset;
            }

            var vertexBuffer = vertexBuffers[i];
            if (vertexBuffer != null) {
                size += vertexBuffer.remaining();

            }
        }

        AreaBuffer areaBuffer = null;
        AreaBuffer.Segment segment = null;
        boolean doUpload = false;
        if (size > 0) {
            areaBuffer = this.getAreaBufferOrAlloc(renderType);
            areaBuffer.freeSegment(oldOffset);
            segment = areaBuffer.allocateSegment(size);
            doUpload = true;
        }

        int baseInstance = section.inAreaIndex;

        int offset = 0;
        for (int i = 0; i < QuadFacing.COUNT; i++) {
            long paramPtr = DrawParametersBuffer.getParamsPtr(this.drawParamsPtr, section.inAreaIndex, renderType.ordinal(), i);

            int vertexOffset = -1;
            int firstIndex = 0;
            int indexCount = 0;

            var vertexBuffer = vertexBuffers[i];
            int vertexCount = 0;

            if (vertexBuffer != null && doUpload) {
                areaBuffer.upload(segment, vertexBuffer, offset);
                vertexOffset = (segment.offset + offset) / vertexSize;

                offset += vertexBuffer.remaining();
                vertexCount = vertexBuffer.limit() / vertexSize;
                indexCount = vertexCount * 6 / 4;
            }

            if (i == QuadFacing.UNDEFINED.ordinal() && !buffer.autoIndices) {
                if (this.indexBuffer == null) {
                    this.indexBuffer = new AreaBuffer(AreaBuffer.Usage.INDEX, 60000, INDEX_SIZE);
                }

                oldOffset = DrawParametersBuffer.getIndexCount(paramPtr) > 0 ? DrawParametersBuffer.getFirstIndex(paramPtr) : -1;
                AreaBuffer.Segment ibSegment = this.indexBuffer.upload(buffer.getIndexBuffer(), oldOffset, paramPtr);
                firstIndex = ibSegment.offset / INDEX_SIZE;
            } else {
                Renderer.getDrawer().getQuadsIndexBuffer().checkCapacity(vertexCount);
            }

            DrawParametersBuffer.setIndexCount(paramPtr, indexCount);
            DrawParametersBuffer.setFirstIndex(paramPtr, firstIndex);
            DrawParametersBuffer.setVertexOffset(paramPtr, vertexOffset);
            DrawParametersBuffer.setBaseInstance(paramPtr, baseInstance);
        }

        updateUniformData(section);

        buffer.release();
    }

    private void updateUniformData(RenderSection section) {
        int encodedOffset = encodeSectionOffset(section.xOffset(), section.yOffset(), section.zOffset());
        int ptrOffset = section.inAreaIndex * 4;
        MemoryUtil.memPutInt(sectionDataBuffer.getPointer() + ptrOffset, encodedOffset);

        if (section.getCompiledSection() == CompiledSection.UNCOMPILED) {
            long buildTime = System.currentTimeMillis();
            this.buildTimes[section.inAreaIndex] = buildTime;

            if (buildTime > this.latestBuildTime) {
                this.latestBuildTime = buildTime;
            }
        }
    }

    private void updateFadeUniform(long currentTime, int fadeTimeMs, float fadeTimeInv) {
        if (this.lastFadeUpdate < this.latestBuildTime + fadeTimeMs) {
            int ptrOffset = 512 * 4;
            for (int i = 0; i < 512; i++) {
                long delta = currentTime - this.buildTimes[i];
                float fade = fadeTimeMs > 0 ? Mth.clamp(delta * fadeTimeInv, 0.0f, 1.0f) : 1.0f;

                MemoryUtil.memPutFloat(sectionDataBuffer.getPointer() + ptrOffset, fade);
                ptrOffset += 4;
            }

            this.lastFadeUpdate = currentTime;
        }
    }

    private AreaBuffer getAreaBufferOrAlloc(TerrainRenderType renderType) {
        this.allocated = true;

        int initialSize = switch (renderType) {
            case SOLID -> 100000;
            case CUTOUT -> 250000;
            case TRANSLUCENT, TRIPWIRE -> 60000;
        };

        return this.vertexBuffers.computeIfAbsent(
                renderType, renderType1 -> new AreaBuffer(AreaBuffer.Usage.VERTEX, initialSize, vertexSize));
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
        float xOffset = (float) ((this.origin.x) + POS_OFFSET - camX);
        float yOffset = (float) ((this.origin.y) + POS_OFFSET - camY);
        float zOffset = (float) ((this.origin.z) + POS_OFFSET - camZ);

        ByteBuffer byteBuffer = stack.malloc(12);

        byteBuffer.putFloat(0, xOffset);
        byteBuffer.putFloat(4, yOffset);
        byteBuffer.putFloat(8, zOffset);

        vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, byteBuffer);
    }

    public void buildDrawBatchesIndirect(Vector3d cameraPos, IndirectBuffer indirectBuffer, StaticQueue<RenderSection> queue, TerrainRenderType terrainRenderType) {
        long bufferPtr = cmdBufferPtr;

        boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;
        boolean backFaceCulling = Initializer.CONFIG.backFaceCulling && !isTranslucent;

        int drawCount = 0;

        long drawParamsBasePtr = this.drawParamsPtr + (terrainRenderType.ordinal() * DrawParametersBuffer.SECTIONS * DrawParametersBuffer.FACINGS) * DrawParametersBuffer.STRIDE;
        final long facingsStride = DrawParametersBuffer.FACINGS * DrawParametersBuffer.STRIDE;

        int count = 0;
        if (backFaceCulling) {
            for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {
                final RenderSection section = iterator.next();

                sectionIndices[count] = section.inAreaIndex;
                masks[count] = getMask(cameraPos, section);
                count++;
            }

            long ptr = bufferPtr;

            for (int j = 0; j < count; ++j) {
                final int sectionIdx = sectionIndices[j];

                int mask = masks[j];

                long drawParamsBasePtr2 = drawParamsBasePtr + (sectionIdx * facingsStride);

                int indexCount = 0;
                int firstIndex = 0;
                int vertexOffset = 0;
                int baseInstance = 0;

                for (int i = 0; i < QuadFacing.COUNT; i++) {

                    if ((mask & 1 << i) == 0) {
                        drawParamsBasePtr2 += DrawParametersBuffer.STRIDE;

                        // Flush draw cmd
                        if (indexCount > 0) {
                            MemoryUtil.memPutInt(ptr, indexCount);
                            MemoryUtil.memPutInt(ptr + 4, 1);
                            MemoryUtil.memPutInt(ptr + 8, firstIndex);
                            MemoryUtil.memPutInt(ptr + 12, vertexOffset);
                            MemoryUtil.memPutInt(ptr + 16, baseInstance);

                            ptr += CMD_STRIDE;
                            drawCount++;
                        }

                        indexCount = 0;
                        firstIndex = 0;
                        vertexOffset = 0;
                        baseInstance = 0;

                        continue;
                    }

                    long drawParamsPtr = drawParamsBasePtr2;

                    final int indexCount_i = DrawParametersBuffer.getIndexCount(drawParamsPtr);
                    final int firstIndex_i = DrawParametersBuffer.getFirstIndex(drawParamsPtr);
                    final int vertexOffset_i = DrawParametersBuffer.getVertexOffset(drawParamsPtr);
                    final int baseInstance_i = DrawParametersBuffer.getBaseInstance(drawParamsPtr);

                    if (indexCount == 0) {
                        indexCount = indexCount_i;
                        firstIndex = firstIndex_i;
                        vertexOffset = vertexOffset_i;
                        baseInstance = baseInstance_i;
                    }
                    else {
                        indexCount += indexCount_i;
                    }

                    drawParamsBasePtr2 += DrawParametersBuffer.STRIDE;
                }

                if (indexCount > 0) {
                    MemoryUtil.memPutInt(ptr, indexCount);
                    MemoryUtil.memPutInt(ptr + 4, 1);
                    MemoryUtil.memPutInt(ptr + 8, firstIndex);
                    MemoryUtil.memPutInt(ptr + 12, vertexOffset);
                    MemoryUtil.memPutInt(ptr + 16, baseInstance);

                    ptr += CMD_STRIDE;
                    drawCount++;
                }
            }

        }
        else {
            for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {
                final RenderSection section = iterator.next();

                sectionIndices[count] = section.inAreaIndex;
                count++;
            }

            final long facingOffset = UNDEFINED_FACING_IDX * DrawParametersBuffer.STRIDE;
            drawParamsBasePtr += facingOffset;

            long ptr = bufferPtr;
            for (int i = 0; i < count; ++i) {
                int sectionIdx = sectionIndices[i];

                long drawParamsPtr = drawParamsBasePtr + (sectionIdx * facingsStride);

                final int indexCount = DrawParametersBuffer.getIndexCount(drawParamsPtr);
                final int firstIndex = DrawParametersBuffer.getFirstIndex(drawParamsPtr);
                final int vertexOffset = DrawParametersBuffer.getVertexOffset(drawParamsPtr);
                final int baseInstance = DrawParametersBuffer.getBaseInstance(drawParamsPtr);

                if (indexCount <= 0) {
                    continue;
                }

                MemoryUtil.memPutInt(ptr, indexCount);
                MemoryUtil.memPutInt(ptr + 4, 1);
                MemoryUtil.memPutInt(ptr + 8, firstIndex);
                MemoryUtil.memPutInt(ptr + 12, vertexOffset);
                MemoryUtil.memPutInt(ptr + 16, baseInstance);

                ptr += CMD_STRIDE;
                drawCount++;
            }
        }

        if (drawCount == 0) {
            return;
        }

        ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(cmdBufferPtr, queue.size() * QuadFacing.COUNT * CMD_STRIDE);
        indirectBuffer.recordCopyCmd(byteBuffer.position(0));

        vkCmdDrawIndexedIndirect(Renderer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, CMD_STRIDE);
    }

    public void buildDrawBatchesDirect(Vector3d cameraPos, StaticQueue<RenderSection> queue, TerrainRenderType terrainRenderType) {
        boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;
        boolean backFaceCulling = Initializer.CONFIG.backFaceCulling && !isTranslucent;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        long drawParamsBasePtr = this.drawParamsPtr + (terrainRenderType.ordinal() * DrawParametersBuffer.SECTIONS * DrawParametersBuffer.FACINGS) * DrawParametersBuffer.STRIDE;
        final long facingsStride = DrawParametersBuffer.FACINGS * DrawParametersBuffer.STRIDE;

        int count = 0;
        if (backFaceCulling) {
            for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {
                final RenderSection section = iterator.next();

                sectionIndices[count] = section.inAreaIndex;
                masks[count] = getMask(cameraPos, section);
                count++;
            }

            for (int j = 0; j < count; ++j) {
                final int sectionIdx = sectionIndices[j];

                int mask = masks[j];

                long drawParamsBasePtr2 = drawParamsBasePtr + (sectionIdx * facingsStride);

                int indexCount   = 0;
                int firstIndex   = 0;
                int vertexOffset = 0;
                int baseInstance = 0;

                for (int i = 0; i < QuadFacing.COUNT; i++) {

                    if ((mask & 1 << i) == 0) {
                        drawParamsBasePtr2 += DrawParametersBuffer.STRIDE;

                        // Flush draw cmd
                        if (indexCount > 0) {
                            vkCmdDrawIndexed(commandBuffer, indexCount, 1, firstIndex, vertexOffset, baseInstance);
                        }

                        indexCount   = 0;
                        firstIndex   = 0;
                        vertexOffset = 0;
                        baseInstance = 0;

                        continue;
                    }

                    long drawParamsPtr = drawParamsBasePtr2;

                    final int indexCount_i = DrawParametersBuffer.getIndexCount(drawParamsPtr);
                    final int firstIndex_i = DrawParametersBuffer.getFirstIndex(drawParamsPtr);
                    final int vertexOffset_i = DrawParametersBuffer.getVertexOffset(drawParamsPtr);
                    final int baseInstance_i = DrawParametersBuffer.getBaseInstance(drawParamsPtr);

                    if (indexCount == 0) {
                        indexCount   = indexCount_i;
                        firstIndex   = firstIndex_i;
                        vertexOffset = vertexOffset_i;
                        baseInstance = baseInstance_i;
                    }
                    else {
                        indexCount += indexCount_i;
                    }

                    drawParamsBasePtr2 += DrawParametersBuffer.STRIDE;
                }

                if (indexCount > 0) {
                    vkCmdDrawIndexed(commandBuffer, indexCount, 1, firstIndex, vertexOffset, baseInstance);
                }
            }

        }
        else {
            final long facingOffset = UNDEFINED_FACING_IDX * DrawParametersBuffer.STRIDE;
            drawParamsBasePtr += facingOffset;

            for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {
                final RenderSection section = iterator.next();

                sectionIndices[count] = section.inAreaIndex;
                count++;
            }

            for (int i = 0; i < count; ++i) {
                int sectionIdx = sectionIndices[i];

                long drawParamsPtr = drawParamsBasePtr + (sectionIdx * facingsStride);

                final int indexCount = DrawParametersBuffer.getIndexCount(drawParamsPtr);
                final int firstIndex = DrawParametersBuffer.getFirstIndex(drawParamsPtr);
                final int vertexOffset = DrawParametersBuffer.getVertexOffset(drawParamsPtr);
                final int baseInstance = DrawParametersBuffer.getBaseInstance(drawParamsPtr);

                if (indexCount <= 0) {
                    continue;
                }

                vkCmdDrawIndexed(commandBuffer, indexCount, 1, firstIndex, vertexOffset, baseInstance);
            }
        }
    }

    private int getMask(Vector3d camera, RenderSection section) {
        final int secX = section.xOffset;
        final int secY = section.yOffset;
        final int secZ = section.zOffset;

        int mask = 1 << UNDEFINED_FACING_IDX;

        mask |= camera.x - secX >= 0 ? 1 << QuadFacing.X_POS.ordinal() : 0;
        mask |= camera.y - secY >= 0 ? 1 << QuadFacing.Y_POS.ordinal() : 0;
        mask |= camera.z - secZ >= 0 ? 1 << QuadFacing.Z_POS.ordinal() : 0;
        mask |= camera.x - (secX + 16) < 0 ? 1 << QuadFacing.X_NEG.ordinal() : 0;
        mask |= camera.y - (secY + 16) < 0 ? 1 << QuadFacing.Y_NEG.ordinal() : 0;
        mask |= camera.z - (secZ + 16) < 0 ? 1 << QuadFacing.Z_NEG.ordinal() : 0;

        return mask;
    }

    public void bindBuffers(VkCommandBuffer commandBuffer, Pipeline pipeline, TerrainRenderType terrainRenderType,
                            UBO sectionData,
                            double camX, double camY, double camZ,
                            long currentTime, int fadeTimeMs, float fadeTimeInv
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var vertexBuffer = getAreaBuffer(terrainRenderType);
            // VkBuffer is either a 64-bit unsigned integer or a 64-bit pointer, and
            // VkDeviceSize is always an uint64_t. Both are equivalent to a Java "long",
            // so use "stack.nlong" here.
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, stack.nlong(vertexBuffer.getId()), stack.nlong(0));
            updateChunkAreaOrigin(commandBuffer, pipeline, camX, camY, camZ, stack);
        }

        this.updateFadeUniform(currentTime, fadeTimeMs, fadeTimeInv);

        sectionData.getBufferSlice().set(sectionDataBuffer, 0, (int) sectionDataBuffer.getBufferSize());

        if (terrainRenderType == TerrainRenderType.TRANSLUCENT && this.indexBuffer != null) {
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

    public void free() {
        this.releaseBuffers();

        DrawParametersBuffer.freeBuffer(this.drawParamsPtr);
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

    public long getDrawParamsPtr() {
        return drawParamsPtr;
    }

}
