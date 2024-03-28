package net.vulkanmod.mixin.voxel;

import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.vulkanmod.interfaces.VoxelShapeExtended;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VoxelShape.class)
public class VoxelShapeMixin implements VoxelShapeExtended {
    @Shadow @Final public DiscreteVoxelShape shape;

    int co;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initCornerOcclusion(DiscreteVoxelShape discreteVoxelShape, CallbackInfo ci) {
//        shape.isEmpty();

        var disShape = this.shape;

        if(!(disShape instanceof BitSetDiscreteVoxelShape)) {
            this.co = 0;
            return;
        }

        int xSize = Math.max(disShape.getXSize(), 1);
        int ySize = Math.max(disShape.getYSize(), 1);
        int zSize = Math.max(disShape.getZSize(), 1);

        int co = 0;
        int s = 0;
        for (int y1 = 0; y1 <= 1; y1++) {
            for (int z1 = 0; z1 <= 1; z1++) {
                for (int x1 = 0; x1 <= 1; x1++) {
//                    int s = (y1 * 2 + z1) * 2 + x1;

                    final int x2 = x1 * (xSize - 1), y2 = y1 * (ySize - 1), z2 = z1 * (zSize - 1);
                    co |= (disShape.isFull(x2, y2, z2) ? 1 : 0) << s;

                    s++;
                }
            }
        }

        this.co = co;
    }

    public int getCornerOcclusion() {
        return this.co;
    }
}
