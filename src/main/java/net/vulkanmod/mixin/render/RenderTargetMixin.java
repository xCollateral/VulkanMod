package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTarget.class)
public class RenderTargetMixin {

    @Shadow public int viewWidth;
    @Shadow public int viewHeight;
    @Shadow public int width;
    @Shadow public int height;

    Framebuffer framebuffer;

    /**
     * @author
     */
    @Overwrite
    public void clear(boolean getError) {}

    /**
     * @author
     */
    @Overwrite
    public void resize(int i, int j, boolean bl) {
        if(this.framebuffer != null) {
            this.framebuffer.cleanUp();
        }

        this.viewWidth = i;
        this.viewHeight = j;
        this.width = i;
        this.height = j;

        //TODO
//        this.framebuffer = new Framebuffer(this.width, this.height, Framebuffer.DEFAULT_FORMAT);
    }

    /**
     * @author
     */
    @Overwrite
    public void bindWrite(boolean updateViewport) {
        Renderer.getInstance().beginRendering(framebuffer);
    }

    /**
     * @author
     */
    @Overwrite
    public void unbindWrite() {

    }

    /**
     * @author
     */
    @Overwrite
    private void _blitToScreen(int width, int height, boolean disableBlend) {
        RenderSystem.depthMask(false);

        DrawUtil.drawFramebuffer(this.framebuffer);

        RenderSystem.depthMask(true);
    }
}
