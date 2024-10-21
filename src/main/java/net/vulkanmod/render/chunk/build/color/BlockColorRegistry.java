package net.vulkanmod.render.chunk.build.color;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.world.level.block.Block;

public class BlockColorRegistry {

	private final Reference2ReferenceOpenHashMap<Block, BlockColor> map = new Reference2ReferenceOpenHashMap<>();

	public void register(BlockColor blockColor, Block... blocks) {
		for (Block block : blocks) {
			this.map.put(block, blockColor);
		}
	}

	public BlockColor getBlockColor(Block block) {
		return this.map.get(block);
	}

}
