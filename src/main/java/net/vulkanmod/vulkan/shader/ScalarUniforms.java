package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.function.Supplier;

public class ScalarUniforms {
    // might just use Enum System again w/ basic hash matching + update skips instead of hashmap
    public static Object2ReferenceOpenHashMap<String, Supplier<Integer>> vec1i_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static Object2ReferenceOpenHashMap<String, Supplier<Float>> vec1f_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static void setupDefaultUniforms() {


        //Vec1i
        vec1i_uniformMap.put("EndPortalLayers", () -> 15);
        vec1i_uniformMap.put("FogShape", () -> RenderSystem.getShaderFogShape().getIndex());

        //Vec1
        vec1f_uniformMap.put("FogStart", RenderSystem::getShaderFogStart);
        vec1f_uniformMap.put("FogEnd", RenderSystem::getShaderFogEnd);
        vec1f_uniformMap.put("LineWidth", RenderSystem::getShaderLineWidth);
        vec1f_uniformMap.put("GameTime", RenderSystem::getShaderGameTime);
        vec1f_uniformMap.put("GlintAlpha", RenderSystem::getShaderGlintAlpha);
        vec1f_uniformMap.put("AlphaCutout", () -> VRenderSystem.alphaCutout);

    }
}
