package net.vulkanmod.mixin.render;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WindowFramebuffer.class)
public class WindowFramebufferMixin extends Framebuffer {

    public WindowFramebufferMixin(boolean useDepth) {
        super(useDepth);
    }

    /**
     * @author
     */
    @Overwrite
    private void initSize(int width, int height) {

        this.viewportWidth = width;
        this.viewportHeight = height;
        this.textureWidth = width;
        this.textureHeight = height;
    }


}
