package net.vulkanmod.fabric.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(LiquidBlockRenderer.class)
public class FabricLiquidBlockRendererM {

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void vertex(VertexConsumer vertexConsumer, double d, double e, double f, float g, float h, float i, float j, float k, int l) {
//        vertexConsumer.vertex(d, e, f).color(g, h, i, 1.0F).uv(j, k).uv2(l).normal(0.0F, 1.0F, 0.0F).endVertex();
        vertexConsumer.vertex((float) d, (float) e, (float) f, g, h, i, 1.0f, j, k, 0, l, 0.0F, 1.0F, 0.0F);
    }
}
