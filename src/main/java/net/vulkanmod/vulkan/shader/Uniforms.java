package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Supplier;

public class Uniforms {
    //TODO: might just use Enum System again w. basic hash matcing +update/rjetcion skips isnetad of hashmap
    public static Object2ReferenceOpenHashMap<String, Supplier<Integer>> vec1i_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static Object2ReferenceOpenHashMap<String, Supplier<Float>> vec1f_uniformMap = new Object2ReferenceOpenHashMap<>();
//    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec2f_uniformMap = new Object2ReferenceOpenHashMap<>();
//    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec3f_uniformMap = new Object2ReferenceOpenHashMap<>();
//    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec4f_uniformMap = new Object2ReferenceOpenHashMap<>();

//    public static EnumSet<UniformState> mat4f_uniformMap = EnumSet.of(UniformState.ModelViewMat, UniformState.ProjMat, UniformState.MVP, UniformState.TextureMat);

    public static void setupDefaultUniforms() {

//        //Mat4
//        mat4f_uniformMap.put(UniformState.ModelViewMat, UniformState.ModelViewMat::getMappedBufferPtr);
//        mat4f_uniformMap.put(UniformState.ProjMat, UniformState.ProjMat::getMappedBufferPtr);
//        mat4f_uniformMap.put(UniformState.MVP, UniformState.MVP::getMappedBufferPtr);
//        mat4f_uniformMap.put(UniformState.TextureMat, UniformState.TextureMat::getMappedBufferPtr);

        //Vec1i
        vec1i_uniformMap.put("EndPortalLayers", () -> 15);
        vec1i_uniformMap.put("FogShape", () -> RenderSystem.getShaderFogShape().getIndex());

        //Vec1
        vec1f_uniformMap.put("FogStart", RenderSystem::getShaderFogStart);
        vec1f_uniformMap.put("FogEnd", RenderSystem::getShaderFogEnd);
        vec1f_uniformMap.put("LineWidth", RenderSystem::getShaderLineWidth);
        vec1f_uniformMap.put("GameTime", RenderSystem::getShaderGameTime);
        vec1f_uniformMap.put("AlphaCutout", () -> VRenderSystem.alphaCutout);

        //Vec2
//        vec2f_uniformMap.put("ScreenSize", VRenderSystem::getScreenSize);

        //Vec3
//        vec3f_uniformMap.put("Light0_Direction", UniformState.Light0_Direction::getMappedBufferPtr);
//        vec3f_uniformMap.put("Light1_Direction", UniformState.Light1_Direction::getMappedBufferPtr);
//        vec3f_uniformMap.put("ChunkOffset", UniformState.ChunkOffset::getMappedBufferPtr);

//        //Vec4
//        vec4f_uniformMap.put("ColorModulator", UniformState.ColorModulator::getMappedBufferPtr);
//        vec4f_uniformMap.put("FogColor", VRenderSystem::getShaderFogColor);

    }
}
