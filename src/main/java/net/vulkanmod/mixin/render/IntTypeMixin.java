package net.vulkanmod.mixin.render;

import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexFormat.IntType.class)
public class IntTypeMixin {

    /**
     * @author
     */
    @Overwrite
    public static VertexFormat.IntType getSmallestTypeFor(int number) {
        return VertexFormat.IntType.SHORT;
    }
}