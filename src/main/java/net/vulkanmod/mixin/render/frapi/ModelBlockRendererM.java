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

package net.vulkanmod.mixin.render.frapi;

import net.vulkanmod.render.chunk.build.frapi.render.BlockRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererM {

    // TODO ThreadLocal look ups are slow, same goes for ItemRendererMixin
    @Unique
    private final ThreadLocal<BlockRenderContext> fabric_contexts = ThreadLocal.withInitial(BlockRenderContext::new);

    @Inject(at = @At("HEAD"), method = "tesselateBlock", cancellable = true)
    private void hookRender(BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack matrix, VertexConsumer buffer, boolean cull, RandomSource rand, long seed, int overlay, CallbackInfo ci) {
        if (!model.isVanillaAdapter()) {
            BlockRenderContext context = fabric_contexts.get();
            context.render(blockView, model, state, pos, matrix, buffer, cull, rand, seed, overlay);
            ci.cancel();
        }
    }

}
