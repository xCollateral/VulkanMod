package net.vulkanmod.mixin.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private Map<String, ShaderInstance> shaders;

    @Shadow private @Nullable static ShaderInstance positionShader;
    @Shadow private @Nullable static ShaderInstance positionColorShader;
    @Shadow private @Nullable static ShaderInstance positionColorTexShader;
    @Shadow private @Nullable static ShaderInstance positionTexShader;
    @Shadow private @Nullable static ShaderInstance positionTexColorShader;
    @Shadow private @Nullable static ShaderInstance particleShader;
    @Shadow private @Nullable static ShaderInstance positionTexColorNormalShader;
    @Shadow private @Nullable static ShaderInstance rendertypeSolidShader;
    @Shadow private @Nullable static ShaderInstance rendertypeCutoutMippedShader;
    @Shadow private @Nullable static ShaderInstance rendertypeCutoutShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTranslucentShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTranslucentMovingBlockShader;
    @Shadow private @Nullable static ShaderInstance rendertypeArmorCutoutNoCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntitySolidShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityCutoutShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityCutoutNoCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityCutoutNoCullZOffsetShader;
    @Shadow private @Nullable static ShaderInstance rendertypeItemEntityTranslucentCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityTranslucentCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityTranslucentShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityTranslucentEmissiveShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntitySmoothCutoutShader;
    @Shadow private @Nullable static ShaderInstance rendertypeBeaconBeamShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityDecalShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityNoOutlineShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityShadowShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityAlphaShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEyesShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEnergySwirlShader;
    @Shadow private @Nullable static ShaderInstance rendertypeLeashShader;
    @Shadow private @Nullable static ShaderInstance rendertypeWaterMaskShader;
    @Shadow private @Nullable static ShaderInstance rendertypeOutlineShader;
    @Shadow private @Nullable static ShaderInstance rendertypeArmorGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeArmorEntityGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeGlintTranslucentShader;
    @Shadow private @Nullable static ShaderInstance rendertypeGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeGlintDirectShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityGlintDirectShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextIntensityShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextSeeThroughShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextIntensitySeeThroughShader;
    @Shadow private @Nullable static ShaderInstance rendertypeLightningShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTripwireShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEndPortalShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEndGatewayShader;
    @Shadow private @Nullable static ShaderInstance rendertypeLinesShader;
    @Shadow private @Nullable static ShaderInstance rendertypeCrumblingShader;
    @Shadow private static @Nullable ShaderInstance rendertypeBreezeWindShader;

    @Shadow private static @Nullable ShaderInstance rendertypeTextBackgroundShader;
    @Shadow private static @Nullable ShaderInstance rendertypeTextBackgroundSeeThroughShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiOverlayShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiTextHighlightShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiGhostRecipeOverlayShader;

    @Shadow private @Nullable static ShaderInstance positionColorLightmapShader;
    @Shadow private @Nullable static ShaderInstance positionColorTexLightmapShader;
    @Shadow private @Nullable static ShaderInstance positionTexLightmapColorShader;

    @Shadow public ShaderInstance blitShader;

    @Shadow protected abstract ShaderInstance preloadShader(ResourceProvider resourceProvider, String string, VertexFormat vertexFormat);

    @Shadow public abstract float getRenderDistance();

    @Inject(method = "reloadShaders", at = @At("HEAD"), cancellable = true)
    public void reloadShaders(ResourceProvider provider, CallbackInfo ci) throws IOException {
        RenderSystem.assertOnRenderThread();

        List<Pair<ShaderInstance, Consumer<ShaderInstance>>> shaders = Lists.newArrayListWithCapacity(this.shaders.size());

        try {
            shaders.add(Pair.of(new ShaderInstance(provider, "particle", DefaultVertexFormat.PARTICLE), (shaderInstance) -> {
                particleShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position", DefaultVertexFormat.POSITION), (shaderInstance) -> {
                positionShader = shaderInstance;
            }));

            ShaderInstance positionColor = new ShaderInstance(provider, "position_color", DefaultVertexFormat.POSITION_COLOR);
            shaders.add(Pair.of(positionColor, (shaderInstance) -> positionColorShader = shaderInstance));
//            shaders.add(Pair.of(new ShaderInstance(provider, "position_color_lightmap", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (shaderInstance) -> {
//               positionColorLightmapShader = shaderInstance;
//            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX), (shaderInstance) -> {
                positionColorTexShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
               positionColorTexLightmapShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_tex", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                positionTexShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR), (shaderInstance) -> {
                positionTexColorShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_tex_color_normal", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), (shaderInstance) -> {
                positionTexColorNormalShader = shaderInstance;
            }));
//            shaders.add(Pair.of(new ShaderInstance(provider, "position_tex_lightmap_color", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR), (shaderInstance) -> {
//               positionTexLightmapColorShader = shaderInstance;
//            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_solid", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeSolidShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_cutout_mipped", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeCutoutMippedShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_cutout", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeCutoutShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_translucent", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeTranslucentShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeTranslucentMovingBlockShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeArmorCutoutNoCullShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntitySolidShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityCutoutShader = shaderInstance;
            }));

            //No diff in these shaders
            ShaderInstance entity_no_cull = new ShaderInstance(provider, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY);
            shaders.add(Pair.of(entity_no_cull, (shaderInstance) -> {
                rendertypeEntityCutoutNoCullShader = shaderInstance;
            }));
