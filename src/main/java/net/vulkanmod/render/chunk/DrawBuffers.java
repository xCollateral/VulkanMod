package net.vulkanmod.render.chunk;

import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.EnumMap;

import static net.vulkanmod.render.vertex.TerrainRenderType.TRANSLUCENT;
import static net.vulkanmod.render.vertex.TerrainRenderType.getActiveLayers;
import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {

    private static final int VERTEX_SIZE = TerrainShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;
    public final int index;
    private final Vector3i origin;

    private boolean allocated = false;
    AreaBuffer indexBuffer;

    private final EnumMap<TerrainRenderType, AreaBuffer> areaBufferTypes = new EnumMap<>(TerrainRenderType.class);

    //Help JIT optimisations by hardcoding the queue size to the max possible ChunkArea limit
//    final StaticQueue<DrawParameters> sectionQueue = new StaticQueue<>(512);
    private final EnumMap<TerrainRenderType, StaticQueue<DrawParameters>> sectionQueues = new EnumMap<>(TerrainRenderType.class);

    public DrawBuffers(int index, Vector3i origin) {

        this.index = index;
        this.origin = origin;
        getActiveLayers().forEach(t -> sectionQueues.put(t, new StaticQueue<>(512)));
    }

    public void allocateBuffers() {
//        getActiveLayers().forEach(t -> areaBufferTypes.put(t, new AreaBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, t.initialSize, VERTEX_SIZE)));
//        this.indexBuffer = new AreaBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 1000000, INDEX_SIZE);

        this.allocated = true;
    }

    public DrawParameters upload(int xOffset, int yOffset, int zOffset, UploadBuffer buffer, DrawParameters drawParameters, TerrainRenderType r) {
        int vertexOffset = drawParameters.vertexOffset;
        int firstIndex = 0;
        drawParameters.baseInstance = encodeSectionOffset(xOffset, yOffset, zOffset);

        if(!buffer.indexOnly) {
            getAreaBufferCheckedAlloc(r).upload(buffer.getVertexBuffer(), drawParameters.vertexBufferSegment);
//            drawParameters.vertexOffset = drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE;
            vertexOffset = drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE;

            //debug
//            if(drawParameters.vertexBufferSegment.getOffset() % VERTEX_SIZE != 0) {
//                throw new RuntimeException("misaligned vertex buffer");
//            }
        }

        if(!buffer.autoIndices) {
            this.indexBuffer.upload(buffer.getIndexBuffer(), drawParameters.indexBufferSegment);
//            drawParameters.firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
            firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
        }

//        AreaUploadManager.INSTANCE.enqueueParameterUpdate(
//                new ParametersUpdate(drawParameters, buffer.indexCount, firstIndex, vertexOffset));

        drawParameters.indexCount = buffer.indexCount;
        drawParameters.firstIndex = firstIndex;
        drawParameters.vertexOffset = vertexOffset;



        buffer.release();

        return drawParameters;
    }

    private AreaBuffer getAreaBufferCheckedAlloc(TerrainRenderType r) {
        if(!this.areaBufferTypes.containsKey(r))
        {
            if(r==TRANSLUCENT) this.indexBuffer=new AreaBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, r.initialSize, INDEX_SIZE);
            this.areaBufferTypes.put(r, new AreaBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, r.initialSize, VERTEX_SIZE));
        }
        return this.areaBufferTypes.get(r);
    }
    private AreaBuffer getAreaBuffer(TerrainRenderType r) {
        return this.areaBufferTypes.get(r);
    }

    private boolean hasRenderType(TerrainRenderType r) {
        return this.areaBufferTypes.containsKey(r);
    }

    private static int encodeSectionOffset(int xOffset, int yOffset, int zOffset) {
        final int xOffset1 = (xOffset & 127);
        final int zOffset1 = (zOffset & 127);
        return yOffset << 18 | zOffset1 << 9 | xOffset1;
    }

    private void updateChunkAreaOrigin(double camX, double camY, double camZ, VkCommandBuffer commandBuffer, long ptr) {
        VUtil.UNSAFE.putFloat(ptr + 0, (float) (this.origin.x - camX));
        VUtil.UNSAFE.putFloat(ptr + 4, (float) -camY);
        VUtil.UNSAFE.putFloat(ptr + 8, (float) (this.origin.z - camZ));

        nvkCmdPushConstants(commandBuffer, TerrainShaderManager.terrainShader.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, ptr);
    }
    public void buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, TerrainRenderType terrainRenderType, double camX, double camY, double camZ) {
        int stride = 20;

        final StaticQueue<DrawParameters> sectionQueue = this.sectionQueues.get(terrainRenderType);

        if(!hasRenderType(terrainRenderType)) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = stack.calloc(20 * sectionQueue.size());
            long bufferPtr = MemoryUtil.memAddress0(byteBuffer);


            boolean isTranslucent = terrainRenderType == TRANSLUCENT;

            VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
            if (isTranslucent) {
                vkCmdBindIndexBuffer(commandBuffer, this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
            }


            int drawCount = 0;
            for (var iterator = sectionQueue.iterator(isTranslucent); iterator.hasNext(); ) {

                DrawParameters drawParameters = iterator.next();

                //Debug
    //            BlockPos o = section.origin;
    ////            BlockPos pos = new BlockPos(-2188, 65, -1674);
    //
    ////            Vec3 cameraPos = WorldRenderer.getCameraPos();
    //            BlockPos pos = new BlockPos(Minecraft.getInstance().getCameraEntity().blockPosition());
    //            if(o.getX() <= pos.getX() && o.getY() <= pos.getY() && o.getZ() <= pos.getZ() &&
    //                    o.getX() + 16 >= pos.getX() && o.getY() + 16 >= pos.getY() && o.getZ() + 16 >= pos.getZ()) {
    //                System.nanoTime();
    //
    //                }
    //
    //            }


                //TODO
                if (!drawParameters.ready && drawParameters.vertexBufferSegment.getOffset() != -1) {
                    if (!drawParameters.vertexBufferSegment.isReady())
                        continue;
                    drawParameters.ready = true;
                }

                long ptr = bufferPtr + (drawCount * 20L);
                MemoryUtil.memPutInt(ptr, drawParameters.indexCount);
                MemoryUtil.memPutInt(ptr + 4, 1);
                MemoryUtil.memPutInt(ptr + 8, drawParameters.firstIndex);
    //            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE);
                MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexOffset);
    //            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexBufferSegment.getOffset());
                MemoryUtil.memPutInt(ptr + 16, drawParameters.baseInstance);



                drawCount++;
            }

            if(drawCount == 0) {
                return;
            }

//            if(drawCount!= size) Initializer.LOGGER.warn(drawCount+"-->"+ size);

            byteBuffer.position(0);

            indirectBuffer.recordCopyCmd(byteBuffer);


            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, stack.npointer(getAreaBuffer(terrainRenderType).getId()), stack.npointer(0));

