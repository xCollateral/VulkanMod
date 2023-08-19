package net.vulkanmod.mixin.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Cube.class)
abstract class ModelPartCubeM implements ModelPartCubeMixed {
    private CubeModel cube;

    @Override
    public CubeModel getCubeModel() {
        if (this.cube == null) {
            this.cube = new CubeModel();
        }
        return this.cube;
    }

    @Override
    @Accessor
    public abstract Polygon[] getPolygons();
}
