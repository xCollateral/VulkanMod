package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer.DrawType;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;

public class Drawer {
    private static final int INITIAL_VB_SIZE = 2000000;
    private static final int INITIAL_UB_SIZE = 200000;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);

    private VertexBuffer[] vertexBuffers;
    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer linesIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer triangleStripIndexBuffer;
    private final AutoIndexBuffer debugLineStripIndexBuffer;
    private final AutoIndexBuffer sequentialIndexBuffer;
    private UniformBuffer[] uniformBuffers;

    private int currentFrame;

    public Drawer() {
        // Index buffers
        this.quadsIndexBuffer = new AutoIndexBuffer(0, 4, 6, (consumer, vertex, index) -> {
            consumer.accept(index + 0, vertex + 0);
            consumer.accept(index + 1, vertex + 1);
            consumer.accept(index + 2, vertex + 2);
            consumer.accept(index + 3, vertex + 2);
            consumer.accept(index + 4, vertex + 3);
            consumer.accept(index + 5, vertex + 0);
        });
        this.linesIndexBuffer = new AutoIndexBuffer(0, 4, 6, (consumer, vertex, index) -> {
            consumer.accept(index + 0, vertex + 0);
            consumer.accept(index + 1, vertex + 1);
            consumer.accept(index + 2, vertex + 2);
            consumer.accept(index + 3, vertex + 3);
            consumer.accept(index + 4, vertex + 2);
            consumer.accept(index + 5, vertex + 1);
        });
        this.triangleFanIndexBuffer = new AutoIndexBuffer(2, 1, 3, (consumer, vertex, index) -> {
            consumer.accept(index + 0, 0);
            consumer.accept(index + 1, vertex + 1);
            consumer.accept(index + 2, vertex + 2);
        });
        this.triangleStripIndexBuffer = new AutoIndexBuffer(2, 1, 3, (consumer, vertex, index) -> {
            consumer.accept(index + 0, vertex + 0);
            consumer.accept(index + 1, vertex + 1);
            consumer.accept(index + 2, vertex + 2);
        });
        this.debugLineStripIndexBuffer = new AutoIndexBuffer(1, 1, 2, (consumer, vertex, index) -> {
            consumer.accept(index + 0, vertex + 0);
            consumer.accept(index + 1, vertex + 1);
        });
        this.sequentialIndexBuffer = new AutoIndexBuffer(0, 1, 1, (consumer, vertex, index) -> {
            consumer.accept(index + 0, vertex + 0);
        });
    }

    public void setCurrentFrame(final int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public void createResources(final int framesNum) {
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

    public void resetBuffers(final int currentFrame) {
        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers[currentFrame].reset();
    }

    public void draw(final ByteBuffer buffer, final VertexFormat.Mode mode, final VertexFormat vertexFormat, final int vertexCount) {
        final VertexBuffer vertexBuffer = this.vertexBuffers[currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        final AutoIndexBuffer autoIndexBuffer = switch (mode) {
            case QUADS -> this.quadsIndexBuffer;
            case LINES -> this.linesIndexBuffer;
            case TRIANGLE_FAN -> this.triangleFanIndexBuffer;
            case LINE_STRIP, TRIANGLE_STRIP -> this.triangleStripIndexBuffer;
            case DEBUG_LINE_STRIP -> this.debugLineStripIndexBuffer;
            case DEBUG_LINES -> this.sequentialIndexBuffer;
            case TRIANGLES -> null;
            default -> throw new RuntimeException(String.format("Unknown mode: %s", mode));
        };

        if (autoIndexBuffer == null) {
            this.draw(vertexBuffer, vertexCount);
        } else {
            final int indexCount = DrawType.fromVertexFormat(mode).indexCount(vertexCount);
            this.drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
        }
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

    public AutoIndexBuffer getSequentialIndexBuffer() {
        return this.sequentialIndexBuffer;
    }

    public UniformBuffer getUniformBuffer() {
        return this.uniformBuffers[currentFrame];
    }

    public void drawIndexed(final VertexBuffer vertexBuffer, final IndexBuffer indexBuffer, final int indexCount) {
        final VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
    }

    public void draw(final VertexBuffer vertexBuffer, final int vertexCount) {
        final VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
    }

    public void bindAutoIndexBuffer(final VkCommandBuffer commandBuffer, final DrawType drawMode) {
        final AutoIndexBuffer autoIndexBuffer = switch (drawMode) {
            case QUADS -> this.quadsIndexBuffer;
            case LINES -> this.linesIndexBuffer;
            case TRIANGLE_FAN -> this.triangleFanIndexBuffer;
            case TRIANGLE_STRIP, LINE_STRIP -> this.triangleStripIndexBuffer;
            case DEBUG_LINE_STRIP -> this.debugLineStripIndexBuffer;
            case DEBUG_LINES -> this.sequentialIndexBuffer;
            default -> throw new IllegalArgumentException(String.format("Unexpected drawMode: %s", drawMode));
        };
        IndexBuffer indexBuffer = autoIndexBuffer.getIndexBuffer();

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
    }

    public void cleanUpResources() {
        if (this.vertexBuffers != null) {
            Arrays.stream(this.vertexBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        if (this.uniformBuffers != null) {
            Arrays.stream(this.uniformBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.quadsIndexBuffer.freeBuffer();
        this.linesIndexBuffer.freeBuffer();
        this.triangleFanIndexBuffer.freeBuffer();
        this.sequentialIndexBuffer.freeBuffer();
        this.debugLineStripIndexBuffer.freeBuffer();
    }
}
