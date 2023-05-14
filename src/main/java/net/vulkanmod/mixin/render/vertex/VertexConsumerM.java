package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(VertexConsumer.class)
public interface VertexConsumerM {

    @Shadow void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

    /**
     * @author
     */
    @Overwrite
    default public void putBulkData(PoseStack.Pose matrixEntry, BakedQuad quad, float[] brightness, float red, float green, float blue, int[] lights, int overlay, boolean useQuadColorData) {
        int[] js = quad.getVertices();
        Vec3i vec3i = quad.getDirection().getNormal();
        Vector3f vec3f = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
        Matrix4f matrix4f = matrixEntry.pose();
        vec3f.mul(matrixEntry.normal());

        int j = js.length / 8;
        try (MemoryStack memoryStack = MemoryStack.stackPush()){
            ByteBuffer byteBuffer = memoryStack.malloc(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP.getVertexSize());
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
                    o = l * brightness[k] * red;
                    p = m * brightness[k] * green;
                    q = n * brightness[k] * blue;
                } else {
                    o = brightness[k] * red;
                    p = brightness[k] * green;
                    q = brightness[k] * blue;
                }

                int r = lights[k];
                m = MemoryUtil.memGetFloat(memAddress + 16);
                n = MemoryUtil.memGetFloat(memAddress + 20);

                Vector4f vector4f = new Vector4f(f, g, h, 1.0f);
                vector4f.mul(matrix4f);

                this.vertex(vector4f.x(), vector4f.y(), vector4f.z(), o, p, q, 1.0f, m, n, overlay, r, vec3f.x(), vec3f.y(), vec3f.z());
            }
        }
    }
}
