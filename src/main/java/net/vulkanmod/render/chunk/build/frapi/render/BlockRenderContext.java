/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.vulkanmod.render.chunk.build.frapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.build.frapi.mesh.MutableQuadViewImpl;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.ArrayLightDataCache;
import net.vulkanmod.render.chunk.build.light.flat.FlatLightPipeline;
import net.vulkanmod.render.chunk.build.light.smooth.NewSmoothLightPipeline;
import net.vulkanmod.render.chunk.build.light.smooth.SmoothLightPipeline;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractBlockRenderContext {
	private VertexConsumer vertexConsumer;

	private final ArrayLightDataCache lightDataCache = new ArrayLightDataCache();

	public BlockRenderContext() {
		LightPipeline flatLightPipeline = new FlatLightPipeline(this.lightDataCache);

		LightPipeline smoothLightPipeline;
		if (Initializer.CONFIG.ambientOcclusion == LightMode.SUB_BLOCK) {
			smoothLightPipeline = new NewSmoothLightPipeline(lightDataCache);
		}
		else {
			smoothLightPipeline = new SmoothLightPipeline(lightDataCache);
		}

		this.setupLightPipelines(flatLightPipeline, smoothLightPipeline);
    }

	public void render(BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack matrixStack, VertexConsumer buffer, boolean cull, RandomSource random, long seed, int overlay) {
		try {
			Vec3 offset = state.getOffset(blockView, pos);
			matrixStack.translate(offset.x, offset.y, offset.z);

			this.blockPos = pos;
			this.vertexConsumer = buffer;
			this.matrix = matrixStack.last().pose();
			this.normalMatrix = matrixStack.last().normal();
			this.overlay = overlay;

			this.random = random;
			this.seed = seed;

			this.lightDataCache.reset(blockView, pos);

			this.prepareForWorld(blockView, cull);
			this.prepareForBlock(state, pos, model.useAmbientOcclusion());

			model.emitBlockQuads(blockView, state, pos, this.randomSupplier, this);
		} catch (Throwable throwable) {
			CrashReport crashReport = CrashReport.forThrowable(throwable, "Tessellating block model - Indigo Renderer");
			CrashReportCategory crashReportSection = crashReport.addCategory("Block model being tessellated");
			CrashReportCategory.populateBlockDetails(crashReportSection, blockView, pos, state);
			throw new ReportedException(crashReport);
		} finally {
			this.vertexConsumer = null;
		}
	}

	protected void endRenderQuad(MutableQuadViewImpl quad) {
		final RenderMaterial mat = quad.material();
		final int colorIndex = mat.disableColorIndex() ? -1 : quad.colorIndex();
		final TriState aoMode = mat.ambientOcclusion();
		final boolean ao = this.useAO && (aoMode == TriState.TRUE || (aoMode == TriState.DEFAULT && this.defaultAO));
		final boolean emissive = mat.emissive();
		final boolean vanillaShade = mat.shadeMode() == ShadeMode.VANILLA;

		LightPipeline lightPipeline = ao ? this.smoothLightPipeline : this.flatLightPipeline;

		colorizeQuad(quad, colorIndex);
		shadeQuad(quad, lightPipeline, emissive, vanillaShade);
        bufferQuad(quad, vertexConsumer);
	}

}
