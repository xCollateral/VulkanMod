package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Pipeline;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    //TODO: remove class

    @Shadow @Final private Minecraft minecraft;

    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;

    @Shadow @Final private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;

    @Shadow private @Nullable PostChain entityEffect;

    @Shadow private double xTransparentOld;

    @Shadow private double yTransparentOld;

    @Shadow private double zTransparentOld;

    /**
     * @author
     */
    @Overwrite
    public void initOutline() {
        if (this.entityEffect != null) {
            this.entityEffect.close();
        }
//        Identifier identifier = new Identifier("shaders/post/entity_outline.json");
//        try {
//            this.entityOutlineShader = new ShaderEffect(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getFramebuffer(), identifier);
//            this.entityOutlineShader.setupDimensions(this.minecraft.getWindow().getFramebufferWidth(), this.minecraft.getWindow().getFramebufferHeight());
//            this.entityOutlinesFramebuffer = this.entityOutlineShader.getSecondaryTarget("final");
//        }
//        catch (IOException iOException) {
//            LOGGER.warn("Failed to load shader: {}", (Object)identifier, (Object)iOException);
//            this.entityOutlineShader = null;
//            this.entityOutlinesFramebuffer = null;
//        }
//        catch (JsonSyntaxException iOException) {
//            LOGGER.warn("Failed to parse shader: {}", (Object)identifier, (Object)iOException);
//            this.entityOutlineShader = null;
//            this.entityOutlinesFramebuffer = null;
//        }
    }

//    /**
//     * @author
//     */
//    @Overwrite
//    private void renderChunkLayer(RenderType renderType, PoseStack poseStack, double d, double e, double f, Matrix4f matrix4f) {
//        RenderSystem.assertOnRenderThread();
//
//        renderType.setupRenderState();
//        if (renderType == RenderType.translucent()) {
//            this.minecraft.getProfiler().push("translucent_sort");
//            double d0 = d - this.xTransparentOld;
//            double d1 = e - this.yTransparentOld;
//            double d2 = f - this.zTransparentOld;
//            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D) {
//                this.xTransparentOld = d;
//                this.yTransparentOld = e;
//                this.zTransparentOld = f;
//                int j = 0;
//
//                for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
//                    if (j < 15 && levelrenderer$renderchunkinfo.chunk.resortTransparency(renderType, this.chunkRenderDispatcher)) {
//                        ++j;
//                    }
//                }
//            }
//
//            this.minecraft.getProfiler().pop();
//        }
//
//        this.minecraft.getProfiler().push("filterempty");
//        this.minecraft.getProfiler().popPush(() -> {
//            return "render_" + renderType;
//        });
//        boolean flag = renderType != RenderType.translucent();
//        ObjectListIterator<LevelRenderer.RenderChunkInfo> objectlistiterator = this.renderChunksInFrustum.listIterator(flag ? 0 : this.renderChunksInFrustum.size());
//        VertexFormat vertexformat = renderType.format();
//
//        //Vulkan
////      Drawer.setModelViewMatrix(poseStack.last().pose());
////      Drawer.setProjectionMatrix(matrix4f);
////        VRenderSystem.applyModelViewMatrix(poseStack.peek().getModel());
////        VRenderSystem.applyProjectionMatrix(matrix4f);
//        VRenderSystem.applyMVP(poseStack.last().pose(), matrix4f);
//
//
//        Drawer drawer = Drawer.getInstance();
//        Pipeline pipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
//        drawer.bindPipeline(pipeline);
//
//        drawer.uploadAndBindUBOs(pipeline);
//
//
////      ShaderInstance shaderinstance = RenderSystem.getShader();
////      BufferUploader.reset();
////
////      for(int k = 0; k < 12; ++k) {
////         int i = RenderSystem.getShaderTexture(k);
////         shaderinstance.setSampler("Sampler" + k, i);
////      }
////
////      if (shaderinstance.MODEL_VIEW_MATRIX != null) {
////         shaderinstance.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
////      }
////
////      if (shaderinstance.PROJECTION_MATRIX != null) {
////         shaderinstance.PROJECTION_MATRIX.set(matrix4f);
////      }
////
////      if (shaderinstance.COLOR_MODULATOR != null) {
////         shaderinstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
////      }
////
////      if (shaderinstance.FOG_START != null) {
////         shaderinstance.FOG_START.set(RenderSystem.getShaderFogStart());
////      }
////
////      if (shaderinstance.FOG_END != null) {
////         shaderinstance.FOG_END.set(RenderSystem.getShaderFogEnd());
////      }
////
////      if (shaderinstance.FOG_COLOR != null) {
////         shaderinstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
////      }
////
////      if (shaderinstance.TEXTURE_MATRIX != null) {
////         shaderinstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
////      }
////
////      if (shaderinstance.GAME_TIME != null) {
////         shaderinstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
////      }
//
////      RenderSystem.setupShaderLights(shaderinstance);
////      shaderinstance.apply();
////      Uniform uniform = shaderinstance.CHUNK_OFFSET;
//        boolean flag1 = false;
//        int count = 0;
//
//        while(true) {
//            if (flag) {
//                if (!objectlistiterator.hasNext()) {
//                    break;
//                }
//            } else if (!objectlistiterator.hasPrevious()) {
//                break;
//            }
//
//            LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo1 = flag ? objectlistiterator.next() : objectlistiterator.previous();
//            ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo1.chunk;
//            if (!chunkrenderdispatcher$renderchunk.getCompiledChunk().isEmpty(renderType)) {
//                VertexBuffer vertexbuffer = chunkrenderdispatcher$renderchunk.getBuffer(renderType);
//                BlockPos blockpos = chunkrenderdispatcher$renderchunk.getOrigin();
////            if (uniform != null) {
////               uniform.set((float)((double)blockpos.getX() - d), (float)((double)blockpos.getY() - e), (float)((double)blockpos.getZ() - f));
////               uniform.upload();
////            }
//                VRenderSystem.setChunkOffset((float)((double)blockpos.getX() - d), (float)((double)blockpos.getY() - e), (float)((double)blockpos.getZ() - f));
//                drawer.pushConstants(pipeline);
//
//                vertexbuffer.draw();
//                flag1 = true;
//
//                //debug
//                count++;
//            }
//        }
//
////      if (uniform != null) {
////         uniform.set(Vector3f.ZERO);
////      }
//
//        //Need to reset push constant in case the pipeline will still be used for rendering
//        VRenderSystem.setChunkOffset(0, 0, 0);
//        drawer.pushConstants(pipeline);
//
////      shaderinstance.clear();
////      if (flag1) {
////         vertexformat.clearBufferState();
////      }
//
////      VertexBuffer.unbind();
////      VertexBuffer.unbindVertexArray();
//
//        this.minecraft.getProfiler().pop();
//        renderType.clearRenderState();
//
//        VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
//
//    }
}
