package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MainTarget.class)
public class MainTargetMixin extends RenderTarget {

    public MainTargetMixin(boolean useDepth) {
        super(useDepth);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void createFrameBuffer(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
        this.width = width;
        this.height = height;
    }

    @Override
    public void bindWrite(boolean updateScissor) {
        Renderer.getInstance().getMainPass().rebindMainTarget();
    }

    @Override
    public void bindRead() {
        Renderer.getInstance().getMainPass().bindAsTexture();
    }

    @Override
    public int getColorTextureId() {
        return Renderer.getInstance().getMainPass().getColorAttachmentGlId();
    }
}
