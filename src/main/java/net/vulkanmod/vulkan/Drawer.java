package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.chunk.AreaUploadManager;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;

public class Drawer {
    private static final int INITIAL_VB_SIZE = 1048576;
    private static final int INITIAL_UB_SIZE = 65536;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);
    private static final int UINT16_INDEX_MAX = 98304;

    private int framesNum;
    private VertexBuffer[] vertexBuffers;
    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer triangleStripIndexBuffer;
    private UniformBuffers uniformBuffers;

    private int currentFrame;

    public Drawer() {
        //Index buffers
        quadsIndexBuffer = new AutoIndexBuffer(UINT16_INDEX_MAX, AutoIndexBuffer.DrawType.QUADS);
        triangleFanIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        triangleStripIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public void createResources(int framesNum) {
        this.framesNum = framesNum;

        if(vertexBuffers != null) {
            Arrays.stream(this.vertexBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.vertexBuffers = new VertexBuffer[framesNum];
        for (int i = 0; i < framesNum; ++i) {
            this.vertexBuffers[i] = new VertexBuffer(INITIAL_VB_SIZE, MemoryTypes.HOST_MEM);
        }

        if(this.uniformBuffers != null)
            this.uniformBuffers.free();
        this.uniformBuffers = new UniformBuffers(INITIAL_UB_SIZE);
    }

    public void resetBuffers(int currentFrame) {
        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers.reset();
    }

    public void draw(ByteBuffer buffer, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount)
    {
        AutoIndexBuffer autoIndexBuffer;
        int indexCount;

        VertexBuffer vertexBuffer = this.vertexBuffers[currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        switch (mode) {
            case QUADS, LINES -> {
                autoIndexBuffer = this.quadsIndexBuffer;
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
            case TRIANGLES -> {
                draw(vertexBuffer, vertexCount);
                return;
            }
            default -> throw new RuntimeException(String.format("unknown drawMode: %s", mode));
        }

        autoIndexBuffer.checkCapacity(vertexCount);

        drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
    }

    public AutoIndexBuffer getQuadsIndexBuffer() {
        return quadsIndexBuffer;
    }

    public AutoIndexBuffer getTriangleFanIndexBuffer() {
        return triangleFanIndexBuffer;
    }

    public UniformBuffers getUniformBuffers() { return this.uniformBuffers; }

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

    public void bindAutoIndexBuffer(VkCommandBuffer commandBuffer, int drawMode) {
        AutoIndexBuffer autoIndexBuffer;
        switch (drawMode) {
            case 7 -> autoIndexBuffer = this.quadsIndexBuffer;
            case 6 -> autoIndexBuffer = this.triangleFanIndexBuffer;
            case 5 -> autoIndexBuffer = this.triangleStripIndexBuffer;
            default -> throw new RuntimeException("unknown drawType");
        }
        IndexBuffer indexBuffer = autoIndexBuffer.getIndexBuffer();

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
    }

    public void cleanUpResources() {
        Buffer buffer;
        for (int i = 0; i < framesNum; ++i) {
            buffer = this.vertexBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            buffer = this.uniformBuffers.getUniformBuffer(i);
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

        }

        buffer = this.quadsIndexBuffer.getIndexBuffer();
        MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
        buffer = this.triangleFanIndexBuffer.getIndexBuffer();
        MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
        buffer = this.triangleStripIndexBuffer.getIndexBuffer();
        MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());
    }

}
