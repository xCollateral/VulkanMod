package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBorderRenderer.class)
public class ChunkBorderRendererM {

    @Shadow @Final private Minecraft minecraft;

    @Shadow @Final private static int CELL_BORDER;

    @Shadow @Final private static int YELLOW;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
        PoseStack poseStack2 = RenderSystem.getModelViewStack();
        poseStack2.pushPose();
        poseStack2.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Entity entity = this.minecraft.gameRenderer.getMainCamera().getEntity();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        double g = (double)this.minecraft.level.getMinBuildHeight() - e;
        double h = (double)this.minecraft.level.getMaxBuildHeight() - e;
//        RenderSystem.disableBlend();
        ChunkPos chunkPos = entity.chunkPosition();
        double i = (double)chunkPos.getMinBlockX() - d;
        double j = (double)chunkPos.getMinBlockZ() - f;
        RenderSystem.lineWidth(1.0F);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        RenderSystem.disableCull();
        float lw = 0.1f;

        double h0 = ((int)e / 16) * 16 + (this.minecraft.level.getMinBuildHeight() % 16) - e;
//        double h0 = this.minecraft.level.getMinBuildHeight() - e ;

        int k;
        int l;
        for(k = -16; k <= 16; k += 16) {
            for(l = -16; l <= 16; l += 16) {
//                bufferBuilder.vertex(i + (double)k, g, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();
//                bufferBuilder.vertex(i + (double)k, g, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
//                bufferBuilder.vertex(i + (double)k, h, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
//                bufferBuilder.vertex(i + (double)k, h, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();

                bufferBuilder.vertex(i + (double)k, g, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();
                bufferBuilder.vertex(i + (double)k, h, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                bufferBuilder.vertex(i + (double)k + lw, g, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                bufferBuilder.vertex(i + (double)k + lw, h, j + (double)l).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();
            }
        }

        for(int i1 = -2; i1 < 3; ++i1) {
            double hr = h0 + 16 * i1;

            bufferBuilder.vertex(i, hr, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i + 16, hr, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i + 16, hr + lw, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i, hr + lw , j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();

            bufferBuilder.vertex(i, hr, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i, hr, j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i, hr + lw, j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i, hr + lw , j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();

            bufferBuilder.vertex(i, hr, j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i + 16, hr, j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i + 16, hr + lw, j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i, hr + lw , j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();

            bufferBuilder.vertex(i + 16, hr, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i + 16, hr, j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i + 16, hr + lw, j + 16).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            bufferBuilder.vertex(i + 16, hr + lw , j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();

//            bufferBuilder.vertex(i + 16.0, m, j + 16.0).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i + 16.0, m, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i, m, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i, m, j).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
        }

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        float a = 0.3f;

        bufferBuilder.vertex(i, h0, j).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i + 16, h0, j).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i + 16, h0 + 16, j).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i, h0 + 16, j).color(0.25F, 0.25F, 0.1F, a).endVertex();

        bufferBuilder.vertex(i, h0, j + 16).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i + 16, h0, j + 16).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i + 16, h0 + 16, j + 16).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i, h0 + 16, j + 16).color(0.25F, 0.25F, 0.1F, a).endVertex();

        bufferBuilder.vertex(i, h0, j).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i, h0, j + 16).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i, h0 + 16, j + 16).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i, h0 + 16, j).color(0.25F, 0.25F, 0.1F, a).endVertex();

        bufferBuilder.vertex(i + 16, h0, j).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i + 16, h0, j + 16).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i + 16, h0 + 16, j + 16).color(0.3F, 0.25F, 0.1F, a).endVertex();
        bufferBuilder.vertex(i + 16, h0 + 16, j).color(0.25F, 0.25F, 0.1F, a).endVertex();

//        for(k = 2; k < 16; k += 2) {
//            l = k % 4 == 0 ? CELL_BORDER : YELLOW;
//            bufferBuilder.vertex(i + (double)k, g, j).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i + (double)k, g, j).color(l).endVertex();
//            bufferBuilder.vertex(i + (double)k, h, j).color(l).endVertex();
//            bufferBuilder.vertex(i + (double)k, h, j).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i + (double)k, g, j + 16.0).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i + (double)k, g, j + 16.0).color(l).endVertex();
//            bufferBuilder.vertex(i + (double)k, h, j + 16.0).color(l).endVertex();
//            bufferBuilder.vertex(i + (double)k, h, j + 16.0).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//        }
//
//        for(k = 2; k < 16; k += 2) {
//            l = k % 4 == 0 ? CELL_BORDER : YELLOW;
//            bufferBuilder.vertex(i, g, j + (double)k).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i, g, j + (double)k).color(l).endVertex();
//            bufferBuilder.vertex(i, h, j + (double)k).color(l).endVertex();
//            bufferBuilder.vertex(i, h, j + (double)k).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i + 16.0, g, j + (double)k).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i + 16.0, g, j + (double)k).color(l).endVertex();
//            bufferBuilder.vertex(i + 16.0, h, j + (double)k).color(l).endVertex();
//            bufferBuilder.vertex(i + 16.0, h, j + (double)k).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//        }
//
//        double m;
//        for(k = this.minecraft.level.getMinBuildHeight(); k <= this.minecraft.level.getMaxBuildHeight(); k += 2) {
//            m = (double)k - e;
//            int n = k % 8 == 0 ? CELL_BORDER : YELLOW;
//            bufferBuilder.vertex(i, m, j).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i, m, j).color(n).endVertex();
//            bufferBuilder.vertex(i, m, j + 16.0).color(n).endVertex();
//            bufferBuilder.vertex(i + 16.0, m, j + 16.0).color(n).endVertex();
//            bufferBuilder.vertex(i + 16.0, m, j).color(n).endVertex();
//            bufferBuilder.vertex(i, m, j).color(n).endVertex();
//            bufferBuilder.vertex(i, m, j).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
//        }
//
        tesselator.end();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(2.0F);
//        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
//
//        for(k = 0; k <= 16; k += 16) {
//            for(l = 0; l <= 16; l += 16) {
//                bufferBuilder.vertex(i + (double)k, g, j + (double)l).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
//                bufferBuilder.vertex(i + (double)k, g, j + (double)l).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//                bufferBuilder.vertex(i + (double)k, h, j + (double)l).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//                bufferBuilder.vertex(i + (double)k, h, j + (double)l).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
//            }
//        }
//
//        for(k = this.minecraft.level.getMinBuildHeight(); k <= this.minecraft.level.getMaxBuildHeight(); k += 16) {
//            m = (double)k - e;
//            bufferBuilder.vertex(i, m, j).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
//            bufferBuilder.vertex(i, m, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i, m, j + 16.0).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i + 16.0, m, j + 16.0).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i + 16.0, m, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i, m, j).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
//            bufferBuilder.vertex(i, m, j).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
//        }
//
//        tesselator.end();
//        RenderSystem.lineWidth(1.0F);
//        RenderSystem.enableBlend();
//        RenderSystem.enableTexture();

        RenderSystem.depthMask(true);
        poseStack2.popPose();
        RenderSystem.applyModelViewMatrix();
    }
}
