package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface VertexConsumerM {


    @Shadow void addVertex(float f, float g, float h, int i, float j, float k, int l, int m, float n, float o, float p);

    /**
     * @author
     */
    @Overwrite
    default void putBulkData(PoseStack.Pose matrixEntry, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lights, int overlay, boolean useQuadColorData) {
        int[] js = quad.getVertices();
        Vec3i vec3i = quad.getDirection().getNormal();
        Vector3f normal = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
        Matrix4f matrix4f = matrixEntry.pose();
        normal.mul(matrixEntry.normal());

        int j = js.length / 8;

        for (int k = 0; k < j; ++k) {
            float r, g, b;

            float quadR, quadG, quadB;

            int i = k * 8;
            float x = Float.intBitsToFloat(js[i]);
            float y = Float.intBitsToFloat(js[i + 1]);
            float z = Float.intBitsToFloat(js[i + 2]);

            if (useQuadColorData) {
                quadR = ColorUtil.RGBA.unpackR(js[i + 3]);
                quadG = ColorUtil.RGBA.unpackG(js[i + 3]);
                quadB = ColorUtil.RGBA.unpackB(js[i + 3]);
                r = quadR * brightness[k] * red;
                g = quadG * brightness[k] * green;
                b = quadB * brightness[k] * blue;
            } else {
                r = brightness[k] * red;
                g = brightness[k] * green;
                b = brightness[k] * blue;
            }

            int color = ColorUtil.RGBA.pack(r, g, b, alpha);

            int light = lights[k];
            float u = Float.intBitsToFloat(js[i + 4]);
            float v = Float.intBitsToFloat(js[i + 5]);

            Vector4f vector4f = new Vector4f(x, y, z, 1.0f);
            vector4f.mul(matrix4f);

            this.addVertex(vector4f.x(), vector4f.y(), vector4f.z(), color, u, v, overlay, light, normal.x(), normal.y(), normal.z());
        }

    }
}
