package net.vulkanmod.mixin.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Program;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow @Final private Map<String, Shader> shaders;

    @Shadow @Final private static Identifier NAUSEA_OVERLAY;
    @Shadow @Final private static boolean field_32688;
    @Shadow @Final public static float CAMERA_DEPTH;
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceManager resourceManager;
    @Shadow @Final private Random random;
    @Shadow private float viewDistance;
    @Shadow @Final public HeldItemRenderer firstPersonRenderer;
    @Shadow @Final private MapRenderer mapRenderer;
    @Shadow @Final private BufferBuilderStorage buffers;
    @Shadow private int ticks;

    @Shadow private float skyDarkness;
    @Shadow private float lastSkyDarkness;
    @Shadow private boolean renderHand;
    @Shadow private boolean blockOutlineEnabled;
    @Shadow private long lastWorldIconUpdate;
    @Shadow private boolean hasWorldIcon;
    @Shadow private long lastWindowFocusedTime;
    @Shadow @Final private LightmapTextureManager lightmapTextureManager;
    @Shadow @Final private OverlayTexture overlayTexture;
    @Shadow private boolean renderingPanorama;
    @Shadow private float zoom;
    @Shadow private float zoomX;
    @Shadow private float zoomY;
    @Shadow @Final public static int field_32687;
    @Shadow private @Nullable ItemStack floatingItem;
    @Shadow private int floatingItemTimeLeft;
    @Shadow private float floatingItemWidth;
    @Shadow private float floatingItemHeight;
    @Shadow private @Nullable ShaderEffect shader;
    @Shadow @Final private static Identifier[] SHADERS_LOCATIONS;
    @Shadow @Final public static int SHADER_COUNT;
    @Shadow private int forcedShaderIndex;
    @Shadow private boolean shadersEnabled;
    @Shadow @Final private Camera camera;
    @Shadow public Shader blitScreenShader;
    @Shadow private @Nullable static Shader positionShader;
    @Shadow private @Nullable static Shader positionColorShader;
    @Shadow private @Nullable static Shader positionColorTexShader;
    @Shadow private @Nullable static Shader positionTexShader;
    @Shadow private @Nullable static Shader positionTexColorShader;
    @Shadow private @Nullable static Shader blockShader;
    @Shadow private @Nullable static Shader newEntityShader;
    @Shadow private @Nullable static Shader particleShader;
    @Shadow private @Nullable static Shader positionColorLightmapShader;
    @Shadow private @Nullable static Shader positionColorTexLightmapShader;
    @Shadow private @Nullable static Shader positionTexColorNormalShader;
    @Shadow private @Nullable static Shader positionTexLightmapColorShader;
    @Shadow private @Nullable static Shader renderTypeSolidShader;
    @Shadow private @Nullable static Shader renderTypeCutoutMippedShader;
    @Shadow private @Nullable static Shader renderTypeCutoutShader;
    @Shadow private @Nullable static Shader renderTypeTranslucentShader;
    @Shadow private @Nullable static Shader renderTypeTranslucentMovingBlockShader;
    @Shadow private @Nullable static Shader renderTypeTranslucentNoCrumblingShader;
    @Shadow private @Nullable static Shader renderTypeArmorCutoutNoCullShader;
    @Shadow private @Nullable static Shader renderTypeEntitySolidShader;
    @Shadow private @Nullable static Shader renderTypeEntityCutoutShader;
    @Shadow private @Nullable static Shader renderTypeEntityCutoutNoNullShader;
    @Shadow private @Nullable static Shader renderTypeEntityCutoutNoNullZOffsetShader;
    @Shadow private @Nullable static Shader renderTypeItemEntityTranslucentCullShader;
    @Shadow private @Nullable static Shader renderTypeEntityTranslucentCullShader;
    @Shadow private @Nullable static Shader renderTypeEntityTranslucentShader;
    @Shadow private @Nullable static Shader renderTypeEntitySmoothCutoutShader;
    @Shadow private @Nullable static Shader renderTypeBeaconBeamShader;
    @Shadow private @Nullable static Shader renderTypeEntityDecalShader;
    @Shadow private @Nullable static Shader renderTypeEntityNoOutlineShader;
    @Shadow private @Nullable static Shader renderTypeEntityShadowShader;
    @Shadow private @Nullable static Shader renderTypeEntityAlphaShader;
    @Shadow private @Nullable static Shader renderTypeEyesShader;
    @Shadow private @Nullable static Shader renderTypeEnergySwirlShader;
    @Shadow private @Nullable static Shader renderTypeLeashShader;
    @Shadow private @Nullable static Shader renderTypeWaterMaskShader;
    @Shadow private @Nullable static Shader renderTypeOutlineShader;
    @Shadow private @Nullable static Shader renderTypeArmorGlintShader;
    @Shadow private @Nullable static Shader renderTypeArmorEntityGlintShader;
    @Shadow private @Nullable static Shader renderTypeGlintTranslucentShader;
    @Shadow private @Nullable static Shader renderTypeGlintShader;
    @Shadow private @Nullable static Shader renderTypeGlintDirectShader;
    @Shadow private @Nullable static Shader renderTypeEntityGlintShader;
    @Shadow private @Nullable static Shader renderTypeEntityGlintDirectShader;
    @Shadow private @Nullable static Shader renderTypeTextShader;
    @Shadow private @Nullable static Shader renderTypeTextIntensityShader;
    @Shadow private @Nullable static Shader renderTypeTextSeeThroughShader;
    @Shadow private @Nullable static Shader renderTypeTextIntensitySeeThroughShader;
    @Shadow private @Nullable static Shader renderTypeLightningShader;
    @Shadow private @Nullable static Shader renderTypeTripwireShader;
    @Shadow private @Nullable static Shader renderTypeEndPortalShader;
    @Shadow private @Nullable static Shader renderTypeEndGatewayShader;
    @Shadow private @Nullable static Shader renderTypeLinesShader;
    @Shadow private @Nullable static Shader renderTypeCrumblingShader;

    /**
     * @author
     */
    @Overwrite
    public void loadShaders(ResourceManager p_172768_) {
        RenderSystem.assertOnRenderThread();
        List<Program> list = Lists.newArrayList();
//        list.addAll(Program.Type.FRAGMENT.getPrograms().values());
//        list.addAll(Program.Type.VERTEX.getPrograms().values());
//        list.forEach(Program::close);
        List<Pair<Shader, Consumer<Shader>>> list1 = Lists.newArrayListWithCapacity(this.shaders.size());

        try {
//         list1.add(Pair.of(new Shader(p_172768_, "block", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172743_) -> {
//            blockShader = p_172743_;
//         }));
//         list1.add(Pair.of(new Shader(p_172768_, "new_entity", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172740_) -> {
//            newEntityShader = p_172740_;
//         }));
            list1.add(Pair.of(new Shader(p_172768_, "particle", VertexFormats.POSITION_TEXTURE_COLOR_LIGHT), (p_172714_) -> {
                particleShader = p_172714_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "position", VertexFormats.POSITION), (p_172711_) -> {
                positionShader = p_172711_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "position_color", VertexFormats.POSITION_COLOR), (p_172708_) -> {
                positionColorShader = p_172708_;
            }));
//         list1.add(Pair.of(new Shader(p_172768_, "position_color_lightmap", VertexFormats.POSITION_COLOR_LIGHTMAP), (p_172705_) -> {
//            positionColorLightmapShader = p_172705_;
//         }));
            list1.add(Pair.of(new Shader(p_172768_, "position_color_tex", VertexFormats.POSITION_COLOR_TEXTURE), (p_172702_) -> {
                positionColorTexShader = p_172702_;
            }));
//         list1.add(Pair.of(new Shader(p_172768_, "position_color_tex_lightmap", VertexFormats.POSITION_COLOR_TEXTURE_LIGHTMAP), (p_172699_) -> {
//            positionColorTexLightmapShader = p_172699_;
//         }));
            list1.add(Pair.of(new Shader(p_172768_, "position_tex", VertexFormats.POSITION_TEXTURE), (p_172696_) -> {
                positionTexShader = p_172696_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "position_tex_color", VertexFormats.POSITION_TEXTURE_COLOR), (p_172693_) -> {
                positionTexColorShader = p_172693_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "position_tex_color_normal", VertexFormats.POSITION_TEXTURE_COLOR_NORMAL), (p_172690_) -> {
                positionTexColorNormalShader = p_172690_;
            }));
//         list1.add(Pair.of(new Shader(p_172768_, "position_tex_lightmap_color", VertexFormats.POSITION_TEXTURE_LIGHTMAP_COLOR), (p_172687_) -> {
//            positionTexLightmapColorShader = p_172687_;
//         }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_solid", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172684_) -> {
                renderTypeSolidShader = p_172684_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_cutout_mipped", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172681_) -> {
                renderTypeCutoutMippedShader = p_172681_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_cutout", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172678_) -> {
                renderTypeCutoutShader = p_172678_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_translucent", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172675_) -> {
                renderTypeTranslucentShader = p_172675_;
            }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_translucent_moving_block", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172672_) -> {
            renderTypeTranslucentMovingBlockShader = p_172672_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_translucent_no_crumbling", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172669_) -> {
            renderTypeTranslucentNoCrumblingShader = p_172669_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_armor_cutout_no_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172666_) -> {
            renderTypeArmorCutoutNoCullShader = p_172666_;
         }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_solid", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172663_) -> {
                renderTypeEntitySolidShader = p_172663_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_cutout", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172660_) -> {
                renderTypeEntityCutoutShader = p_172660_;
            }));

            //No diff in these shaders
            Shader entity_no_cull = new Shader(p_172768_, "rendertype_entity_cutout_no_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            list1.add(Pair.of(entity_no_cull, (p_172657_) -> {
                renderTypeEntityCutoutNoNullShader = p_172657_;
            }));
