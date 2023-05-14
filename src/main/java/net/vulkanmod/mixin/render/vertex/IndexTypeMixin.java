package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexFormat.IndexType.class)
public class IndexTypeMixin {

    /**
     * @author
     */
    @Overwrite
    public static VertexFormat.IndexType least(int number) {
        return VertexFormat.IndexType.SHORT;
    }
}