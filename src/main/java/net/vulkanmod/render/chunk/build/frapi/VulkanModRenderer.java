package net.vulkanmod.render.chunk.build.frapi;

import java.util.HashMap;

import net.minecraft.resources.ResourceLocation;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.vulkanmod.render.chunk.build.frapi.material.MaterialFinderImpl;
import net.vulkanmod.render.chunk.build.frapi.material.RenderMaterialImpl;
import net.vulkanmod.render.chunk.build.frapi.mesh.MeshBuilderImpl;

/**
 * The Fabric default renderer implementation. Supports all
 * features defined in the API except shaders and offers no special materials.
 */
public class VulkanModRenderer implements Renderer {
	public static final VulkanModRenderer INSTANCE = new VulkanModRenderer();

	public static final RenderMaterial MATERIAL_STANDARD = INSTANCE.materialFinder().find();

	static {
		INSTANCE.registerMaterial(RenderMaterial.MATERIAL_STANDARD, MATERIAL_STANDARD);
	}

	private final HashMap<ResourceLocation, RenderMaterial> materialMap = new HashMap<>();

	private VulkanModRenderer() {}

	@Override
	public MeshBuilder meshBuilder() {
		return new MeshBuilderImpl();
	}

	@Override
	public MaterialFinder materialFinder() {
		return new MaterialFinderImpl();
	}

	@Override
	public RenderMaterial materialById(ResourceLocation id) {
		return materialMap.get(id);
	}

	@Override
	public boolean registerMaterial(ResourceLocation id, RenderMaterial material) {
		if (materialMap.containsKey(id)) return false;

		// cast to prevent acceptance of impostor implementations
		materialMap.put(id, (RenderMaterialImpl) material);
		return true;
	}
}
