package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;

public class GlFramebuffer {

    private static int ID_COUNT = 0;
    private static final Int2ReferenceOpenHashMap<GlFramebuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlFramebuffer boundFramebuffer;
    private static GlFramebuffer boundRenderbuffer;

    public static int genFramebufferId() {
        int id = ID_COUNT;
        map.put(id, new GlFramebuffer(id));
        ID_COUNT++;
        return id;
    }

    public static void bindFramebuffer(int target, int id) {
        // target
        // 36160 GL_FRAMEBUFFER
        // 36161 GL_RENDERBUFFER

        if(target != GL30C.GL_FRAMEBUFFER) {
            throw new IllegalArgumentException("target is not GL_FRAMEBUFFER");
        }

        boundId = id;
        boundFramebuffer = map.get(id);

        if(boundFramebuffer == null)
            throw new NullPointerException("bound framebuffer is null");

        if(boundFramebuffer.framebuffer != null)
            boundFramebuffer.beginRendering();
    }

    public static void glFramebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
//        GL30C.glFramebufferTexture2D(target, attachment, texTarget, texture, level);

        // attachment
        // 36064 attachment0
        // 36096 depth attachment

        // texTarget
        // 3553 texture2D

        if(attachment != GL30C.GL_COLOR_ATTACHMENT0 && attachment != GL30C.GL_DEPTH_ATTACHMENT) {
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

    public static void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
//        GL30C.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);

        if(target != GL30C.GL_FRAMEBUFFER) {
            throw new IllegalArgumentException("target is not GL_FRAMEBUFFER");
        }
        if(renderbuffertarget != GL30C.GL_RENDERBUFFER) {
            throw new UnsupportedOperationException();
        }

//        boundFramebuffer.setAttachmentTexture(attachment, texture);
        //TODO
    }

    public static void bindRenderbuffer(int target, int id) {
        // target
        // 36160 GL_FRAMEBUFFER
        // 36161 GL_RENDERBUFFER

        if(target != GL30C.GL_RENDERBUFFER) {
            throw new IllegalArgumentException("target is not GL_RENDERBUFFER");
        }

        boundRenderbuffer = map.get(id);

        if(boundRenderbuffer == null)
            throw new NullPointerException("bound renderbuffer is null");
    }

    public static void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        //TODO
//        GL30C.glRenderbufferStorage(target, internalformat, width, height);
    }

    public static int glCheckFramebufferStatus(int target) {
        //TODO
        return GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    private final int id;
    Framebuffer framebuffer;
    GlTexture colorAttachment;
    GlTexture depthAttachment;

    GlFramebuffer(int i) {
        this.id = i;
    }

    void beginRendering() {
        Renderer.getInstance().beginRendering(this.framebuffer);
    }

    void setAttachmentTexture(int attachment, int texture) {
        GlTexture glTexture = GlTexture.getTexture(texture);
        Validate.notNull(glTexture);

        if(glTexture.vulkanImage == null)
            return;

        switch (attachment) {
            case(GL30C.GL_COLOR_ATTACHMENT0) ->
                    this.setColorAttachment(glTexture);

            case(GL30C.GL_DEPTH_ATTACHMENT) ->
                    this.depthAttachment = glTexture;

            default -> throw new IllegalStateException("Unexpected value: " + attachment);
        }
    }

    void setColorAttachment(GlTexture texture) {
        this.colorAttachment = texture;

        if(this.framebuffer == null) {
            this.framebuffer = new Framebuffer(this.colorAttachment.vulkanImage);
        }

        this.beginRendering();
    }
}
