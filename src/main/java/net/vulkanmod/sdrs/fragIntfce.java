package net.vulkanmod.sdrs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.system.MemoryUtil;
public class  fragIntfce
{
 public static int currentSize;
public static long getFunc(String aa)
 { 
	 int[]ax = switch(aa) 
 { 
	case"blit_screen" ->fragblit_screen.fragblit_screen;
	case"particle" ->fragparticle.fragparticle;
	case"position" ->fragposition.fragposition;
	case"position_color" ->fragposition_color.fragposition_color;
	case"position_color_lightmap" ->fragposition_color_lightmap.fragposition_color_lightmap;
	case"position_color_normal" ->fragposition_color_normal.fragposition_color_normal;
	case"position_color_tex" ->fragposition_color_tex.fragposition_color_tex;
	case"position_color_tex_lightmap" ->fragposition_color_tex_lightmap.fragposition_color_tex_lightmap;
	case"position_tex" ->fragposition_tex.fragposition_tex;
	case"position_tex_color" ->fragposition_tex_color.fragposition_tex_color;
	case"position_tex_color_normal" ->fragposition_tex_color_normal.fragposition_tex_color_normal;
	case"rendertype_armor_cutout_no_cull" ->fragrendertype_armor_cutout_no_cull.fragrendertype_armor_cutout_no_cull;
	case"rendertype_armor_entity_glint" ->fragrendertype_armor_entity_glint.fragrendertype_armor_entity_glint;
	case"rendertype_armor_glint" ->fragrendertype_armor_glint.fragrendertype_armor_glint;
	case"rendertype_beacon_beam" ->fragrendertype_beacon_beam.fragrendertype_beacon_beam;
	case"rendertype_crumbling" ->fragrendertype_crumbling.fragrendertype_crumbling;
	case"rendertype_cutout" ->fragrendertype_cutout.fragrendertype_cutout;
	case"rendertype_cutout_mipped" ->fragrendertype_cutout_mipped.fragrendertype_cutout_mipped;
	case"rendertype_end_portal" ->fragrendertype_end_portal.fragrendertype_end_portal;
	case"rendertype_energy_swirl" ->fragrendertype_energy_swirl.fragrendertype_energy_swirl;
	case"rendertype_entity_alpha" ->fragrendertype_entity_alpha.fragrendertype_entity_alpha;
	case"rendertype_entity_cutout" ->fragrendertype_entity_cutout.fragrendertype_entity_cutout;
	case"rendertype_entity_cutout_no_cull" ->fragrendertype_entity_cutout_no_cull.fragrendertype_entity_cutout_no_cull;
	case"rendertype_entity_cutout_no_cull_z_offset" ->fragrendertype_entity_cutout_no_cull_z_offset.fragrendertype_entity_cutout_no_cull_z_offset;
	case"rendertype_entity_decal" ->fragrendertype_entity_decal.fragrendertype_entity_decal;
	case"rendertype_entity_glint" ->fragrendertype_entity_glint.fragrendertype_entity_glint;
	case"rendertype_entity_glint_direct" ->fragrendertype_entity_glint_direct.fragrendertype_entity_glint_direct;
	case"rendertype_entity_no_outline" ->fragrendertype_entity_no_outline.fragrendertype_entity_no_outline;
	case"rendertype_entity_shadow" ->fragrendertype_entity_shadow.fragrendertype_entity_shadow;
	case"rendertype_entity_smooth_cutout" ->fragrendertype_entity_smooth_cutout.fragrendertype_entity_smooth_cutout;
	case"rendertype_entity_solid" ->fragrendertype_entity_solid.fragrendertype_entity_solid;
	case"rendertype_entity_translucent" ->fragrendertype_entity_translucent.fragrendertype_entity_translucent;
	case"rendertype_entity_translucent_cull" ->fragrendertype_entity_translucent_cull.fragrendertype_entity_translucent_cull;
	case"rendertype_eyes" ->fragrendertype_eyes.fragrendertype_eyes;
	case"rendertype_glint" ->fragrendertype_glint.fragrendertype_glint;
	case"rendertype_glint_direct" ->fragrendertype_glint_direct.fragrendertype_glint_direct;
	case"rendertype_glint_translucent" ->fragrendertype_glint_translucent.fragrendertype_glint_translucent;
	case"rendertype_item_entity_translucent_cull" ->fragrendertype_item_entity_translucent_cull.fragrendertype_item_entity_translucent_cull;
	case"rendertype_leash" ->fragrendertype_leash.fragrendertype_leash;
	case"rendertype_lightning" ->fragrendertype_lightning.fragrendertype_lightning;
	case"rendertype_lines" ->fragrendertype_lines.fragrendertype_lines;
	case"rendertype_outline" ->fragrendertype_outline.fragrendertype_outline;
	case"rendertype_solid" ->fragrendertype_solid.fragrendertype_solid;
	case"rendertype_text" ->fragrendertype_text.fragrendertype_text;
	case"rendertype_text_see_through" ->fragrendertype_text_see_through.fragrendertype_text_see_through;
	case"rendertype_translucent" ->fragrendertype_translucent.fragrendertype_translucent;
	case"rendertype_translucent_moving_block" ->fragrendertype_translucent_moving_block.fragrendertype_translucent_moving_block;
	case"rendertype_translucent_no_crumbling" ->fragrendertype_translucent_no_crumbling.fragrendertype_translucent_no_crumbling;
	case"rendertype_tripwire" ->fragrendertype_tripwire.fragrendertype_tripwire;
	case"rendertype_water_mask" ->fragrendertype_water_mask.fragrendertype_water_mask;
	default -> throw new RuntimeException("Fail!: Invalid Shader File!");
};ByteBuffer axl = MemoryUtil.memAlignedAlloc(Integer.SIZE, ax.length*4); //Align to native uint32_t which is the alignment for the vkShaderModuleCreateInfo pCode in the C/C++ API
  axl.asIntBuffer().put(ax);
  currentSize=axl.remaining();
return MemoryUtil.memAddress0(axl);
}
}