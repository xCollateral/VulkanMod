package net.vulkanmod.mixin.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.shaders.Program;
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
    @Shadow private @Nullable static ShaderInstance positionTexShader;
    @Shadow private @Nullable static ShaderInstance positionTexColorShader;
    @Shadow private @Nullable static ShaderInstance particleShader;
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
    @Shadow private static @Nullable ShaderInstance rendertypeCloudsShader;

    @Shadow private static @Nullable ShaderInstance rendertypeTextBackgroundShader;
    @Shadow private static @Nullable ShaderInstance rendertypeTextBackgroundSeeThroughShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiOverlayShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiTextHighlightShader;
    @Shadow private static @Nullable ShaderInstance rendertypeGuiGhostRecipeOverlayShader;

    @Shadow private @Nullable static ShaderInstance positionColorLightmapShader;
    @Shadow private @Nullable static ShaderInstance positionColorTexLightmapShader;

    @Shadow public ShaderInstance blitShader;

    @Shadow protected abstract ShaderInstance preloadShader(ResourceProvider resourceProvider, String string, VertexFormat vertexFormat);

    @Shadow public abstract float getRenderDistance();

    @Shadow protected abstract void loadBlurEffect(ResourceProvider resourceProvider);

    @Inject(method = "reloadShaders", at = @At("HEAD"), cancellable = true)
    public void reloadShaders(ResourceProvider provider, CallbackInfo ci) {
        RenderSystem.assertOnRenderThread();

        List<Pair<ShaderInstance, Consumer<ShaderInstance>>> pairs = Lists.newArrayListWithCapacity(this.shaders.size());

        try {
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "particle", DefaultVertexFormat.PARTICLE),
                            (shaderInstance) -> particleShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "position", DefaultVertexFormat.POSITION),
                            (shaderInstance) -> positionShader = shaderInstance));

            ShaderInstance positionColor = new ShaderInstance(provider, "position_color", DefaultVertexFormat.POSITION_COLOR);
            //Added performance Overhead from using alpha testing for all glint effects should be negligible (Very little geometry)
            ShaderInstance rendertypeGlintTranslucent = new ShaderInstance(provider, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX);
            //TODO: only used for Falling Blocks,
            ShaderInstance rendertypeSolid = new ShaderInstance(provider, "rendertype_solid", DefaultVertexFormat.BLOCK);

            pairs.add(
                    Pair.of(
                            positionColor,
                            (shaderInstance) -> positionColorShader = shaderInstance));

            // These aren't used
//            pairs.add(
//                    Pair.of(new ShaderInstance(provider, "position_color_lightmap", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
//                            (shaderInstance) -> positionColorLightmapShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                            (shaderInstance) -> positionColorTexLightmapShader = shaderInstance));

            pairs.add(
                    Pair.of(new ShaderInstance(provider, "position_tex", DefaultVertexFormat.POSITION_TEX),
                            (shaderInstance) -> positionTexShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR),
                            (shaderInstance) -> positionTexColorShader = shaderInstance));
//            pairs.add(
//                    Pair.of(new ShaderInstance(provider, "position_tex_lightmap_color", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR),
//                            (shaderInstance) -> positionTexLightmapColorShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeSolid,
                            (shaderInstance) -> rendertypeSolidShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeSolid,
                            (shaderInstance) -> rendertypeCutoutMippedShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeSolid,
                            (shaderInstance) -> rendertypeCutoutShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeSolid,
                            (shaderInstance) -> rendertypeTranslucentShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK),
                            (shaderInstance) -> rendertypeTranslucentMovingBlockShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY),
                            (shaderInstance) -> rendertypeArmorCutoutNoCullShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY),
                            (shaderInstance) -> rendertypeEntitySolidShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY),
                            (shaderInstance) -> rendertypeEntityCutoutShader = shaderInstance));

            // No diff in these shaders
            ShaderInstance entity_no_cull = new ShaderInstance(provider, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY);
            pairs.add(
                    Pair.of(entity_no_cull,
                    (shaderInstance) -> rendertypeEntityCutoutNoCullShader = shaderInstance));
            pairs.add(
                    Pair.of(entity_no_cull,
                    (shaderInstance) -> rendertypeEntityCutoutNoCullZOffsetShader = shaderInstance));

            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeItemEntityTranslucentCullShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntityTranslucentCullShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntityTranslucentShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntityTranslucentEmissiveShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntitySmoothCutoutShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK),
                    (shaderInstance) -> rendertypeBeaconBeamShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntityDecalShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntityNoOutlineShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntityShadowShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEntityAlphaShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEyesShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeEnergySwirlShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
                    (shaderInstance) -> rendertypeLeashShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_water_mask", DefaultVertexFormat.POSITION),
                    (shaderInstance) -> rendertypeWaterMaskShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_outline", DefaultVertexFormat.POSITION_TEX_COLOR),
                    (shaderInstance) -> rendertypeOutlineShader = shaderInstance));
