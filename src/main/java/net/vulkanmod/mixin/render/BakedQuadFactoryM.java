package net.vulkanmod.mixin.render;

import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.CubeFace;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

import org.joml.Math;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static java.lang.Math.floorMod;
import static org.joml.Math.*;

@Mixin(BakedQuadFactory.class)
public class BakedQuadFactoryM {

    private static final float d = 1.0f / 16.0f;
    private static final float v = 57.295784f;


    /**
     * @author
     */
    @Overwrite
    private float[] getPositionMatrix(Vec3f from, Vec3f to) {
        float[] fs = new float[Direction.values().length];
        fs[CubeFace.DirectionIds.WEST] = from.getX() * d;
        fs[CubeFace.DirectionIds.DOWN] = from.getY() * d;
        fs[CubeFace.DirectionIds.NORTH] = from.getZ() * d;
        fs[CubeFace.DirectionIds.EAST] = to.getX() * d;
        fs[CubeFace.DirectionIds.UP] = to.getY() * d;
        fs[CubeFace.DirectionIds.SOUTH] = to.getZ() * d;
        return fs;
    }

    /**
     * @author
     */
    @Overwrite
    public static ModelElementTexture uvLock(ModelElementTexture texture, Direction orientation, AffineTransformation rotation, Identifier modelId) {
        float q;
        float p;
        float o;
        float n;
        Matrix4f matrix4f = AffineTransformations.uvLock(rotation, orientation, () -> "Unable to resolve UVLock for model: " + modelId).getMatrix();
        float f = texture.getU(texture.getDirectionIndex(0));
        float g = texture.getV(texture.getDirectionIndex(0));
        Vector4f vector4f = new Vector4f(f * d, g * d, 0.0f, 1.0f);
        vector4f.transform(matrix4f);
        float h = 16.0f * vector4f.getX();
        float i = 16.0f * vector4f.getY();
        float j = texture.getU(texture.getDirectionIndex(2));
        float k = texture.getV(texture.getDirectionIndex(2));
        Vector4f vector4f2 = new Vector4f(j * d, k * d, 0.0f, 1.0f);
        vector4f2.transform(matrix4f);
        float l = 16.0f * vector4f2.getX();
        float m = 16.0f * vector4f2.getY();
        if (Math.signum(j - f) == Math.signum(l - h)) {
            n = h;
            o = l;
        } else {
            n = l;
            o = h;
        }
        if (Math.signum(k - g) == Math.signum(m - i)) {
            p = i;
            q = m;
        } else {
            p = m;
            q = i;
        }
        float r = Math.toRadians(texture.rotation);
        float sin = sin(r);
        Vec3f vec3f = new Vec3f(cosFromSin(sin, r), sin, 0.0f);
        vec3f.transform(new Matrix3f(matrix4f));
        int mod = round(atan2(vec3f.getY(), vec3f.getX()) * v) & 360;
        return new ModelElementTexture(new float[]{n, p, o, q}, mod);
    }
}
