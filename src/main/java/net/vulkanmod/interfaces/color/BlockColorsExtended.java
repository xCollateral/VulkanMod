package net.vulkanmod.interfaces.color;

import net.minecraft.client.color.block.BlockColors;
import net.vulkanmod.render.chunk.build.color.BlockColorRegistry;

public interface BlockColorsExtended {

    static BlockColorsExtended from(BlockColors blockColors) {
        return (BlockColorsExtended) blockColors;
    }

    BlockColorRegistry getColorResolverMap();
}
