package net.vulkanmod.render.chunk;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.ShaderManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {

    private static final int VERTEX_SIZE = ShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;

    private boolean allocated = false;
    AreaBuffer vertexBuffer;
    AreaBuffer indexBuffer;

    public void allocateBuffers() {
        this.vertexBuffer = new AreaBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 3500000, VERTEX_SIZE);
        this.indexBuffer = new AreaBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 1000000, INDEX_SIZE);

        this.allocated = true;
    }

    public DrawParameters upload(UploadBuffer buffer, DrawParameters drawParameters) {
        int vertexOffset = drawParameters.vertexOffset;
        int firstIndex = 0;

        if(!buffer.indexOnly) {
            this.vertexBuffer.upload(buffer.getVertexBuffer(), drawParameters.vertexBufferSegment);
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

        Renderer.getDrawer().getQuadsIndexBuffer().checkCapacity(buffer.indexCount * 2 / 3);

        buffer.release();

        return drawParameters;
    }

    public int buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, ChunkArea chunkArea, RenderType renderType, double camX, double camY, double camZ) {
        int stride = 20;

        int drawCount = 0;

        ResettableQueue<RenderSection> queue = chunkArea.sectionQueue;

        MemoryStack stack = MemoryStack.stackPush();
        ByteBuffer byteBuffer = stack.calloc(20 * queue.size());
        ByteBuffer uboBuffer = stack.calloc(16 * queue.size());
        long bufferPtr = MemoryUtil.memAddress0(byteBuffer);
        long uboPtr = MemoryUtil.memAddress0(uboBuffer);

        TerrainRenderType terrainRenderType = TerrainRenderType.get(renderType);
        terrainRenderType.setCutoutUniform();
        boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

        Pipeline pipeline = ShaderManager.getInstance().getTerrainIndirectShader(renderType);

        if(isTranslucent) {
            vkCmdBindIndexBuffer(Renderer.getCommandBuffer(), this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }

        var iterator = queue.iterator(isTranslucent);
        while (iterator.hasNext()) {
            RenderSection section = iterator.next();
            DrawParameters drawParameters = section.getDrawParameters(terrainRenderType);

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


            if(drawParameters.indexCount == 0) {
                continue;
            }

            //TODO
            if(!drawParameters.ready && drawParameters.vertexBufferSegment.getOffset() != -1) {
                if(!drawParameters.vertexBufferSegment.isReady())
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
            MemoryUtil.memPutInt(ptr + 16, 0);

            ptr = uboPtr + (drawCount * 16L);
            MemoryUtil.memPutFloat(ptr, (float)((double) section.xOffset - camX));
            MemoryUtil.memPutFloat(ptr + 4, (float)((double) section.yOffset - camY));
            MemoryUtil.memPutFloat(ptr + 8, (float)((double) section.zOffset - camZ));

            drawCount++;
        }

        if(drawCount == 0) {
            MemoryStack.stackPop();
            return 0;
        }


        byteBuffer.position(0);

        indirectBuffer.recordCopyCmd(byteBuffer);

        pipeline.getManualUBO().setSrc(uboPtr, 16 * drawCount);

        LongBuffer pVertexBuffer = stack.longs(vertexBuffer.getId());
        LongBuffer pOffset = stack.longs(0);
        vkCmdBindVertexBuffers(Renderer.getCommandBuffer(), 0, pVertexBuffer, pOffset);

//            pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), WorldRenderer.getInstance().getUniformBuffers(), Drawer.getCurrentFrame());
        pipeline.bindDescriptorSets(Renderer.getCommandBuffer(), Renderer.getCurrentFrame());
        vkCmdDrawIndexedIndirect(Renderer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, stride);

//            fakeIndirectCmd(Drawer.getCommandBuffer(), indirectBuffer, drawCount, uboBuffer);

//        MemoryUtil.memFree(byteBuffer);
        MemoryStack.stackPop();

        return drawCount;
    }

    private static void fakeIndirectCmd(VkCommandBuffer commandBuffer, IndirectBuffer indirectBuffer, int drawCount, ByteBuffer offsetBuffer) {
        Pipeline pipeline = ShaderManager.shaderManager.terrainDirectShader;
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

    public void buildDrawBatchesDirect(ResettableQueue<RenderSection> queue, Pipeline pipeline, RenderType renderType, double camX, double camY, double camZ) {
        TerrainRenderType terrainRenderType = TerrainRenderType.get(renderType);
        terrainRenderType.setCutoutUniform();
        boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pVertexBuffer = stack.longs(vertexBuffer.getId());
            LongBuffer pOffset = stack.longs(0);
            vkCmdBindVertexBuffers(Renderer.getCommandBuffer(), 0, pVertexBuffer, pOffset);

        }

        if(isTranslucent) {
            vkCmdBindIndexBuffer(Renderer.getCommandBuffer(), this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

//        Pipeline pipeline = ShaderManager.shaderManager.terrainDirectShader;
//        Pipeline pipeline = TestShaders.getShaderPipeline(renderType);
//        Drawer.getInstance().bindPipeline(pipeline);
        pipeline.bindDescriptorSets(Renderer.getCommandBuffer(), Renderer.getCurrentFrame());

//        ResettableQueue<RenderSection> queue = chunkArea.sectionQueue;

        int drawCount = 0;
        MemoryStack stack = MemoryStack.stackGet().push();
        ByteBuffer byteBuffer = stack.malloc(24 * queue.size());
        long bufferPtr = MemoryUtil.memAddress0(byteBuffer);

        var iterator = queue.iterator(isTranslucent);
        while (iterator.hasNext()) {
            RenderSection section = iterator.next();
            DrawParameters drawParameters = section.getDrawParameters(terrainRenderType);

            if(drawParameters.indexCount == 0) {
                continue;
            }

            long ptr = bufferPtr + (drawCount * 24L);
            MemoryUtil.memPutInt(ptr, drawParameters.indexCount);
            MemoryUtil.memPutInt(ptr + 4, drawParameters.firstIndex);
            MemoryUtil.memPutInt(ptr + 8, drawParameters.vertexOffset);

            MemoryUtil.memPutFloat(ptr + 12, (float)((double) section.xOffset - camX));
            MemoryUtil.memPutFloat(ptr + 16, (float)((double) section.yOffset - camY));
            MemoryUtil.memPutFloat(ptr + 20, (float)((double) section.zOffset - camZ));

            drawCount++;

        }

        if(drawCount > 0) {
            long offset;
            int indexCount;
            int firstIndex;
            int vertexOffset;
            for(int i = 0; i < drawCount; ++i) {

                offset = i * 24 + bufferPtr;

                indexCount    = MemoryUtil.memGetInt(offset + 0);
                firstIndex    = MemoryUtil.memGetInt(offset + 4);
                vertexOffset  = MemoryUtil.memGetInt(offset + 8);

//                if(indexCount == 0) {
//                    continue;
//                }

                nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, offset + 12);

                vkCmdDrawIndexed(commandBuffer, indexCount, 1, firstIndex, vertexOffset, 0);
            }
        }

        stack.pop();
    }

    public void releaseBuffers() {
        if(!this.allocated)
            return;

        this.vertexBuffer.freeBuffer();
        this.indexBuffer.freeBuffer();

        this.vertexBuffer = null;
        this.indexBuffer = null;
        this.allocated = false;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public static class DrawParameters {
        int indexCount;
        int firstIndex;
        int vertexOffset;
        AreaBuffer.Segment vertexBufferSegment = new AreaBuffer.Segment();
        AreaBuffer.Segment indexBufferSegment;
        boolean ready = false;

        DrawParameters(boolean translucent) {
            if(translucent) {
                indexBufferSegment = new AreaBuffer.Segment();
            }
        }

        public void reset(ChunkArea chunkArea) {
            this.indexCount = 0;
            this.firstIndex = 0;
            this.vertexOffset = 0;

            int segmentOffset = this.vertexBufferSegment.getOffset();
            if(chunkArea != null && chunkArea.drawBuffers.isAllocated() && segmentOffset != -1) {
//                this.chunkArea.drawBuffers.vertexBuffer.setSegmentFree(segmentOffset);
                chunkArea.drawBuffers.vertexBuffer.setSegmentFree(this.vertexBufferSegment);
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
