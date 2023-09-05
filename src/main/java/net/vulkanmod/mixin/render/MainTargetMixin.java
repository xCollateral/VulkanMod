package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MainTarget.class)
public class MainTargetMixin extends RenderTarget {

    public MainTargetMixin(boolean useDepth) {
        super(useDepth);
    }

    /**
     * @author
     */
    @Overwrite
    private void createFrameBuffer(int width, int height) {

        this.viewWidth = width;
        this.viewHeight = height;
        this.width = width;
        this.height = height;
    }


    public void bindWrite(boolean updateViewport) {
//        Drawer.getInstance().beginRendering(framebuffer);
    }

    public void unbindWrite() {

    }
}
