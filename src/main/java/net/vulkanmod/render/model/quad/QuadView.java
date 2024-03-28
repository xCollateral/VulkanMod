package net.vulkanmod.render.model.quad;

import net.minecraft.core.Direction;

public interface QuadView {

    int getFlags();

    float getX(int idx);

    float getY(int idx);

    float getZ(int idx);

    int getColor(int idx);

    float getU(int idx);

    float getV(int idx);

    int getColorIndex();

    Direction getFacingDirection();

    default boolean isTinted() {
        return this.getColorIndex() != -1;
    }


}
