package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.vulkanmod.vulkan.Drawer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;

@Mixin(BufferRenderer.class)
public class BufferRendererM {

    /**
     * @author
     */
    @Overwrite
    public static void unbindAll() {}

    /**
     * @author
     */
    @Overwrite
    private static void draw(ByteBuffer buffer, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, int count, VertexFormat.IntType elementFormat, int vertexCount, boolean textured) {
        RenderSystem.assertOnRenderThread();
        buffer.clear();

        int glMode;
        switch (drawMode) {
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

        Drawer drawer = Drawer.getInstance();
        drawer.draw(buffer, glMode, vertexFormat, count);
    }


}
