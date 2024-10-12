package net.vulkanmod.render.model.quad;

import net.minecraft.core.Direction;
import net.vulkanmod.render.chunk.cull.QuadFacing;

public interface ModelQuadView {

    int getFlags();

    float getX(int idx);

    float getY(int idx);

    float getZ(int idx);

    int getColor(int idx);

    float getU(int idx);

    float getV(int idx);

    int getColorIndex();

    Direction getFacingDirection();

    Direction lightFace();

    QuadFacing getQuadFacing();

    int getNormal();

    default boolean isTinted() {
        return this.getColorIndex() != -1;
    }


}
