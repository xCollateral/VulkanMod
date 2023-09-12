package net.vulkanmod.mixin.render.vertex;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FaceBakery.class)
public class FaceBakeryM {

    private static final float d = 1.0f / 16.0f;

    /**
     * @author
     */
    @Overwrite
    private float[] setupShape(Vector3f vector3f, Vector3f vector3f2) {
        float[] fs = new float[Direction.values().length];
        fs[FaceInfo.Constants.MIN_X] = vector3f.x() * d;
        fs[FaceInfo.Constants.MIN_Y] = vector3f.y() * d;
        fs[FaceInfo.Constants.MIN_Z] = vector3f.z() * d;
        fs[FaceInfo.Constants.MAX_X] = vector3f2.x()* d;
        fs[FaceInfo.Constants.MAX_Y] = vector3f2.y()* d;
        fs[FaceInfo.Constants.MAX_Z] = vector3f2.z()* d;
        return fs;
    }

    /**
     * @author
     */
    @Overwrite
    public static BlockFaceUV recomputeUVs(BlockFaceUV blockFaceUV, Direction direction, Transformation transformation, ResourceLocation resourceLocation) {
        float q;
        float p;
        float o;
        float n;
        Matrix4f matrix4f = BlockMath.getUVLockTransform(transformation, direction, () -> "Unable to resolve UVLock for model: " + resourceLocation).getMatrix();
        float f = blockFaceUV.getU(blockFaceUV.getReverseIndex(0));
        float g = blockFaceUV.getV(blockFaceUV.getReverseIndex(0));
        Vector4f vector4f = new Vector4f(f * d, g * d, 0.0f, 1.0f);
        vector4f.mul(matrix4f);
        float h = 16.0f * vector4f.x();
        float i = 16.0f * vector4f.y();
        float j = blockFaceUV.getU(blockFaceUV.getReverseIndex(2));
        float k = blockFaceUV.getV(blockFaceUV.getReverseIndex(2));
        Vector4f vector4f2 = new Vector4f(j * d, k * d, 0.0f, 1.0f);
        vector4f2.mul(matrix4f);
        float l = 16.0f * vector4f2.x();
        float m = 16.0f * vector4f2.y();
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
        float r = (float)Math.toRadians(blockFaceUV.rotation);
        Vector3f vec3f = new Vector3f(Mth.cos(r), Mth.sin(r), 0.0f);
        Matrix3f matrix3f = new Matrix3f(matrix4f);
        vec3f.mul(matrix3f);
        int s = Math.floorMod(-((int)Math.round(Math.toDegrees(Math.atan2(vec3f.y(), vec3f.x())) / 90.0)) * 90, 360);
        return new BlockFaceUV(new float[]{n, p, o, q}, s);
    }
}