//         list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_cutout_no_cull_z_offset", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172654_) -> {
//            renderTypeEntityCutoutNoCullZOffsetShader = p_172654_;
//         }));
            list1.add(Pair.of(entity_no_cull, (p_172654_) -> {
                renderTypeEntityCutoutNoNullZOffsetShader = p_172654_;
             }));

            list1.add(Pair.of(new Shader(p_172768_, "rendertype_item_entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172651_) -> {
                renderTypeItemEntityTranslucentCullShader = p_172651_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172648_) -> {
                renderTypeEntityTranslucentCullShader = p_172648_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_translucent", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172645_) -> {
                renderTypeEntityTranslucentShader = p_172645_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_smooth_cutout", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172642_) -> {
                renderTypeEntitySmoothCutoutShader = p_172642_;
            }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_beacon_beam", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172639_) -> {
            renderTypeBeaconBeamShader = p_172639_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_decal", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172840_) -> {
            renderTypeEntityDecalShader = p_172840_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_no_outline", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172837_) -> {
            renderTypeEntityNoOutlineShader = p_172837_;
         }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_shadow", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172834_) -> {
                renderTypeEntityShadowShader = p_172834_;
            }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_alpha", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172831_) -> {
            renderTypeEntityAlphaShader = p_172831_;
         }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_eyes", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172828_) -> {
                renderTypeEyesShader = p_172828_;
            }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_energy_swirl", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL), (p_172825_) -> {
            renderTypeEnergySwirlShader = p_172825_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_leash", VertexFormats.POSITION_COLOR_LIGHT), (p_172822_) -> {
            renderTypeLeashShader = p_172822_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_water_mask", VertexFormats.POSITION), (p_172819_) -> {
            renderTypeWaterMaskShader = p_172819_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_outline", VertexFormats.POSITION_COLOR_TEXTURE), (p_172816_) -> {
            renderTypeOutlineShader = p_172816_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_armor_glint", VertexFormats.POSITION_TEXTURE), (p_172813_) -> {
            renderTypeArmorGlintShader = p_172813_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_armor_entity_glint", VertexFormats.POSITION_TEXTURE), (p_172810_) -> {
            renderTypeArmorEntityGlintShader = p_172810_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_glint_translucent", VertexFormats.POSITION_TEXTURE), (p_172807_) -> {
            renderTypeGlintTranslucentShader = p_172807_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_glint", VertexFormats.POSITION_TEXTURE), (p_172805_) -> {
            renderTypeGlintShader = p_172805_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_glint_direct", VertexFormats.POSITION_TEXTURE), (p_172803_) -> {
            renderTypeGlintDirectShader = p_172803_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_glint", VertexFormats.POSITION_TEXTURE), (p_172801_) -> {
            renderTypeEntityGlintShader = p_172801_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_entity_glint_direct", VertexFormats.POSITION_TEXTURE), (p_172799_) -> {
            renderTypeEntityGlintDirectShader = p_172799_;
         }));
         Shader textShader = new Shader(p_172768_, "rendertype_text", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
            list1.add(Pair.of(textShader, (p_172796_) -> {
                renderTypeTextShader = p_172796_;
            }));
         list1.add(Pair.of(textShader, (p_172794_) -> {
            renderTypeTextIntensityShader = p_172794_;
         }));
         Shader seeThroughShader = new Shader(p_172768_, "rendertype_text_see_through", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
         list1.add(Pair.of(seeThroughShader, (p_172792_) -> {
            renderTypeTextSeeThroughShader = p_172792_;
         }));
         list1.add(Pair.of(seeThroughShader, (p_172789_) -> {
            renderTypeTextIntensitySeeThroughShader = p_172789_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_lightning", VertexFormats.POSITION_COLOR), (p_172787_) -> {
            renderTypeLightningShader = p_172787_;
         }));
         list1.add(Pair.of(new Shader(p_172768_, "rendertype_tripwire", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172785_) -> {
                renderTypeTripwireShader = p_172785_;
            }));
         Shader endPortalShader = new Shader(p_172768_, "rendertype_end_portal", VertexFormats.POSITION);
         list1.add(Pair.of(endPortalShader, (p_172782_) -> {
            renderTypeEndPortalShader = p_172782_;
         }));
         list1.add(Pair.of(endPortalShader, (p_172778_) -> {
            renderTypeEndGatewayShader = p_172778_;
         }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_lines", VertexFormats.LINES), (p_172774_) -> {
                renderTypeLinesShader = p_172774_;
            }));
            list1.add(Pair.of(new Shader(p_172768_, "rendertype_crumbling", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL), (p_172733_) -> {
                renderTypeCrumblingShader = p_172733_;
            }));
        } catch (IOException ioexception) {
            list1.forEach((p_172772_) -> {
                p_172772_.getFirst().close();
            });
            throw new RuntimeException("could not reload shaders", ioexception);
        }

        //this.shutdownShaders();
        //TODO: clear shaders
        list1.forEach((p_172729_) -> {
            Shader shaderinstance = p_172729_.getFirst();
            this.shaders.put(shaderinstance.getName(), shaderinstance);
            p_172729_.getSecond().accept(shaderinstance);
        });
    }

}
