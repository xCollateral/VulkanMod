package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.vulkanmod.vulkan.Drawer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BufferUploader.class)
public class BufferUploaderM {

    /**
     * @author
     */
    @Overwrite
    public static void reset() {}

    /**
     * @author
     */
    @Overwrite
    public static void drawWithShader(BufferBuilder.RenderedBuffer buffer) {
        RenderSystem.assertOnRenderThread();
        buffer.release();

        BufferBuilder.DrawState parameters = buffer.drawState();

        Drawer drawer = Drawer.getInstance();
        drawer.draw(buffer.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
    }

}
