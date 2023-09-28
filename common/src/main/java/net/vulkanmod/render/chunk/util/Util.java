package net.vulkanmod.render.chunk.util;

import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Util {

    public static final Direction[] DIRECTIONS = Direction.values();
    public static final Direction[] XZ_DIRECTIONS = getXzDirections();

    private static Direction[] getXzDirections() {
        Direction[] directions = new Direction[4];

        int i = 0;
        for(Direction direction : Direction.values()) {
            if(direction.getAxis() == Direction.Axis.X || direction.getAxis() == Direction.Axis.Z) {
                directions[i] = direction;
                ++i;
            }
        }
        return directions;
    }

    public static long posLongHash(int x, int y, int z) {
        return (long)x & 0x00000000FFFFL | ((long) z << 16) & 0x0000FFFF0000L | ((long) y << 32) & 0xFFFF00000000L;
    }

    public static int flooredLog(int v) {
        assert v > 0;
        int log = 30;
        int t = 0x40000000;

        while((v & t) == 0) {
            t >>= 1;
            log--;
        }

        return log;
    }

    public static int align(int i, int alignment) {
        int r = i % alignment;
        return r != 0 ? i + alignment - r : i;
    }

    public static ByteBuffer createCopy(ByteBuffer src) {
        ByteBuffer ret = MemoryUtil.memAlloc(src.remaining());
        MemoryUtil.memCopy(src, ret);
        return ret;
    }
}
