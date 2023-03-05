package net.vulkanmod.mixin.math;

import com.mojang.math.Vector3f;
import net.vulkanmod.interfaces.math.Vec3Extended;
import org.joml.Math;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vector3f.class)
public class Vertex3fM implements Vec3Extended {

    @Shadow private float x;

    @Shadow private float y;

    @Shadow private float z;

    public Vector3f mulPosition(Matrix4fc mat, Vector3f dest) {
        float x = this.x, y = this.y, z = this.z;
        float xd = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30())));
        float yd = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31())));
        float zd = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32())));
        dest.set(xd, yd, zd);
        return dest;
    }
}
