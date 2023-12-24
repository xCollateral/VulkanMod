package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ExtendedRenderTarget;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin implements ExtendedRenderTarget {

    @Shadow public int viewWidth;
    @Shadow public int viewHeight;
    @Shadow public int width;
    @Shadow public int height;

    @Shadow protected int depthBufferId;
    @Shadow protected int colorTextureId;
    @Shadow public int frameBufferId;

    @Shadow @Final private float[] clearChannels;
    @Shadow @Final public boolean useDepth;

    Framebuffer framebuffer;

    boolean needClear = false;
    boolean bound = false;

    private static int boundTarget = 0;

    /**
     * @author
     */
    @Overwrite
    public void clear(boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();

        if(!Renderer.isRecording())
            return;

        if(!bound) {
            needClear = true;
            return;
        }

//        this.bindWrite(true);
        GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
        int i = 16384;
        if (this.useDepth) {
            GlStateManager._clearDepth(1.0);
            i |= 256;
        }

        GlStateManager._clear(i, getError);
//        this.unbindWrite();
        needClear = false;
    }

//    /**
//     * @author
//     */
//    @Overwrite
//    public void destroyBuffers() {
//        RenderSystem.assertOnRenderThreadOrInit();
//        this.unbindRead();
//        this.unbindWrite();
//        if (this.depthBufferId > -1) {
//            TextureUtil.releaseTextureId(this.depthBufferId);
//            this.depthBufferId = -1;
//        }
//
//        if (this.colorTextureId > -1) {
//            TextureUtil.releaseTextureId(this.colorTextureId);
//            this.colorTextureId = -1;
//        }
//
//        if (this.frameBufferId > -1) {
//            GlStateManager._glBindFramebuffer(36160, 0);
//            GlStateManager._glDeleteFramebuffers(this.frameBufferId);
//            this.frameBufferId = -1;
//        }
//
//    }

    /**
     * @author
     */
    @Overwrite
    public void bindRead() {
        RenderSystem.assertOnRenderThread();
//        GlStateManager._bindTexture(this.colorTextureId);

        GlTexture.bindTexture(this.colorTextureId);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GlTexture.getBoundTexture().getVulkanImage()
                    .readOnlyLayout(stack, Renderer.getCommandBuffer());
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void unbindRead() {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.bindTexture(0);
    }

    /**
     * @author
     */
    @Overwrite
    public void bindWrite(boolean bl) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                this._bindWrite(bl);
            });
        } else {
            this._bindWrite(bl);
        }

    }

    /**
     * @author
     */
    @Overwrite
    private void _bindWrite(boolean bl) {
        RenderSystem.assertOnRenderThreadOrInit();

//        if(this.frameBufferId == boundTarget)
//            return;

        GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        if (bl) {
            GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
        }

        this.bound = true;
        boundTarget = this.frameBufferId;
        if(needClear)
            clear(false);
    }

    /**
     * @author
     */
    @Overwrite
    public void unbindWrite() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                GlStateManager._glBindFramebuffer(36160, 0);
                this.bound = false;
                boundTarget = 0;
            });
        } else {
            GlStateManager._glBindFramebuffer(36160, 0);
            this.bound = false;
            boundTarget = 0;
        }
    }

    /**
     * @author
     */
    @Overwrite
    private void _blitToScreen(int width, int height, boolean disableBlend) {
        if(needClear) {
            //If true it means target has not been used
            return;
        }

        Framebuffer framebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId).getFramebuffer();
        VTextureSelector.bindTexture(0, framebuffer.getColorAttachment());

        DrawUtil.blitToScreen();
    }

    @Override
    public boolean isBound() {
        return bound;
    }

    @Override
    public RenderPass getRenderPass() {
        return GlFramebuffer.getFramebuffer(this.frameBufferId).getRenderPass();
    }
}
