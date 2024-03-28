package net.vulkanmod.render.chunk.util;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public enum SimpleDirection {
    DOWN(0, 1, -1, new Vec3i(0, -1, 0)),
    UP(1, 0, -1, new Vec3i(0, 1, 0)),
    NORTH(2, 3, 2, new Vec3i(0, 0, -1)),
    SOUTH(3, 2, 0, new Vec3i(0, 0, 1)),
    WEST(4, 5, 1, new Vec3i(-1, 0, 0)),
    EAST(5, 4, 3, new Vec3i(1, 0, 0));

    private static final SimpleDirection[] VALUES = SimpleDirection.values();

    public static SimpleDirection of(Direction direction) {
        return VALUES[direction.get3DDataValue()];
    }

    private final int data3d;
    private final int oppositeIndex;
    private final int data2d;

    public final byte nx, ny, nz;

    SimpleDirection(int j, int k, int l, Vec3i normal) {
        this.data3d = j;
        this.oppositeIndex = k;
        this.data2d = l;

        this.nx = (byte) normal.getX();
        this.ny = (byte) normal.getY();
        this.nz = (byte) normal.getZ();
    }

    public int get3DDataValue() {
        return this.data3d;
    }

    public byte getStepX() {
        return this.nx;
    }

    public byte getStepY() {
        return this.ny;
    }

    public byte getStepZ() {
        return this.nz;
    }
}
