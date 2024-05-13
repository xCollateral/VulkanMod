package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer.DrawType;
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

    private VertexBuffer[] vertexBuffers;
    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer linesIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer stripIndexBuffer;
    private final AutoIndexBuffer debugLinesIndexBuffer;
    private final AutoIndexBuffer lineStripIndexBuffer;
    private UniformBuffer[] uniformBuffers;

    private int currentFrame;

    public Drawer() {
        //Index buffers
        this.quadsIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.QUADS);
        this.linesIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.LINES);
        this.triangleFanIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        this.stripIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
        this.debugLinesIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.DEBUG_LINES);
        this.lineStripIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.LINE_STRIP);
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

        final AutoIndexBuffer autoIndexBuffer;
        final int indexCount;

        switch (mode) {
            case QUADS -> {
                autoIndexBuffer = this.quadsIndexBuffer;
                indexCount = vertexCount / 4 * 6;
            }
            case LINES -> {
                autoIndexBuffer = this.linesIndexBuffer;
                indexCount = vertexCount / 4 * 6;
            }
            case TRIANGLE_FAN -> {
                autoIndexBuffer = this.triangleFanIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case TRIANGLE_STRIP, LINE_STRIP -> {
                autoIndexBuffer = this.stripIndexBuffer;
                indexCount = vertexCount;
            }
            case DEBUG_LINES -> {
                autoIndexBuffer = this.debugLinesIndexBuffer;
                indexCount = vertexCount;
            }
            case DEBUG_LINE_STRIP -> {
                autoIndexBuffer = this.lineStripIndexBuffer;
                indexCount = vertexCount;
            }
            case TRIANGLES -> {
                draw(vertexBuffer, vertexCount);
                return;
            }
            default -> throw new RuntimeException(String.format("Unknown drawMode: %s", mode));
        }

        this.drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
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

    public AutoIndexBuffer getStripIndexBuffer() {
        return this.stripIndexBuffer;
    }

    public AutoIndexBuffer getDebugLinesIndexBuffer() {
        return this.debugLinesIndexBuffer;
    }

    public AutoIndexBuffer getLineStripIndexBuffer() {
        return this.lineStripIndexBuffer;
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
            case TRIANGLE_FAN -> this.triangleFanIndexBuffer;
            case LINE_STRIP, TRIANGLE_STRIP -> this.stripIndexBuffer;
            case DEBUG_LINES -> this.debugLinesIndexBuffer;
            case LINES -> this.linesIndexBuffer;
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
        this.stripIndexBuffer.freeBuffer();
        this.debugLinesIndexBuffer.freeBuffer();
        this.lineStripIndexBuffer.freeBuffer();
    }
}
