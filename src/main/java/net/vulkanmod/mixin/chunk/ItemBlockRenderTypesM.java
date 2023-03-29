package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesM
{
    @Shadow @Final private static Map<Block, RenderType> TYPE_BY_BLOCK;
    @Shadow @Final private static Map<Fluid, RenderType> TYPE_BY_FLUID;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static RenderType getChunkRenderType(BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof TripWireBlock) {
            return RenderType.TRANSLUCENT;
        }

        return (TYPE_BY_BLOCK.get(block) == RenderType.TRANSLUCENT) ? RenderType.TRANSLUCENT : RenderType.CUTOUT;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static RenderType getRenderLayer(FluidState fluidState) {
        RenderType renderType = TYPE_BY_FLUID.get(fluidState.getType());
        return renderType != null ? renderType : RenderType.CUTOUT;
    }
}