//            pairs.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
//                rendertypeArmorGlintShader = shaderInstance;
//            }));
            pairs.add(
                    Pair.of(rendertypeGlintTranslucent,
                    (shaderInstance) -> rendertypeArmorEntityGlintShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeGlintTranslucent,
                    (shaderInstance) -> rendertypeGlintTranslucentShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeGlintTranslucent,
                    (shaderInstance) -> rendertypeGlintShader = shaderInstance));
//            pairs.add(Pair.of(new ShaderInstance(provider, "rendertype_glint_direct", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
//                rendertypeGlintDirectShader = shaderInstance;
//            }));
            pairs.add(
                    Pair.of(rendertypeGlintTranslucent,
                    (shaderInstance) -> rendertypeEntityGlintShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeGlintTranslucent,
                    (shaderInstance) -> rendertypeEntityGlintDirectShader = shaderInstance));

            //Text
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                    (shaderInstance) -> rendertypeTextShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_text_background", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
                    (shaderInstance) -> rendertypeTextBackgroundShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                    (shaderInstance) -> rendertypeTextIntensityShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                    (shaderInstance) -> rendertypeTextSeeThroughShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_text_background_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
                    (shaderInstance) -> rendertypeTextBackgroundSeeThroughShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                    (shaderInstance) -> rendertypeTextIntensitySeeThroughShader = shaderInstance));

            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR),
                    (shaderInstance) -> rendertypeLightningShader = shaderInstance));
            pairs.add(
                    Pair.of(rendertypeSolid,
                    (shaderInstance) -> rendertypeTripwireShader = shaderInstance));
            ShaderInstance endPortalShader = new ShaderInstance(provider, "rendertype_end_portal", DefaultVertexFormat.POSITION);
            pairs.add(
                    Pair.of(endPortalShader,
                    (shaderInstance) -> rendertypeEndPortalShader = shaderInstance));
            pairs.add(
                    Pair.of(endPortalShader,
                    (shaderInstance) -> rendertypeEndGatewayShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_clouds", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),
                    (shaderInstance) -> rendertypeCloudsShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL),
                    (shaderInstance) -> rendertypeLinesShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_crumbling", DefaultVertexFormat.BLOCK),
                    (shaderInstance) -> rendertypeCrumblingShader = shaderInstance));

            pairs.add(
                    Pair.of(positionColor,
                    (shaderInstance) -> rendertypeGuiShader = shaderInstance));
            pairs.add(
                    Pair.of(positionColor,
                    (shaderInstance) -> rendertypeGuiOverlayShader = shaderInstance));
            pairs.add(
                    Pair.of(positionColor,
                    (shaderInstance) -> rendertypeGuiTextHighlightShader = shaderInstance));
            pairs.add(
                    Pair.of(positionColor,
                    (shaderInstance) -> rendertypeGuiGhostRecipeOverlayShader = shaderInstance));
            pairs.add(
                    Pair.of(new ShaderInstance(provider, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY),
                    (shaderInstance) -> rendertypeBreezeWindShader = shaderInstance));

            // FRAPI shader loading
            CoreShaderRegistrationCallback.RegistrationContext context = (id, vertexFormat, loadCallback) -> {
                ShaderInstance program = new FabricShaderProgram(provider, id, vertexFormat);
                pairs.add(Pair.of(program, loadCallback));
            };
            CoreShaderRegistrationCallback.EVENT.invoker().registerShaders(context);

            this.loadBlurEffect(provider);
        } catch (IOException ioexception) {
            pairs.forEach((pair) -> pair.getFirst().close());
            throw new RuntimeException("could not reload shaders", ioexception);
        }

        this.shutdownShaders();
        pairs.forEach((pair) -> {
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
