package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.vulkan.VK11.*;

public class GlFramebuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlFramebuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlFramebuffer boundFramebuffer;

    public static int genFramebufferId() {
        int id = ID_COUNTER;
        map.put(id, new GlFramebuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindFramebuffer(int target, int id) {
        // target
        // 36160 GL_FRAMEBUFFER
        // 36161 GL_RENDERBUFFER

//        if(target != GL30.GL_FRAMEBUFFER) {
//            throw new IllegalArgumentException("target is not GL_FRAMEBUFFER");
//        }

        if(boundId == id)
            return;

        if(id == 0) {
            Renderer.getInstance().endRenderPass();

//            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
//            if(renderTarget != null)
//                renderTarget.bindWrite(true);
            boundFramebuffer = null;
            boundId = 0;
            return;
        }

        boundFramebuffer = map.get(id);

        if(boundFramebuffer == null)
            throw new NullPointerException("bound framebuffer is null");

        if(boundFramebuffer.framebuffer != null) {
            if(boundFramebuffer.beginRendering())
                boundId = id;
            else
                boundId = -1;
        }
    }

    public static void deleteFramebuffer(int id) {
        if(id == 0) {
            return;
        }

        boundFramebuffer = map.remove(id);

        if(boundFramebuffer == null)
            throw new NullPointerException("bound framebuffer is null");

        boundFramebuffer.cleanUp();
        boundFramebuffer = null;
    }

    public static void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
//        GL30C.glFramebufferTexture2D(target, attachment, texTarget, texture, level);

        // attachment
        // 36064 attachment0
        // 36096 depth attachment

        // texTarget
        // 3553 texture2D

        if(attachment != GL30.GL_COLOR_ATTACHMENT0 && attachment != GL30.GL_DEPTH_ATTACHMENT) {
            throw new UnsupportedOperationException();
        }
        if(texTarget != GL11.GL_TEXTURE_2D) {
            throw new UnsupportedOperationException();
        }
        if(level != 0) {
            throw new UnsupportedOperationException();
        }

        boundFramebuffer.setAttachmentTexture(attachment, texture);
    }

    public static void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
//        GL30C.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);

//        if(target != GL30.GL_FRAMEBUFFER) {
//            throw new IllegalArgumentException("target is not GL_FRAMEBUFFER");
//        }
//        if(renderbuffertarget != GL30.GL_RENDERBUFFER) {
//            throw new UnsupportedOperationException();
//        }

        boundFramebuffer.setAttachmentRenderbuffer(attachment, renderbuffer);
        //TODO
    }

    public static int glCheckFramebufferStatus(int target) {
        //TODO
        return GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    public static GlFramebuffer getBoundFramebuffer() {
        return boundFramebuffer;
    }

    public static GlFramebuffer getFramebuffer(int id) {
        return map.get(id);
    }

    private final int id;
    Framebuffer framebuffer;
    RenderPass renderPass;

    VulkanImage colorAttachment;
    VulkanImage depthAttachment;

    GlFramebuffer(int i) {
        this.id = i;
    }

    boolean beginRendering() {
        return Renderer.getInstance().beginRendering(this.renderPass, this.framebuffer);
    }

    void setAttachmentTexture(int attachment, int texture) {
        GlTexture glTexture = GlTexture.getTexture(texture);

        if(glTexture == null)
            throw new NullPointerException(String.format("Texture %d is null", texture));

        if(glTexture.vulkanImage == null)
            return;

        switch (attachment) {
            case(GL30.GL_COLOR_ATTACHMENT0) ->
                    this.setColorAttachment(glTexture);

            case(GL30.GL_DEPTH_ATTACHMENT) ->
                    this.setDepthAttachment(glTexture);

            default -> throw new IllegalStateException("Unexpected value: " + attachment);
        }
    }

    void setAttachmentRenderbuffer(int attachment, int texture) {
        GlRenderbuffer renderbuffer = GlRenderbuffer.getRenderbuffer(texture);

        if(renderbuffer == null)
            throw new NullPointerException(String.format("Texture %d is null", texture));

        if(renderbuffer.vulkanImage == null)
            return;

        switch (attachment) {
            case(GL30.GL_COLOR_ATTACHMENT0) ->
                    this.setColorAttachment(renderbuffer);

            case(GL30.GL_DEPTH_ATTACHMENT) ->
                    this.setDepthAttachment(renderbuffer);

            default -> throw new IllegalStateException("Unexpected value: " + attachment);
        }
    }

    void setColorAttachment(GlTexture texture) {
        this.colorAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setDepthAttachment(GlTexture texture) {
        //TODO check if texture is in depth format
        this.depthAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setColorAttachment(GlRenderbuffer texture) {
        this.colorAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setDepthAttachment(GlRenderbuffer texture) {
        //TODO check if texture is in depth format
        this.depthAttachment = texture.vulkanImage;
        createAndBind();
    }

    void createAndBind() {
        //Cannot create without color attachment
        if(this.colorAttachment == null)
            return;

        if(this.framebuffer != null) {
            this.cleanUp();
        }

        boolean hasDepthImage = this.depthAttachment != null;
        VulkanImage depthImage = this.depthAttachment;
//        hasDepthImage = false;
//        VulkanImage depthImage = null;

        this.framebuffer = Framebuffer.builder(this.colorAttachment, depthImage).build();
        RenderPass.Builder builder = RenderPass.builder(this.framebuffer);

        builder.getColorAttachmentInfo()
                .setLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        if(hasDepthImage)
            builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_LOAD_OP_LOAD);

        this.renderPass = builder.build();

        this.beginRendering();
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public RenderPass getRenderPass() {
        return renderPass;
    }

    void cleanUp() {
        this.framebuffer.cleanUp(false);
        this.renderPass.cleanUp();

        this.framebuffer = null;
        this.renderPass = null;
    }
}
