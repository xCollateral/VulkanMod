package net.vulkanmod.mixin.render;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Framebuffer.class)
public class FramebufferMixin {

    @Shadow public int textureWidth;
    @Shadow public int textureHeight;
    @Shadow public int viewportWidth;
    @Shadow public int viewportHeight;

    /**
     * @author
     */
    @Overwrite
    public void clear(boolean getError) {}

    /**
     * @author
     */
    @Overwrite
    private void resizeInternal(int width, int height, boolean getError) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        this.textureWidth = width;
        this.textureHeight = height;
    }

    /**
     * @author
     */
    @Overwrite
    private void bind(boolean updateViewport) {}

    /**
     * @author
     */
    @Overwrite
    public void endWrite() {}

    /**
     * @author
     */
    @Overwrite
    private void drawInternal(int width, int height, boolean disableBlend) {}
}
