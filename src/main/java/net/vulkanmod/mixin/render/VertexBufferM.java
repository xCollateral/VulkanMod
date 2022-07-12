package net.vulkanmod.mixin.render;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Shader;
import net.minecraft.util.math.Matrix4f;
import net.vulkanmod.vulkan.VBO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public class VertexBufferM {

    private VBO vbo;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(CallbackInfo ci) {
        vbo = new VBO();
    }

    /**
     * @author
     */
    @Overwrite
    public void bind() {}

    /**
     * @author
     */
    @Overwrite
    private void uploadInternal(BufferBuilder buffer) {
        //TODO
        vbo.upload_(buffer);
    }

    /**
     * @author
     */
    @Overwrite
    public void innerSetShader(Matrix4f viewMatrix, Matrix4f projectionMatrix, Shader shader) {
        vbo._drawWithShader(viewMatrix, projectionMatrix, shader);
    }

    /**
     * @author
     */
    @Overwrite
    public void drawVertices() {
        vbo.drawChunkLayer();
    }

    /**
     * @author
     */
    @Overwrite
    public void close() {
        vbo.close();
    }
}