//            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_cutout_no_cull_z_offset", DefaultVertexFormat.POSITION_COLOR_TEX_OVERLAY_LIGHTMAP), (p_172654_) -> {
//               rendertypeEntityCutoutNoCullZOffsetShader = p_172654_;
//            }));
            shaders.add(Pair.of(entity_no_cull, (shaderInstance) -> {
                rendertypeEntityCutoutNoCullZOffsetShader = shaderInstance;
            }));

            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeItemEntityTranslucentCullShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityTranslucentCullShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityTranslucentShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY), shader -> {
                rendertypeEntityTranslucentEmissiveShader = shader;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntitySmoothCutoutShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeBeaconBeamShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityDecalShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityNoOutlineShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityShadowShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityAlphaShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEyesShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEnergySwirlShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (shaderInstance) -> {
                rendertypeLeashShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_water_mask", DefaultVertexFormat.POSITION), (shaderInstance) -> {
                rendertypeWaterMaskShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_outline", DefaultVertexFormat.POSITION_COLOR_TEX), (shaderInstance) -> {
                rendertypeOutlineShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeArmorGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_entity_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeArmorEntityGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeGlintTranslucentShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_glint_direct", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeGlintDirectShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeEntityGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_glint_direct", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeEntityGlintDirectShader = shaderInstance;
            }));

            //Text
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_background", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextBackgroundShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextIntensityShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextSeeThroughShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_background_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextBackgroundSeeThroughShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextIntensitySeeThroughShader = shaderInstance;
            }));

            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR), (shaderInstance) -> {
                rendertypeLightningShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_tripwire", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeTripwireShader = shaderInstance;
            }));
            ShaderInstance endPortalShader = new ShaderInstance(provider, "rendertype_end_portal", DefaultVertexFormat.POSITION);
            shaders.add(Pair.of(endPortalShader, (shaderInstance) -> {
                rendertypeEndPortalShader = shaderInstance;
            }));
            shaders.add(Pair.of(endPortalShader, (shaderInstance) -> {
                rendertypeEndGatewayShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL), (shaderInstance) -> {
                rendertypeLinesShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_crumbling", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeCrumblingShader = shaderInstance;
            }));

            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiShader = shaderInstance;
            }));
            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiOverlayShader = shaderInstance;
            }));
            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiTextHighlightShader = shaderInstance;
            }));
            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiGhostRecipeOverlayShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeBreezeWindShader = shaderInstance;
            }));
        } catch (IOException ioexception) {
            shaders.forEach((pair) -> pair.getFirst().close());
            throw new RuntimeException("could not reload shaders", ioexception);
        }

        // FRAPI shader loading
        CoreShaderRegistrationCallback.RegistrationContext context = (id, vertexFormat, loadCallback) -> {
            ShaderInstance program = new FabricShaderProgram(provider, id, vertexFormat);
            shaders.add(Pair.of(program, loadCallback));
        };
        CoreShaderRegistrationCallback.EVENT.invoker().registerShaders(context);

        this.shutdownShaders();
        shaders.forEach((pair) -> {
            ShaderInstance shaderinstance = pair.getFirst();
            this.shaders.put(shaderinstance.getName(), shaderinstance);
            pair.getSecond().accept(shaderinstance);
        });

        ci.cancel();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void shutdownShaders() {
        RenderSystem.assertOnRenderThread();

        final var clearList = ImmutableList.copyOf(this.shaders.values());
        MemoryManager.getInstance().addFrameOp(() -> clearList.forEach((ShaderInstance::close)));

        this.shaders.clear();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void preloadUiShader(ResourceProvider resourceProvider) {
        if (this.blitShader != null) {
            throw new RuntimeException("Blit shader already preloaded");
        } else {
            try {
                this.blitShader = new ShaderInstance(resourceProvider, "blit_screen", DefaultVertexFormat.POSITION_TEX);
            } catch (IOException var3) {
                throw new RuntimeException("could not preload blit shader", var3);
            }

            positionShader = this.preloadShader(resourceProvider, "position", DefaultVertexFormat.POSITION);
            positionColorShader = this.preloadShader(resourceProvider, "position_color", DefaultVertexFormat.POSITION_COLOR);
            positionColorTexShader = this.preloadShader(resourceProvider, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX);
            positionTexShader = this.preloadShader(resourceProvider, "position_tex", DefaultVertexFormat.POSITION_TEX);
            positionTexColorShader = this.preloadShader(resourceProvider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR);
            rendertypeTextShader = this.preloadShader(resourceProvider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

            rendertypeGuiShader = positionColorShader;
            rendertypeGuiOverlayShader = positionColorShader;
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public float getDepthFar() {
//        return this.getRenderDistance() * 4.0F;
        return Float.POSITIVE_INFINITY;
    }

}
