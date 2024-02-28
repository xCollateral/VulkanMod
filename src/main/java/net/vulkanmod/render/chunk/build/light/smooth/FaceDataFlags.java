package net.vulkanmod.render.chunk.build.light.smooth;

/**
 * Bit flags to indicate which light properties have been computed for a given face.
 */
abstract class FaceDataFlags {
    /**
     * The light data has been retrieved from the cache.
     */
    public static final int HAS_LIGHT_DATA = 0b01;

    /**
     * The light data has been unpacked into normalized floating point values.
     */
    public static final int HAS_UNPACKED_LIGHT_DATA = 0b10;
}
