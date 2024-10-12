package net.vulkanmod.render.chunk.build.renderer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;

public abstract class DefaultFluidRenderers {

	private static final ReferenceOpenHashSet<FluidRenderHandler> set = new ReferenceOpenHashSet<>();

	public static void add(FluidRenderHandler handler) {
		set.add(handler);
	}

	public static boolean has(FluidRenderHandler handler) {
		return set.contains(handler);
	}
}
