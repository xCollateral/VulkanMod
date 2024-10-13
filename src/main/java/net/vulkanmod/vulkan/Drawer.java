package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;

public class Drawer {
    private static final int INITIAL_VB_SIZE = 4000000;
    private static final int INITIAL_IB_SIZE = 1000000;
    private static final int INITIAL_UB_SIZE = 200000;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);

    private int framesNum;
    private VertexBuffer[] vertexBuffers;
    private IndexBuffer[] indexBuffers;

    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer quadsIntIndexBuffer;
    private final AutoIndexBuffer linesIndexBuffer;
    private final AutoIndexBuffer debugLineStripIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer triangleStripIndexBuffer;

    private UniformBuffer[] uniformBuffers;

    private int currentFrame;

    public Drawer() {
        // Index buffers
        this.quadsIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.QUAD_U16_MAX_VERTEX_COUNT, AutoIndexBuffer.DrawType.QUADS);
        this.quadsIntIndexBuffer = new AutoIndexBuffer(100000, AutoIndexBuffer.DrawType.QUADS);
        this.linesIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.LINES);
        this.debugLineStripIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.DEBUG_LINE_STRIP);
        this.triangleFanIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        this.triangleStripIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public void createResources(int framesNum) {
        this.framesNum = framesNum;

        if (this.vertexBuffers != null) {
            Arrays.stream(this.vertexBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.vertexBuffers = new VertexBuffer[framesNum];
        Arrays.setAll(this.vertexBuffers, i -> new VertexBuffer(INITIAL_VB_SIZE, MemoryTypes.HOST_MEM));

        if (this.indexBuffers != null) {
            Arrays.stream(this.indexBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.indexBuffers = new IndexBuffer[framesNum];
        Arrays.setAll(this.indexBuffers, i -> new IndexBuffer(INITIAL_IB_SIZE, MemoryTypes.HOST_MEM));

        if (this.uniformBuffers != null) {
            Arrays.stream(this.uniformBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.uniformBuffers = new UniformBuffer[framesNum];
        Arrays.setAll(this.uniformBuffers, i -> new UniformBuffer(INITIAL_UB_SIZE, MemoryTypes.HOST_MEM));
    }

    public void resetBuffers(int currentFrame) {
        this.vertexBuffers[currentFrame].reset();
        this.indexBuffers[currentFrame].reset();
        this.uniformBuffers[currentFrame].reset();
    }

    public void draw(ByteBuffer vertexData, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount) {
        draw(vertexData, null, mode, vertexFormat, vertexCount);
    }

    public void draw(ByteBuffer vertexData, ByteBuffer indexData, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount) {
        VertexBuffer vertexBuffer = this.vertexBuffers[this.currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, vertexData);

        if (indexData != null) {
            IndexBuffer indexBuffer = this.indexBuffers[this.currentFrame];
            indexBuffer.copyBuffer(indexData);

            int indexCount = vertexCount * 3 / 2;

            drawIndexed(vertexBuffer, indexBuffer, indexCount);
        }
        else {
            AutoIndexBuffer autoIndexBuffer = getAutoIndexBuffer(mode, vertexCount);

            if (autoIndexBuffer != null) {
                int indexCount = autoIndexBuffer.getIndexCount(vertexCount);

                drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
            }
            else {
                draw(vertexBuffer, vertexCount);
            }
        }
    }

    public void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        bindIndexBuffer(commandBuffer, indexBuffer);
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
    }

    public void draw(VertexBuffer vertexBuffer, int vertexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
    }

    public void bindIndexBuffer(VkCommandBuffer commandBuffer, IndexBuffer indexBuffer) {
        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), indexBuffer.indexType.type);
    }

    public void cleanUpResources() {
        Buffer buffer;
        for (int i = 0; i < this.framesNum; ++i) {
            buffer = this.vertexBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            buffer = this.indexBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            buffer = this.uniformBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
        }

        this.quadsIndexBuffer.freeBuffer();
        this.linesIndexBuffer.freeBuffer();
        this.triangleFanIndexBuffer.freeBuffer();
        this.debugLineStripIndexBuffer.freeBuffer();
    }

    public AutoIndexBuffer getQuadsIndexBuffer() {
        return this.quadsIndexBuffer;
    }

    public AutoIndexBuffer getLinesIndexBuffer() {
        return this.linesIndexBuffer;
    }

    public AutoIndexBuffer getTriangleFanIndexBuffer() {
        return this.triangleFanIndexBuffer;
    }

    public AutoIndexBuffer getTriangleStripIndexBuffer() {
        return this.triangleStripIndexBuffer;
    }

    public AutoIndexBuffer getDebugLineStripIndexBuffer() {
        return this.debugLineStripIndexBuffer;
    }

    public UniformBuffer getUniformBuffer() {
        return this.uniformBuffers[this.currentFrame];
    }

    private AutoIndexBuffer getAutoIndexBuffer(VertexFormat.Mode mode, int vertexCount) {
        return switch (mode) {
            case QUADS -> {
                int indexCount = vertexCount * 3 / 2;

                yield indexCount > AutoIndexBuffer.U16_MAX_INDEX_COUNT
                        ? this.quadsIntIndexBuffer : this.quadsIndexBuffer;
            }
            case LINES -> this.linesIndexBuffer;
            case TRIANGLE_FAN -> this.triangleFanIndexBuffer;
            case TRIANGLE_STRIP, LINE_STRIP -> this.triangleStripIndexBuffer;
            case DEBUG_LINE_STRIP -> this.debugLineStripIndexBuffer;
            case TRIANGLES, DEBUG_LINES -> null;
		};
    }
}
