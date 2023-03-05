package net.vulkanmod.mixin.model;

import net.minecraft.client.model.geom.ModelPart;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.model.CubeModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cube.class)
public class ModelPartCubeM implements ModelPartCubeMixed {

    CubeModel cube;

    @Inject(method = "<init>", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/model/geom/ModelPart$Cube;polygons:[Lnet/minecraft/client/model/geom/ModelPart$Polygon;",
            ordinal = 0, shift = At.Shift.AFTER))
    private void getVertices(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float q, float r, CallbackInfo ci) {
        CubeModel cube = new CubeModel();
        cube.setVertices(i, j, f, g, h, k, l, m, n, o, p, bl, q, r);
        this.cube = cube;
    }


    @Override
    public CubeModel getCubeModel() {
        return this.cube;
    }
}
