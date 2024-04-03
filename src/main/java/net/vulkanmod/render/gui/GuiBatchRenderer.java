package net.vulkanmod.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

// TODO: needs refactor
public class GuiBatchRenderer {

//    public static void blit(PoseStack p_93201_, int p_93202_, int p_93203_, int p_93204_, int p_93205_, int p_93206_, TextureAtlasSprite sprite) {
//        innerBlit(p_93201_.last().pose(), p_93202_, p_93202_ + p_93205_, p_93203_, p_93203_ + p_93206_, p_93204_, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
//    }
//
//    public static void blit(PoseStack p_93229_, Gui gui, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_) {
//        blit(p_93229_, p_93230_, p_93231_, gui.getBlitOffset(), (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
//    }
//
//    public static void blit(PoseStack p_93229_, int zOffset, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_) {
//        blit(p_93229_, p_93230_, p_93231_, zOffset, (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
//    }

    public static void blit(PoseStack p_93144_, int p_93145_, int p_93146_, int p_93147_, float p_93148_, float p_93149_, int p_93150_, int p_93151_, int p_93152_, int p_93153_) {
        innerBlit(p_93144_, p_93145_, p_93145_ + p_93150_, p_93146_, p_93146_ + p_93151_, p_93147_, p_93150_, p_93151_, p_93148_, p_93149_, p_93153_, p_93152_);
    }

    public static void blit(PoseStack p_93161_, int p_93162_, int p_93163_, int p_93164_, int p_93165_, float p_93166_, float p_93167_, int p_93168_, int p_93169_, int p_93170_, int p_93171_) {
        innerBlit(p_93161_, p_93162_, p_93162_ + p_93164_, p_93163_, p_93163_ + p_93165_, 0, p_93168_, p_93169_, p_93166_, p_93167_, p_93170_, p_93171_);
    }

    public static void blit(Matrix4f matrix4f, int x0, int y0, int width, int height, float u, float v, int u_width, int v_height, int p_93170_, int p_93171_) {
        innerBlit(matrix4f, x0, x0 + width, y0, y0 + height, 0, u_width, v_height, u, v, p_93170_, p_93171_);
    }

    public static void blit(PoseStack p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_) {
        blit(p_93134_, p_93135_, p_93136_, p_93139_, p_93140_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
    }

    public static void blit(Matrix4f matrix4f, int x0, int y0, float u, float v, int width, int height, int p_93141_, int p_93142_) {
        blit(matrix4f, x0, y0, width, height, u, v, width, height, p_93141_, p_93142_);
    }

    private static void innerBlit(PoseStack poseStack, int p_93189_, int p_93190_, int p_93191_, int p_93192_, int p_93193_, int p_93194_, int p_93195_, float p_93196_, float p_93197_, int p_93198_, int p_93199_) {
        innerBlit(poseStack.last().pose(), p_93189_, p_93190_, p_93191_, p_93192_, p_93193_, (p_93196_ + 0.0F) / (float)p_93198_, (p_93196_ + (float)p_93194_) / (float)p_93198_, (p_93197_ + 0.0F) / (float)p_93199_, (p_93197_ + (float)p_93195_) / (float)p_93199_);
    }

    private static void innerBlit(Matrix4f matrix4f, int x1, int x2, int y1, int y2, int z, int p_93194_, int p_93195_, float u, float v, int p_93198_, int p_93199_) {
        innerBlit(matrix4f, x1, x2, y1, y2, z, (u + 0.0F) / (float)p_93198_, (u + (float)p_93194_) / (float)p_93198_, (v + 0.0F) / (float)p_93199_, (v + (float)p_93195_) / (float)p_93199_);
    }

    public static void innerBlit(Matrix4f matrix4f, int x1, int x2, int y1, int y2, int z, float uv1, float uv2, float uv3, float uv4) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();

        bufferbuilder.vertex(matrix4f, (float)x1, (float)y2, (float)z).uv(uv1, uv4).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y2, (float)z).uv(uv2, uv4).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y1, (float)z).uv(uv2, uv3).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x1, (float)y1, (float)z).uv(uv1, uv3).endVertex();

    }

    public static void fill(PoseStack poseStack, int x1, int y1, int x2, int y2, int color) {
        innerFill(poseStack.last().pose(), x1, y1, x2, y2, color);
    }

    public static void fill(Matrix4f matrix4f, int x1, int y1, int x2, int y2, int color) {
        innerFill(matrix4f, x1, y1, x2, y2, color);
    }

    private static void innerFill(Matrix4f matrix4f, int x1, int y1, int x2, int y2, int color) {
        if (x1 < x2) {
            int i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            int j = y1;
            y1 = y2;
            y2 = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();

        bufferbuilder.vertex(matrix4f, (float)x1, (float)y2, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y2, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y1, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x1, (float)y1, 0.0F).color(f, f1, f2, f3).endVertex();

    }

    public static int drawTextShadowed(Font font, MultiBufferSource bufferSource, PoseStack poseStack, FormattedCharSequence charSequence, float x, float y, int intensity) {
        return drawInternal(font, bufferSource, charSequence, x, y, intensity, poseStack.last().pose(), true);
    }

    public static int drawTextShadowed(Font font, MultiBufferSource bufferSource, Matrix4f matrix4f, FormattedCharSequence charSequence, float x, float y, int intensity) {
        return drawInternal(font, bufferSource, charSequence, x, y, intensity, matrix4f, true);
    }

    public static int drawString(Font font, MultiBufferSource bufferSource, PoseStack poseStack, String string, float x, float y, int intensity) {
        return drawInternal(font, bufferSource, string, x, y, intensity, poseStack.last().pose(), false, font.isBidirectional());
    }

    private static int drawInternal(Font font, MultiBufferSource bufferSource, FormattedCharSequence formattedCharSequence, float x, float y, int intensity, Matrix4f matrix4f, boolean shadow) {
        int j = font.drawInBatch(formattedCharSequence, x, y, intensity, shadow, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

        return j;
    }

    private static int drawInternal(Font font, MultiBufferSource bufferSource, String string, float f, float g, int i, Matrix4f matrix4f, boolean bl, boolean bl2) {
        if (string == null) {
            return 0;
        } else {
            int i1 = font.drawInBatch(string, f, g, i, bl, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880, bl2);

            return i1;
        }
    }

    public static void beginBatch(VertexFormat.Mode mode, VertexFormat format) {
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(mode, format);
    }

    public static void endBatch() {
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        BufferBuilder.RenderedBuffer builtBuffer = bufferbuilder.end();
        BufferUploader.drawWithShader(builtBuffer);
    }
}

