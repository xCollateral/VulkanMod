package net.vulkanmod.interfaces;

import net.vulkanmod.render.vertex.VertexUtil;

public interface ExtendedVertexBuilder {

    void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal);
}
