package net.vulkanmod.interfaces;

public interface ExtendedVertexBuilder {

    void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal);

    //Particles
    default void vertex(float x, float y, float z, float u, float v, int packedColor, int light) {}
}
