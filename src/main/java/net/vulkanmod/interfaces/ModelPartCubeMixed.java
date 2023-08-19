package net.vulkanmod.interfaces;

import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.vulkanmod.render.model.CubeModel;

public interface ModelPartCubeMixed {

    CubeModel getCubeModel();

    Polygon[] getPolygons();
}
