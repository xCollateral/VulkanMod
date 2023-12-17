package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlTexture;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GL30.class)
public class GL30M {

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glGenerateMipmap(@NativeType("GLenum") int target) {
        GlTexture.generateMipmap(target);
    }

    /**
     * @author
     * @reason
     */
    @NativeType("void")
    @Overwrite(remap = false)
    public static int glGenFramebuffers() {
        return GlFramebuffer.genFramebufferId();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glBindFramebuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer) {
        GlFramebuffer.bindFramebuffer(target, framebuffer);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glFramebufferTexture2D(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture, @NativeType("GLint") int level) {
        GlFramebuffer.framebufferTexture2D(target, attachment, textarget, texture, level);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glFramebufferRenderbuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int renderbuffertarget, @NativeType("GLuint") int renderbuffer) {
//        GL30C.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glDeleteFramebuffers(@NativeType("GLuint const *") int framebuffer) {
        GlFramebuffer.deleteFramebuffer(framebuffer);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    @NativeType("GLenum")
    public static int glCheckFramebufferStatus(@NativeType("GLenum") int target) {
        return GlFramebuffer.glCheckFramebufferStatus(target);
    }

    //RENDER BUFFER

    /**
     * @author
     * @reason
     */
    @NativeType("void")
    @Overwrite(remap = false)
    public static int glGenRenderbuffers() {
        return GlRenderbuffer.genId();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glBindRenderbuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer) {
        GlRenderbuffer.bindRenderbuffer(target, framebuffer);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glRenderbufferStorage(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        GlRenderbuffer.renderbufferStorage(target, internalformat, width, height);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glDeleteRenderbuffers(@NativeType("GLuint const *") int renderbuffer) {
        GlRenderbuffer.deleteRenderbuffer(renderbuffer);
    }
}
