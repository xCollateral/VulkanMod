package net.vulkanmod.mixin.render.frapi;

import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
	@Invoker("hasAnimatedTexture")
	static boolean hasAnimatedTexture(ItemStack stack) {
		throw new AssertionError();
	}
}
