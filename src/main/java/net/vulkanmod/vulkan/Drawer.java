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
    private static final int UINT16_INDEX_MAX = 98304;

    private int framesNum;
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
        quadsIndexBuffer = new AutoIndexBuffer(UINT16_INDEX_MAX, AutoIndexBuffer.DrawType.QUADS);
        linesIndexBuffer = new AutoIndexBuffer(UINT16_INDEX_MAX, AutoIndexBuffer.DrawType.LINES);
        triangleFanIndexBuffer = new AutoIndexBuffer(UINT16_INDEX_MAX, AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        stripIndexBuffer = new AutoIndexBuffer(UINT16_INDEX_MAX, AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
        debugLinesIndexBuffer = new AutoIndexBuffer(UINT16_INDEX_MAX, AutoIndexBuffer.DrawType.DEBUG_LINES);
        lineStripIndexBuffer = new AutoIndexBuffer(UINT16_INDEX_MAX, AutoIndexBuffer.DrawType.LINE_STRIP);
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public void createResources(int framesNum) {
        this.framesNum = framesNum;

        if (vertexBuffers != null) {
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

        VertexBuffer vertexBuffer = this.vertexBuffers[currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

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

        autoIndexBuffer.checkCapacity(vertexCount);

        drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
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

    public void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
    }

    public void draw(VertexBuffer vertexBuffer, int vertexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
    }

    public void bindAutoIndexBuffer(VkCommandBuffer commandBuffer, DrawType drawMode) {
        AutoIndexBuffer autoIndexBuffer;
        switch (drawMode) {
            case QUADS -> autoIndexBuffer = this.quadsIndexBuffer;
            case TRIANGLE_FAN -> autoIndexBuffer = this.triangleFanIndexBuffer;
            case LINE_STRIP, TRIANGLE_STRIP -> autoIndexBuffer = this.stripIndexBuffer;
            case DEBUG_LINES -> autoIndexBuffer = this.debugLinesIndexBuffer;
            case LINES -> autoIndexBuffer = this.linesIndexBuffer;
            default -> throw new IllegalArgumentException(String.format("Unexpected value: %s", drawMode));
        }
        IndexBuffer indexBuffer = autoIndexBuffer.getIndexBuffer();

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
    }

    public void cleanUpResources() {
        Buffer buffer;
        for (int i = 0; i < framesNum; ++i) {
            buffer = this.vertexBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            buffer = this.uniformBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

        }

        buffer = this.quadsIndexBuffer.getIndexBuffer();
        MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
        buffer = this.triangleFanIndexBuffer.getIndexBuffer();
        MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
        buffer = this.stripIndexBuffer.getIndexBuffer();
        MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
    }

}
