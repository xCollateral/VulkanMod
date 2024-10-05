package net.vulkanmod.mixin.render.vertex;

import com.mojang.math.*;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FaceBakery.class)
public class FaceBakeryM {

    private static final float DIV = 1.0f / 16.0f;
    private static final double DIV_2 = 1.0 / 90.0;

    /**
     * @author
     */
    @Overwrite
    private float[] setupShape(Vector3f vector3f, Vector3f vector3f2) {
        float[] fs = new float[Direction.values().length];
        fs[FaceInfo.Constants.MIN_X] = vector3f.x() * DIV;
        fs[FaceInfo.Constants.MIN_Y] = vector3f.y() * DIV;
        fs[FaceInfo.Constants.MIN_Z] = vector3f.z() * DIV;
        fs[FaceInfo.Constants.MAX_X] = vector3f2.x() * DIV;
        fs[FaceInfo.Constants.MAX_Y] = vector3f2.y() * DIV;
        fs[FaceInfo.Constants.MAX_Z] = vector3f2.z() * DIV;
        return fs;
    }

    /**
     * @author
     */
    @Overwrite
    public static BlockFaceUV recomputeUVs(BlockFaceUV blockFaceUV, Direction direction, Transformation transformation) {
        Matrix4f matrix4f = BlockMath.getUVLockTransform(transformation, direction).getMatrix();
        float f = blockFaceUV.getU(blockFaceUV.getReverseIndex(0));
        float g = blockFaceUV.getV(blockFaceUV.getReverseIndex(0));
        Vector4f vector4f = matrix4f.transform(new Vector4f(f * DIV, g * DIV, 0.0F, 1.0F));
        float h = 16.0F * vector4f.x();
        float i = 16.0F * vector4f.y();
        float j = blockFaceUV.getU(blockFaceUV.getReverseIndex(2));
        float k = blockFaceUV.getV(blockFaceUV.getReverseIndex(2));
        Vector4f vector4f2 = matrix4f.transform(new Vector4f(j * DIV, k * DIV, 0.0F, 1.0F));
        float l = 16.0F * vector4f2.x();
        float m = 16.0F * vector4f2.y();
        float n;
        float o;
        if (Math.signum(j - f) == Math.signum(l - h)) {
            n = h;
            o = l;
        } else {
            n = l;
            o = h;
        }

        float p;
        float q;
        if (Math.signum(k - g) == Math.signum(m - i)) {
            p = i;
            q = m;
        } else {
            p = m;
            q = i;
        }

        float r = (float)Math.toRadians(blockFaceUV.rotation);
        Matrix3f matrix3f = new Matrix3f(matrix4f);
        Vector3f vector3f = matrix3f.transform(new Vector3f(Mth.cos(r), Mth.sin(r), 0.0F));
        int s = Math.floorMod(-((int)Math.round(Math.toDegrees(Math.atan2(vector3f.y(), vector3f.x())) * DIV_2)) * 90, 360);
        return new BlockFaceUV(new float[]{n, p, o, q}, s);
    }
}
