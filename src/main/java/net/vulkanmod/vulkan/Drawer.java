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
    private static final int INITIAL_VB_SIZE = 2000000;
    private static final int INITIAL_UB_SIZE = 200000;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);

    private int framesNum;
    private VertexBuffer[] vertexBuffers;
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
        this.uniformBuffers[currentFrame].reset();
    }

    public void draw(ByteBuffer buffer, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount) {
        AutoIndexBuffer autoIndexBuffer;
        int indexCount;

        VertexBuffer vertexBuffer = this.vertexBuffers[this.currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        switch (mode) {
            case QUADS -> {
                indexCount = vertexCount * 3 / 2;

                autoIndexBuffer = indexCount > AutoIndexBuffer.U16_MAX_INDEX_COUNT
                        ? this.quadsIntIndexBuffer : this.quadsIndexBuffer;
            }
            case LINES -> {
                autoIndexBuffer = this.linesIndexBuffer;
                indexCount = vertexCount * 3 / 2;
            }
            case TRIANGLE_FAN -> {
                autoIndexBuffer = this.triangleFanIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case TRIANGLE_STRIP, LINE_STRIP -> {
                autoIndexBuffer = this.triangleStripIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case DEBUG_LINE_STRIP -> {
                autoIndexBuffer = this.debugLineStripIndexBuffer;
                indexCount = (vertexCount - 1) * 2;
            }
            case TRIANGLES, DEBUG_LINES -> {
                indexCount = 0;
                autoIndexBuffer = null;
            }
            default -> throw new RuntimeException(String.format("unknown drawMode: %s", mode));
        }

        if (indexCount > 0) {
            autoIndexBuffer.checkCapacity(vertexCount);

            drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
        } else {
            draw(vertexBuffer, vertexCount);
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

}
