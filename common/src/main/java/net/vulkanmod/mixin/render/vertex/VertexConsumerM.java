package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import net.vulkanmod.render.vertex.VertexUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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

        for (int k = 0; k < j; ++k) {
            float q;
            float p;
            float o;

            float l;
            float n;
            float m;

            int i = k * 8;
            float f = Float.intBitsToFloat(js[i]);
            float g = Float.intBitsToFloat(js[i + 1]);
            float h = Float.intBitsToFloat(js[i + 2]);

            if (useQuadColorData) {
                l = VertexUtil.unpackColorR(js[i + 3]); // equivalent to / 255.0f
                m = VertexUtil.unpackColorG(js[i + 3]);
                n = VertexUtil.unpackColorB(js[i + 3]);
                o = l * brightness[k] * red;
                p = m * brightness[k] * green;
                q = n * brightness[k] * blue;
            } else {
                o = brightness[k] * red;
                p = brightness[k] * green;
                q = brightness[k] * blue;
            }

            int r = lights[k];
            m = Float.intBitsToFloat(js[i + 4]);
            n = Float.intBitsToFloat(js[i + 5]);

            Vector4f vector4f = new Vector4f(f, g, h, 1.0f);
            vector4f.mul(matrix4f);

            this.vertex(vector4f.x(), vector4f.y(), vector4f.z(), o, p, q, 1.0f, m, n, overlay, r, vec3f.x(), vec3f.y(), vec3f.z());
        }

    }
}
