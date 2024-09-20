package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public class VBO {
    private final MemoryType memoryType;
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;

    private int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;

    private boolean autoIndexed = false;

    public VBO(com.mojang.blaze3d.vertex.VertexBuffer.Usage usage) {
       this.memoryType = usage == com.mojang.blaze3d.vertex.VertexBuffer.Usage.STATIC ? MemoryTypes.GPU_MEM : MemoryTypes.HOST_MEM;
    }

    public void upload(MeshData meshData) {
        MeshData.DrawState parameters = meshData.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
        this.mode = parameters.mode();

        this.uploadVertexBuffer(parameters, meshData.vertexBuffer());
        this.uploadIndexBuffer(meshData.indexBuffer());

        meshData.close();
    }

    private void uploadVertexBuffer(MeshData.DrawState parameters, ByteBuffer data) {
        if (data != null) {
            if (this.vertexBuffer != null)
                this.vertexBuffer.freeBuffer();

            this.vertexBuffer = new VertexBuffer(data.remaining(), this.memoryType);
            this.vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);
        }
    }

    public void uploadIndexBuffer(ByteBuffer data) {
        if (data == null) {

            AutoIndexBuffer autoIndexBuffer;
            switch (this.mode) {
                case TRIANGLE_FAN -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleFanIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case TRIANGLE_STRIP, LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleStripIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case QUADS -> {
                    autoIndexBuffer = Renderer.getDrawer().getQuadsIndexBuffer();
                }
                case LINES -> {
                    autoIndexBuffer = Renderer.getDrawer().getLinesIndexBuffer();
                }
                case DEBUG_LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getDebugLineStripIndexBuffer();
                }
                case TRIANGLES, DEBUG_LINES -> {
                    autoIndexBuffer = null;
                }
                default -> throw new IllegalStateException("Unexpected draw mode: %s".formatted(this.mode));
            }

            if (this.indexBuffer != null && !this.autoIndexed)
                this.indexBuffer.freeBuffer();

            if (autoIndexBuffer != null) {
                autoIndexBuffer.checkCapacity(this.vertexCount);
                this.indexBuffer = autoIndexBuffer.getIndexBuffer();
            }

            this.autoIndexed = true;

        } else {
            if (this.indexBuffer != null)
                this.indexBuffer.freeBuffer();

            this.indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            this.indexBuffer.copyBuffer(data);
        }

    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            drawWithShader(MV, P, ((ShaderMixed) shader).getPipeline());

        }
    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, GraphicsPipeline pipeline) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            VRenderSystem.applyMVP(MV, P);

            VRenderSystem.setPrimitiveTopologyGL(this.mode.asGLMode);

            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);

            if (this.indexBuffer != null)
                Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
            else
                Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);

            VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

        }
    }

    public void draw() {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            Renderer renderer = Renderer.getInstance();
            Pipeline pipeline = renderer.getBoundPipeline();
            renderer.uploadAndBindUBOs(pipeline);

            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
        }
    }

    public void close() {
        if (this.vertexCount <= 0)
            return;

        this.vertexBuffer.freeBuffer();
        this.vertexBuffer = null;

        if (!this.autoIndexed) {
            this.indexBuffer.freeBuffer();
            this.indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
    }

}
