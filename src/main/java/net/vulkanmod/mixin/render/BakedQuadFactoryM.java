package net.vulkanmod.mixin.render;

import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.CubeFace;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

import org.joml.Math;


import org.joml.Matrix2f;
import org.joml.Vector4i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static java.lang.Math.floorMod;
import static org.joml.Math.*;

@Mixin(BakedQuadFactory.class)
public class BakedQuadFactoryM {

    private static final float d = 0.0625f;
    private static final float v = 57.295784f;
    private static final Matrix2f m4f4f2c2 = new Matrix2f();
    private static final float[] uvs = {0,0,0,0};


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
//        float q;
//        float p;
//        float o;
//        float n;
        Matrix4f matrix4f = AffineTransformations.uvLock(rotation, orientation, () -> "Unable to resolve UVLock for model: " + modelId).getMatrix();

        int i2 = (((3 - texture.rotation / 45)) & 2) != 0 ? 2 : 0;
        int i1= (((2 - texture.rotation / 45)) & 2) != 0 ? 2 : 0;
        int f = (int) texture.uvs[i1] >> 4;
        int g = (int) texture.uvs[i1+1] >> 4;
        int j = (int) texture.uvs[i1-1] >> 4;
        int k = (int) texture.uvs[i1-2] >> 4;;
//
        Matrix2f m4f4f2c = new Matrix2f(f, g, j, k);
//        Vector4i vector4f = new Vector4i(f, g, 0, 1);
//        Vector4i vector4f2 = new Vector4i(j, k, 0, 1);
//        Vector4f vector4f1=new Vector4f(vector4f.x, vector4f.y, 0, 1);
//        vector4f1.transform(matrix4f);
//        Vector4f vector4f3=new Vector4f(vector4f2.x, vector4f2.y, 0, 1);
//        vector4f3.transform(matrix4f);


        ;

//        float  n;
//        float  o;
//        float  p;
//        float  q;
//        n = l;
//        o = h;
            m4f4f2c2.transpose();

        float r = Math.toRadians(texture.rotation);
        float sin = sin(r);
        Vec3f vec3f = new Vec3f(cosFromSin(sin, r), sin, 0.0f);
        vec3f.transform(new Matrix3f(matrix4f));
        int mod = round(atan2(vec3f.getY(), vec3f.getX()) * v) & 360;
//        float[] a =new float[4];
        return new ModelElementTexture(m4f4f2c2.get(uvs), mod);
    }
}
