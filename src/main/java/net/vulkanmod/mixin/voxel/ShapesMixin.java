package net.vulkanmod.mixin.voxel;

import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Shapes.class)
public class ShapesMixin {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static int findBits(double d, double e) {
        if (!(d < -1.0E-7) && !(e > 1.0000001)) {
            for(int i = 0; i <= 4; ++i) {
                int j = 1 << i;
                double f = d * (double)j;
                double g = e * (double)j;
                boolean bl = Math.abs(f - (double)Math.round(f)) < 1.0E-7 * (double)j;
                boolean bl2 = Math.abs(g - (double)Math.round(g)) < 1.0E-7 * (double)j;
                if (bl && bl2) {
                    return i;
                }
            }

        }
        return -1;
    }
}