//            pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), WorldRenderer.getInstance().getUniformBuffers(), Drawer.getCurrentFrame());
            updateChunkAreaOrigin(camX, camY, camZ, commandBuffer, stack.nmalloc(16));
            vkCmdDrawIndexedIndirect(commandBuffer, indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, stride);
        }

//            fakeIndirectCmd(Drawer.getCommandBuffer(), indirectBuffer, drawCount, uboBuffer);

//        MemoryUtil.memFree(byteBuffer);


    }

    private static void fakeIndirectCmd(VkCommandBuffer commandBuffer, IndirectBuffer indirectBuffer, int drawCount, ByteBuffer offsetBuffer) {
        Pipeline pipeline = TerrainShaderManager.terrainShader;
//        Drawer.getInstance().bindPipeline(pipeline);
        pipeline.bindDescriptorSets(Renderer.getCommandBuffer(), Renderer.getCurrentFrame());
//        pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), WorldRenderer.getInstance().getUniformBuffers(), Drawer.getCurrentFrame());

        ByteBuffer buffer = indirectBuffer.getByteBuffer();
        long address = MemoryUtil.memAddress0(buffer);
        long offsetAddress = MemoryUtil.memAddress0(offsetBuffer);
        int baseOffset = (int) indirectBuffer.getOffset();
        long offset;
        int stride = 20;

        int indexCount;
        int instanceCount;
        int firstIndex;
        int vertexOffset;
        int firstInstance;
        for(int i = 0; i < drawCount; ++i) {
            offset = i * stride + baseOffset + address;

            indexCount    = MemoryUtil.memGetInt(offset + 0);
            instanceCount = MemoryUtil.memGetInt(offset + 4);
            firstIndex    = MemoryUtil.memGetInt(offset + 8);
            vertexOffset  = MemoryUtil.memGetInt(offset + 12);
            firstInstance = MemoryUtil.memGetInt(offset + 16);


            long uboOffset = i * 16 + offsetAddress;

            nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, uboOffset);

            vkCmdDrawIndexed(commandBuffer, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
        }
    }

    public void buildDrawBatchesDirect(TerrainRenderType terrainRenderType, double camX, double camY, double camZ) {
        if(!this.hasRenderType(terrainRenderType)) return;
        boolean isTranslucent = terrainRenderType == TRANSLUCENT;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, stack.npointer(getAreaBuffer(terrainRenderType).getId()), stack.npointer(0));
            updateChunkAreaOrigin(camX, camY, camZ, commandBuffer, stack.nmalloc(16));
        }

        if(isTranslucent) {
            vkCmdBindIndexBuffer(commandBuffer, this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }

        for (var iterator = this.sectionQueues.get(terrainRenderType).iterator(isTranslucent); iterator.hasNext(); ) {
            final DrawParameters drawParameters = iterator.next();
            vkCmdDrawIndexed(commandBuffer, drawParameters.indexCount, 1, drawParameters.firstIndex, drawParameters.vertexOffset, drawParameters.baseInstance);

        }


    }

    public void releaseBuffers() {
        if(!this.allocated)
            return;

        this.areaBufferTypes.values().forEach(AreaBuffer::freeBuffer);
        if(this.areaBufferTypes.containsKey(TRANSLUCENT)) this.indexBuffer.freeBuffer();
        this.areaBufferTypes.clear();


        this.indexBuffer = null;
        this.allocated = false;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public void addMeshlet(TerrainRenderType r, DrawParameters drawParameters) {
        this.sectionQueues.get(r).add(drawParameters);
    }

    public void clear() {
        this.sectionQueues.values().forEach(StaticQueue::clear);
    }
    
//    public void clear(TerrainRenderType r) {
//        this.sectionQueues.get(r).clear();
//    }

    public static class DrawParameters {
        int indexCount;
        int firstIndex;
        int vertexOffset;
        int baseInstance;
        final AreaBuffer.Segment vertexBufferSegment = new AreaBuffer.Segment();
        final AreaBuffer.Segment indexBufferSegment;
        boolean ready = false;

        DrawParameters(boolean translucent) {
            indexBufferSegment = translucent ? new AreaBuffer.Segment() : null;
        }

        public void reset(ChunkArea chunkArea, TerrainRenderType r) {
            this.indexCount = 0;
            this.firstIndex = 0;
            this.vertexOffset = 0;

            int segmentOffset = this.vertexBufferSegment.getOffset();
            if(chunkArea != null && chunkArea.drawBuffers.hasRenderType(r) && segmentOffset != -1) {
//                this.chunkArea.drawBuffers.vertexBuffer.setSegmentFree(segmentOffset);
                chunkArea.drawBuffers.getAreaBuffer(r).setSegmentFree(this.vertexBufferSegment);
                if(r==TRANSLUCENT||this.indexBufferSegment!=null) chunkArea.drawBuffers.indexBuffer.setSegmentFree(this.indexBufferSegment);
            }
        }
    }

    public record ParametersUpdate(DrawParameters drawParameters, int indexCount, int firstIndex, int vertexOffset) {

        public void setDrawParameters() {
            this.drawParameters.indexCount = indexCount;
            this.drawParameters.firstIndex = firstIndex;
            this.drawParameters.vertexOffset = vertexOffset;
            this.drawParameters.ready = true;
        }
    }

}
