package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public class VertexBufferM {



    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(CallbackInfo ci) {}

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenBuffers()I"))
    private int doNothing() {
        return 0;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenVertexArrays()I"))
    private int doNothing2() {
        return 0;
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
    public static void unbind() {}

    /**
     * @author
     */
    @Overwrite
    public void upload(BufferBuilder.RenderedBuffer buffer) {}

    /**
     * @author
     */
    @Overwrite
    public void drawWithShader(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance shader) {}

    /**
     * @author
     */
    @Overwrite
    public void draw() {}

    /**
     * @author
     */
    @Overwrite
    public void close() {}
}
