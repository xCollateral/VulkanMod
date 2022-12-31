package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.vulkanmod.vulkan.Drawer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BufferUploader.class)
public class BufferRendererM {

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

        int glMode;
        switch (parameters.mode()) {
            case QUADS:
            case LINES:
                glMode = 7;
                break;
            case TRIANGLE_FAN:
                glMode = 6;
                break;
            case TRIANGLE_STRIP:
            case LINE_STRIP:
                glMode = 5;
                break;
            default:
                glMode = 4;
        }

//      Drawer.setModelViewMatrix(RenderSystem.getModelViewMatrix());
//      Drawer.setProjectionMatrix(RenderSystem.getProjectionMatrix());

//        Drawer drawer = Drawer.getInstance();
        Drawer.draw(buffer.vertexBuffer(), glMode, parameters.format(), parameters.vertexCount());
    }


}
