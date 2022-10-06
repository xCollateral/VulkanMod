package net.vulkanmod.sdrs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.system.MemoryUtil;
public class  vertIntfce
{
 public static int currentSize;
public static long getFunc(String aa)
 { 
	 int[]ax = switch(aa) 
 { 
	case"blit_screen" ->vertblit_screen.vertblit_screen;
	case"particle" ->vertparticle.vertparticle;
	case"position" ->vertposition.vertposition;
	case"position_color" ->vertposition_color.vertposition_color;
	case"position_color_lightmap" ->vertposition_color_lightmap.vertposition_color_lightmap;
	case"position_color_normal" ->vertposition_color_normal.vertposition_color_normal;
	case"position_color_tex" ->vertposition_color_tex.vertposition_color_tex;
	case"position_color_tex_lightmap" ->vertposition_color_tex_lightmap.vertposition_color_tex_lightmap;
	case"position_tex" ->vertposition_tex.vertposition_tex;
	case"position_tex_color" ->vertposition_tex_color.vertposition_tex_color;
	case"position_tex_color_normal" ->vertposition_tex_color_normal.vertposition_tex_color_normal;
	case"rendertype_armor_cutout_no_cull" ->vertrendertype_armor_cutout_no_cull.vertrendertype_armor_cutout_no_cull;
	case"rendertype_armor_entity_glint" ->vertrendertype_armor_entity_glint.vertrendertype_armor_entity_glint;
	case"rendertype_armor_glint" ->vertrendertype_armor_glint.vertrendertype_armor_glint;
	case"rendertype_beacon_beam" ->vertrendertype_beacon_beam.vertrendertype_beacon_beam;
	case"rendertype_crumbling" ->vertrendertype_crumbling.vertrendertype_crumbling;
	case"rendertype_cutout" ->vertrendertype_cutout.vertrendertype_cutout;
	case"rendertype_cutout_mipped" ->vertrendertype_cutout_mipped.vertrendertype_cutout_mipped;
	case"rendertype_end_portal" ->vertrendertype_end_portal.vertrendertype_end_portal;
	case"rendertype_energy_swirl" ->vertrendertype_energy_swirl.vertrendertype_energy_swirl;
	case"rendertype_entity_alpha" ->vertrendertype_entity_alpha.vertrendertype_entity_alpha;
	case"rendertype_entity_cutout" ->vertrendertype_entity_cutout.vertrendertype_entity_cutout;
	case"rendertype_entity_cutout_no_cull" ->vertrendertype_entity_cutout_no_cull.vertrendertype_entity_cutout_no_cull;
	case"rendertype_entity_cutout_no_cull_z_offset" ->vertrendertype_entity_cutout_no_cull_z_offset.vertrendertype_entity_cutout_no_cull_z_offset;
	case"rendertype_entity_decal" ->vertrendertype_entity_decal.vertrendertype_entity_decal;
	case"rendertype_entity_glint" ->vertrendertype_entity_glint.vertrendertype_entity_glint;
	case"rendertype_entity_glint_direct" ->vertrendertype_entity_glint_direct.vertrendertype_entity_glint_direct;
	case"rendertype_entity_no_outline" ->vertrendertype_entity_no_outline.vertrendertype_entity_no_outline;
	case"rendertype_entity_shadow" ->vertrendertype_entity_shadow.vertrendertype_entity_shadow;
	case"rendertype_entity_smooth_cutout" ->vertrendertype_entity_smooth_cutout.vertrendertype_entity_smooth_cutout;
	case"rendertype_entity_solid" ->vertrendertype_entity_solid.vertrendertype_entity_solid;
	case"rendertype_entity_translucent" ->vertrendertype_entity_translucent.vertrendertype_entity_translucent;
	case"rendertype_entity_translucent_cull" ->vertrendertype_entity_translucent_cull.vertrendertype_entity_translucent_cull;
	case"rendertype_eyes" ->vertrendertype_eyes.vertrendertype_eyes;
	case"rendertype_glint" ->vertrendertype_glint.vertrendertype_glint;
	case"rendertype_glint_direct" ->vertrendertype_glint_direct.vertrendertype_glint_direct;
	case"rendertype_glint_translucent" ->vertrendertype_glint_translucent.vertrendertype_glint_translucent;
	case"rendertype_item_entity_translucent_cull" ->vertrendertype_item_entity_translucent_cull.vertrendertype_item_entity_translucent_cull;
	case"rendertype_leash" ->vertrendertype_leash.vertrendertype_leash;
	case"rendertype_lightning" ->vertrendertype_lightning.vertrendertype_lightning;
	case"rendertype_lines" ->vertrendertype_lines.vertrendertype_lines;
	case"rendertype_outline" ->vertrendertype_outline.vertrendertype_outline;
	case"rendertype_solid" ->vertrendertype_solid.vertrendertype_solid;
	case"rendertype_text" ->vertrendertype_text.vertrendertype_text;
	case"rendertype_text_see_through" ->vertrendertype_text_see_through.vertrendertype_text_see_through;
	case"rendertype_translucent" ->vertrendertype_translucent.vertrendertype_translucent;
	case"rendertype_translucent_moving_block" ->vertrendertype_translucent_moving_block.vertrendertype_translucent_moving_block;
	case"rendertype_translucent_no_crumbling" ->vertrendertype_translucent_no_crumbling.vertrendertype_translucent_no_crumbling;
	case"rendertype_tripwire" ->vertrendertype_tripwire.vertrendertype_tripwire;
	case"rendertype_water_mask" ->vertrendertype_water_mask.vertrendertype_water_mask;
	default -> throw new RuntimeException("Fail!: Invalid Shader File!");
};ByteBuffer axl = MemoryUtil.memAlignedAlloc(Integer.SIZE, ax.length*4); //Align to native uint32_t which is the alignment for the vkShaderModuleCreateInfo pCode in the C/C++ API
  axl.asIntBuffer().put(ax);
  currentSize=axl.remaining();
return MemoryUtil.memAddress0(axl);
}
}