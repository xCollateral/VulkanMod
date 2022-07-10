package net.vulkanmod.mixin.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(VertexConsumer.class)
public interface VertexConsumerM {

    @Shadow void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

    /**
     * @author
     */
    @Overwrite
    default public void quad(MatrixStack.Entry matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, int[] lights, int overlay, boolean useQuadColorData) {
        float[] fs = new float[]{brightnesses[0], brightnesses[1], brightnesses[2], brightnesses[3]};
        int[] is = new int[]{lights[0], lights[1], lights[2], lights[3]};
        int[] js = quad.getVertexData();
        Vec3i vec3i = quad.getFace().getVector();
        Vec3f vec3f = new Vec3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
        Matrix4f matrix4f = matrixEntry.getPositionMatrix();
        vec3f.transform(matrixEntry.getNormalMatrix());

        int j = js.length / 8;
        try (MemoryStack memoryStack = MemoryStack.stackPush()){
            ByteBuffer byteBuffer = memoryStack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getVertexSize());
            long memAddress = MemoryUtil.memAddress0(byteBuffer);

            for (int k = 0; k < j; ++k) {
                float q;
                float p;
                float o;
                float n;
                float m;

                long idx = 0;
                for(int i = k * 8; i < k * 8 + 8; ++i) {
                    MemoryUtil.memPutInt(memAddress + idx, js[i]);
                    idx += 4L;
                }

                float f = MemoryUtil.memGetFloat(memAddress);
                float g = MemoryUtil.memGetFloat(memAddress + 4);
                float h = MemoryUtil.memGetFloat(memAddress + 8);

                if (useQuadColorData) {
                    float l = (float)(MemoryUtil.memGetByte(memAddress + 12) & 0xFF) * 0.003921568F; // equivalent to / 255.0f
                    m = (float)(MemoryUtil.memGetByte(memAddress + 13) & 0xFF) * 0.003921568F;
                    n = (float)(MemoryUtil.memGetByte(memAddress + 14) & 0xFF) * 0.003921568F;
                    o = l * fs[k] * red;
                    p = m * fs[k] * green;
                    q = n * fs[k] * blue;
                } else {
                    o = fs[k] * red;
                    p = fs[k] * green;
                    q = fs[k] * blue;
                }

                int r = is[k];
                m = MemoryUtil.memGetFloat(memAddress + 16);
                n = MemoryUtil.memGetFloat(memAddress + 20);

                Vector4f vector4f = new Vector4f(f, g, h, 1.0f);
                vector4f.transform(matrix4f);

                this.vertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), o, p, q, 1.0f, m, n, overlay, r, vec3f.getX(), vec3f.getY(), vec3f.getZ());
            }
        }
    }
}
