package net.vulkanmod.render.chunk.cull;

import net.minecraft.core.Direction;

public enum QuadFacing {
    X_POS,
    X_NEG,
    Y_POS,
    Y_NEG,
    Z_POS,
    Z_NEG,
    NONE;

    public static final QuadFacing[] VALUES = QuadFacing.values();
    public static final int COUNT = VALUES.length;

    public static QuadFacing from(Direction direction) {
        return switch (direction) {
            case DOWN -> Y_NEG;
            case UP -> Y_POS;
            case NORTH -> Z_NEG;
            case SOUTH -> Z_POS;
            case WEST -> X_NEG;
            case EAST -> X_POS;
        };
    }
}
