package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTarget.class)
public class FramebufferMixin {

    @Shadow public int viewWidth;

    @Shadow public int viewHeight;

    @Shadow public int width;

    @Shadow public int height;

    /**
     * @author
     */
    @Overwrite
    public void clear(boolean getError) {}

    /**
     * @author
     */
    @Overwrite
    private void _resize(int i, int j, boolean bl) {
        this.viewWidth = i;
        this.viewHeight = j;
        this.width = i;
        this.height = j;
    }

    /**
     * @author
     */
    @Overwrite
    private void _bindWrite(boolean updateViewport) {}

    /**
     * @author
     */
    @Overwrite
    public void unbindWrite() {}

    /**
     * @author
     */
    @Overwrite
    private void _blitToScreen(int width, int height, boolean disableBlend) {}
}
