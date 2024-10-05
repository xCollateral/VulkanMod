package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(GL15.class)
public class GL15M {

    /**
     * @author
     */
    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenBuffers() {
        return GlBuffer.glGenBuffers();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void glBindBuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int buffer) {
        GlBuffer.glBindBuffer(target, buffer);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") ByteBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data, usage);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void glBufferData(int i, long l, int j) {
        GlBuffer.glBufferData(i, l, j);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    @NativeType("void *")
    public static ByteBuffer glMapBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int access) {
        return GlBuffer.glMapBuffer(target, access);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int access, long length, @Nullable ByteBuffer old_buffer) {
        return GlBuffer.glMapBuffer(target, access);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glUnmapBuffer(@NativeType("GLenum") int target) {
        return GlBuffer.glUnmapBuffer(target);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void glDeleteBuffers(int i) {
        GlBuffer.glDeleteBuffers(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void glDeleteBuffers(@NativeType("GLuint const *") IntBuffer buffers) {
        GlBuffer.glDeleteBuffers(buffers);
    }
}
