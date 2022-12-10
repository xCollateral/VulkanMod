package net.vulkanmod.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FormattedCharSequence;

public class GuiBatchRenderer extends GuiComponent {

    public static void blit(PoseStack p_93201_, int p_93202_, int p_93203_, int p_93204_, int p_93205_, int p_93206_, TextureAtlasSprite sprite) {
        innerBlit(p_93201_.last().pose(), p_93202_, p_93202_ + p_93205_, p_93203_, p_93203_ + p_93206_, p_93204_, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }

    public static void blit(PoseStack p_93229_, Gui gui, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_) {
        blit(p_93229_, p_93230_, p_93231_, gui.getBlitOffset(), (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
    }

    public static void blit(PoseStack p_93229_, int zOffset, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_) {
        blit(p_93229_, p_93230_, p_93231_, zOffset, (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
    }

    public static void blit(PoseStack p_93144_, int p_93145_, int p_93146_, int p_93147_, float p_93148_, float p_93149_, int p_93150_, int p_93151_, int p_93152_, int p_93153_) {
        innerBlit(p_93144_, p_93145_, p_93145_ + p_93150_, p_93146_, p_93146_ + p_93151_, p_93147_, p_93150_, p_93151_, p_93148_, p_93149_, p_93153_, p_93152_);
    }

    public static void blit(PoseStack p_93161_, int p_93162_, int p_93163_, int p_93164_, int p_93165_, float p_93166_, float p_93167_, int p_93168_, int p_93169_, int p_93170_, int p_93171_) {
        innerBlit(p_93161_, p_93162_, p_93162_ + p_93164_, p_93163_, p_93163_ + p_93165_, 0, p_93168_, p_93169_, p_93166_, p_93167_, p_93170_, p_93171_);
    }

    public static void blit(Matrix4f p_93161_, int p_93162_, int p_93163_, int p_93164_, int p_93165_, float p_93166_, float p_93167_, int p_93168_, int p_93169_, int p_93170_, int p_93171_) {
        innerBlit(p_93161_, p_93162_, p_93162_ + p_93164_, p_93163_, p_93163_ + p_93165_, 0, p_93168_, p_93169_, p_93166_, p_93167_, p_93170_, p_93171_);
    }

    public static void blit(PoseStack p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_) {
        blit(p_93134_, p_93135_, p_93136_, p_93139_, p_93140_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
    }

    public static void blit(Matrix4f p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_) {
        blit(p_93134_, p_93135_, p_93136_, p_93139_, p_93140_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
    }

    private static void innerBlit(PoseStack p_93188_, int p_93189_, int p_93190_, int p_93191_, int p_93192_, int p_93193_, int p_93194_, int p_93195_, float p_93196_, float p_93197_, int p_93198_, int p_93199_) {
        innerBlit(p_93188_.last().pose(), p_93189_, p_93190_, p_93191_, p_93192_, p_93193_, (p_93196_ + 0.0F) / (float)p_93198_, (p_93196_ + (float)p_93194_) / (float)p_93198_, (p_93197_ + 0.0F) / (float)p_93199_, (p_93197_ + (float)p_93195_) / (float)p_93199_);
    }

    private static void innerBlit(Matrix4f p_93188_, int p_93189_, int p_93190_, int p_93191_, int p_93192_, int p_93193_, int p_93194_, int p_93195_, float p_93196_, float p_93197_, int p_93198_, int p_93199_) {
        innerBlit(p_93188_, p_93189_, p_93190_, p_93191_, p_93192_, p_93193_, (p_93196_ + 0.0F) / (float)p_93198_, (p_93196_ + (float)p_93194_) / (float)p_93198_, (p_93197_ + 0.0F) / (float)p_93199_, (p_93197_ + (float)p_93195_) / (float)p_93199_);
    }

    public static void innerBlit(Matrix4f matrix4f, int x1, int x2, int y1, int y2, int z, float uv1, float uv2, float uv3, float uv4) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();

        bufferbuilder.vertex(matrix4f, (float)x1, (float)y2, (float)z).uv(uv1, uv4).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y2, (float)z).uv(uv2, uv4).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y1, (float)z).uv(uv2, uv3).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x1, (float)y1, (float)z).uv(uv1, uv3).endVertex();

    }

    public static void fill(PoseStack p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        innerFill(p_93173_.last().pose(), p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
    }

    public static void fill(Matrix4f p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        innerFill(p_93173_, p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
    }

    private static void innerFill(Matrix4f p_93106_, int p_93107_, int p_93108_, int p_93109_, int p_93110_, int p_93111_) {
        if (p_93107_ < p_93109_) {
            int i = p_93107_;
            p_93107_ = p_93109_;
            p_93109_ = i;
        }

        if (p_93108_ < p_93110_) {
            int j = p_93108_;
            p_93108_ = p_93110_;
            p_93110_ = j;
        }

        float f3 = (float)(p_93111_ >> 24 & 255) / 255.0F;
        float f = (float)(p_93111_ >> 16 & 255) / 255.0F;
        float f1 = (float)(p_93111_ >> 8 & 255) / 255.0F;
        float f2 = (float)(p_93111_ & 255) / 255.0F;
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();

        bufferbuilder.vertex(p_93106_, (float)p_93107_, (float)p_93110_, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(p_93106_, (float)p_93109_, (float)p_93110_, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(p_93106_, (float)p_93109_, (float)p_93108_, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(p_93106_, (float)p_93107_, (float)p_93108_, 0.0F).color(f, f1, f2, f3).endVertex();

    }

    public static int drawShadow(Font font, MultiBufferSource bufferSource, PoseStack p_92745_, FormattedCharSequence p_92746_, float p_92747_, float p_92748_, int p_92749_) {
        return drawInternal(font, bufferSource, p_92746_, p_92747_, p_92748_, p_92749_, p_92745_.last().pose(), true);
    }

    public static int drawShadow(Font font, MultiBufferSource bufferSource, Matrix4f p_92745_, FormattedCharSequence p_92746_, float p_92747_, float p_92748_, int p_92749_) {
        return drawInternal(font, bufferSource, p_92746_, p_92747_, p_92748_, p_92749_, p_92745_, true);
    }

    public static int drawString(Font font, MultiBufferSource bufferSource, PoseStack p_92884_, String p_92885_, float p_92886_, float p_92887_, int p_92888_) {
        return drawInternal(font, bufferSource, p_92885_, p_92886_, p_92887_, p_92888_, p_92884_.last().pose(), false, font.isBidirectional());
    }

    private static int drawInternal(Font font, MultiBufferSource bufferSource, FormattedCharSequence p_92727_, float p_92728_, float p_92729_, int p_92730_, Matrix4f p_92731_, boolean p_92732_) {
        int i = font.drawInBatch(p_92727_, p_92728_, p_92729_, p_92730_, p_92732_, p_92731_, bufferSource, false, 0, 15728880);

        return i;
    }

    private static int drawInternal(Font font, MultiBufferSource bufferSource, String p_92804_, float p_92805_, float p_92806_, int p_92807_, Matrix4f matrix4f, boolean p_92809_, boolean p_92810_) {
        if (p_92804_ == null) {
            return 0;
        } else {
            int i = font.drawInBatch(p_92804_, p_92805_, p_92806_, p_92807_, p_92809_, matrix4f, bufferSource, false, 0, 15728880, p_92810_);

            return i;
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

