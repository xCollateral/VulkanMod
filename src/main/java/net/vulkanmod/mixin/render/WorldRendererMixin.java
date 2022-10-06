package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.render.*;
import net.vulkanmod.interfaces.ShaderMixed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Pipeline;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow private @Nullable ShaderEffect entityOutlineShader;

    @Shadow @Final private MinecraftClient client;

    @Shadow private double lastTranslucentSortX;

    @Shadow private double lastTranslucentSortY;

    @Shadow private double lastTranslucentSortZ;

    @Shadow private ChunkBuilder chunkBuilder;

    @Shadow @Final private ObjectArrayList<WorldRenderer.ChunkInfo> chunkInfos;

    /**
     * @author
     */
    @Overwrite
    public void loadEntityOutlineShader() {
        if (this.entityOutlineShader != null) {
            this.entityOutlineShader.close();
        }
//        Identifier identifier = new Identifier("shaders/post/entity_outline.json");
//        try {
//            this.entityOutlineShader = new ShaderEffect(this.client.getTextureManager(), this.client.getResourceManager(), this.client.getFramebuffer(), identifier);
//            this.entityOutlineShader.setupDimensions(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
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

    /**
     * @author
     */
    @Overwrite
    private void renderLayer(RenderLayer renderType, MatrixStack poseStack, double d, double e, double f, Matrix4f matrix4f) {
        RenderSystem.assertOnRenderThread();

        renderType.startDrawing();
        if (renderType == RenderLayer.getTranslucent()) {
            this.client.getProfiler().push("translucent_sort");
            double d0 = d - this.lastTranslucentSortX;
            double d1 = e - this.lastTranslucentSortY;
            double d2 = f - this.lastTranslucentSortZ;
            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D) {
                this.lastTranslucentSortX = d;
                this.lastTranslucentSortY = e;
                this.lastTranslucentSortZ = f;
                int j = 0;

                for(WorldRenderer.ChunkInfo levelrenderer$renderchunkinfo : this.chunkInfos) {
                    if (j < 15 && levelrenderer$renderchunkinfo.chunk.scheduleSort(renderType, this.chunkBuilder)) {
                        ++j;
                    }
                }
            }

            this.client.getProfiler().pop();
        }

        this.client.getProfiler().push("filterempty");
        this.client.getProfiler().swap(() -> {
            return "render_" + renderType;
        });
        boolean flag = renderType != RenderLayer.getTranslucent();
        ObjectListIterator<WorldRenderer.ChunkInfo> objectlistiterator = this.chunkInfos.listIterator(flag ? 0 : this.chunkInfos.size());
        VertexFormat vertexformat = renderType.getVertexFormat();

        //Vulkan
//      Drawer.setModelViewMatrix(poseStack.last().pose());
//      Drawer.setProjectionMatrix(matrix4f);
//        VRenderSystem.applyModelViewMatrix(poseStack.peek().getModel());
//        VRenderSystem.applyProjectionMatrix(matrix4f);
        VRenderSystem.applyMVP(poseStack.peek().getPositionMatrix(), matrix4f);


        Drawer drawer = Drawer.getInstance();
        Pipeline pipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
        drawer.bindPipeline(pipeline);

        drawer.uploadAndBindUBOs(pipeline);


//      ShaderInstance shaderinstance = RenderSystem.getShader();
//      BufferUploader.reset();
//
//      for(int k = 0; k < 12; ++k) {
//         int i = RenderSystem.getShaderTexture(k);
//         shaderinstance.setSampler("Sampler" + k, i);
//      }
//
//      if (shaderinstance.MODEL_VIEW_MATRIX != null) {
//         shaderinstance.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
//      }
//
//      if (shaderinstance.PROJECTION_MATRIX != null) {
//         shaderinstance.PROJECTION_MATRIX.set(matrix4f);
//      }
//
//      if (shaderinstance.COLOR_MODULATOR != null) {
//         shaderinstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
//      }
//
//      if (shaderinstance.FOG_START != null) {
//         shaderinstance.FOG_START.set(RenderSystem.getShaderFogStart());
//      }
//
//      if (shaderinstance.FOG_END != null) {
//         shaderinstance.FOG_END.set(RenderSystem.getShaderFogEnd());
//      }
//
//      if (shaderinstance.FOG_COLOR != null) {
//         shaderinstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
//      }
//
//      if (shaderinstance.TEXTURE_MATRIX != null) {
//         shaderinstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
//      }
//
//      if (shaderinstance.GAME_TIME != null) {
//         shaderinstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
//      }

//      RenderSystem.setupShaderLights(shaderinstance);
//      shaderinstance.apply();
//      Uniform uniform = shaderinstance.CHUNK_OFFSET;
        boolean flag1 = false;
        int count = 0;

        while(true) {
            if (flag) {
                if (!objectlistiterator.hasNext()) {
                    break;
                }
            } else if (!objectlistiterator.hasPrevious()) {
                break;
            }

            WorldRenderer.ChunkInfo levelrenderer$renderchunkinfo1 = flag ? objectlistiterator.next() : objectlistiterator.previous();
            ChunkBuilder.BuiltChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo1.chunk;
            if (!chunkrenderdispatcher$renderchunk.getData().isEmpty(renderType)) {
                VertexBuffer vertexbuffer = chunkrenderdispatcher$renderchunk.getBuffer(renderType);
                BlockPos blockpos = chunkrenderdispatcher$renderchunk.getOrigin();
//            if (uniform != null) {
//               uniform.set((float)((double)blockpos.getX() - d), (float)((double)blockpos.getY() - e), (float)((double)blockpos.getZ() - f));
//               uniform.upload();
//            }
                VRenderSystem.setChunkOffset((float)((double)blockpos.getX() - d), (float)((double)blockpos.getY() - e), (float)((double)blockpos.getZ() - f));
                drawer.pushConstants(pipeline);

                vertexbuffer.drawVertices();
                flag1 = true;

                //debug
                count++;
            }
        }

//      if (uniform != null) {
//         uniform.set(Vector3f.ZERO);
//      }

        //Need to reset push constant in case the pipeline will still be used for rendering
        VRenderSystem.setChunkOffset(0, 0, 0);
        drawer.pushConstants(pipeline);

//      shaderinstance.clear();
//      if (flag1) {
//         vertexformat.clearBufferState();
//      }


//      VertexBuffer.unbind();
//      VertexBuffer.unbindVertexArray();
        this.client.getProfiler().pop();
        renderType.endDrawing();

        VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

    }
}
