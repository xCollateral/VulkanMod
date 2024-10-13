package net.vulkanmod.render.chunk.cull;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.vulkanmod.render.vertex.format.I32_SNorm;
import org.joml.Vector3f;

public enum QuadFacing {
    X_POS,
    X_NEG,
    Y_POS,
    Y_NEG,
    Z_POS,
    Z_NEG,
    UNDEFINED;

    public static final QuadFacing[] VALUES = QuadFacing.values();
    public static final int COUNT = VALUES.length;

    public static QuadFacing fromDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> Y_NEG;
            case UP -> Y_POS;
            case NORTH -> Z_NEG;
            case SOUTH -> Z_POS;
            case WEST -> X_NEG;
            case EAST -> X_POS;
        };
    }

    public static QuadFacing fromNormal(int packedNormal) {
        final float x = I32_SNorm.unpackX(packedNormal);
        final float y = I32_SNorm.unpackY(packedNormal);
        final float z = I32_SNorm.unpackZ(packedNormal);

        return fromNormal(x, y, z);
    }

    public static QuadFacing fromNormal(Vector3f normal) {
        return fromNormal(normal.x(), normal.y(), normal.z());
    }

    public static QuadFacing fromNormal(float x, float y, float z) {
        final float absX = Math.abs(x);
        final float absY = Math.abs(y);
        final float absZ = Math.abs(z);

        float sum = absX + absY + absZ;

        if (Mth.equal(sum, 1.0f)) {
            if (Mth.equal(absX, 1.0f)) {
                return x > 0.0f ? QuadFacing.X_POS : QuadFacing.X_NEG;
            }

            if (Mth.equal(absY, 1.0f)) {
                return y > 0.0f ? QuadFacing.Y_POS : QuadFacing.Y_NEG;
            }

            if (Mth.equal(absZ, 1.0f)) {
                return z > 0.0f ? QuadFacing.Z_POS : QuadFacing.Z_NEG;
            }
        }

        return QuadFacing.UNDEFINED;
    }
}
