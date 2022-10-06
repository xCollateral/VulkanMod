package net.vulkanmod.mixin.render;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.render.VertexFormat;
import net.vulkanmod.interfaces.VertexFormatMixed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexFormat.class)
public class VertexFormatMixin implements VertexFormatMixed {
    @Shadow private IntList offsets;

    public int getOffset(int i) {
        return offsets.getInt(i);
    }

}
