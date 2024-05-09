package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;

public class Drawer {
    private static final int INITIAL_VB_SIZE = 1048576;
    public static final int INITIAL_UB_SIZE = 1024;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);

    private int framesNum;
    private VertexBuffer[] vertexBuffers;
    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer triangleStripIndexBuffer;
    private UniformBuffer[] uniformBuffers;

    private int currentFrame;

    private int currentUniformOffset;

    public Drawer() {
        //Index buffers
        quadsIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.QUADS);
        triangleFanIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        triangleStripIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
        this.currentUniformOffset = 0;
    }

    public void createResources(int framesNum) {
        this.framesNum = framesNum;

        if (vertexBuffers != null) {
            Arrays.stream(this.vertexBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.vertexBuffers = new VertexBuffer[framesNum];
        Arrays.setAll(this.vertexBuffers, i -> new VertexBuffer(INITIAL_VB_SIZE, MemoryType.BAR_MEM));

        if (this.uniformBuffers != null) {
            Arrays.stream(this.uniformBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.uniformBuffers = new UniformBuffer[framesNum];
        Arrays.setAll(this.uniformBuffers, i -> new UniformBuffer(INITIAL_UB_SIZE, MemoryType.BAR_MEM));
    }

    public void resetBuffers(int currentFrame) {
        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers[currentFrame].reset();
    }

    public void draw(ByteBuffer buffer, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount, int textureID)
    {
        AutoIndexBuffer autoIndexBuffer;
        int indexCount;

        VertexBuffer vertexBuffer = this.vertexBuffers[currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        switch (mode) {
            case QUADS, LINES, DEBUG_LINES -> {
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

        drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount, textureID);
    }

    public void updateUniformOffset2(int currentUniformOffset1) {


        //get the basealignment/offsets of the Base/Initial Uniform on the DescriptorSet
        //TODO: manage alignment w/ varing offsets/uniforms : may use a uniform block system instead, but unconfirmed if overlapping ranges are problematic otoh
//        final int currentUniformOffset1 = (UniformState.MVP.getOffsetFromHash()/64);

        if(currentUniformOffset1<0) return;

        currentUniformOffset = currentUniformOffset1;


        currentUniformOffset &= 127;
    }
    public void updateUniformOffset() {


        //get the basealignment/offsets of the Base/Initial Uniform on the DescriptorSet
        //TODO: manage alignment w/ varing offsets/uniforms : may use a uniform block system instead, but unconfirmed if overlapping ranges are problematic otoh
        final int currentUniformOffset1 = (UniformState.MVP.getOffsetFromHash()/64);

        if(currentUniformOffset1<0) return;

        currentUniformOffset = currentUniformOffset1;


        currentUniformOffset &= 127;
    }

    public AutoIndexBuffer getQuadsIndexBuffer() {
        return quadsIndexBuffer;
    }

    public AutoIndexBuffer getTriangleFanIndexBuffer() {
        return triangleFanIndexBuffer;
    }

    public UniformBuffer getUniformBuffer() {
        return this.uniformBuffers[currentFrame];
    }

    public void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount, int textureID) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
        final int baseInstance = textureID << 16 | currentUniformOffset;
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, baseInstance);
    }

    public void draw(VertexBuffer vertexBuffer, int vertexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdDraw(commandBuffer, vertexCount, 1, 0, currentUniformOffset);
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

            buffer = this.uniformBuffers[i];
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
