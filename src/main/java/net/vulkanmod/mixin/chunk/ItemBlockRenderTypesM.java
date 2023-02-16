package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesM
{

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static RenderType getRenderLayer(FluidState fluidState) {
        return RenderType.translucent();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static RenderType getChunkRenderType(BlockState blockState) {
        return RenderType.translucent();
    }


}
