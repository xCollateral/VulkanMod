package net.vulkanmod.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.Matrix4f;

public class GuiBatchRenderer extends DrawableHelper {

    public static void blit(MatrixStack p_93201_, int p_93202_, int p_93203_, int p_93204_, int p_93205_, int p_93206_, Sprite sprite) {
        innerBlit(p_93201_.peek().getPositionMatrix(), p_93202_, p_93202_ + p_93205_, p_93203_, p_93203_ + p_93206_, p_93204_, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
    }

    public static void blit(MatrixStack p_93229_, InGameHud gui, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_) {
        blit(p_93229_, p_93230_, p_93231_, gui.getZOffset(), (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
    }

    public static void blit(MatrixStack p_93229_, int zOffset, int p_93230_, int p_93231_, int p_93232_, int p_93233_, int p_93234_, int p_93235_) {
        blit(p_93229_, p_93230_, p_93231_, zOffset, (float)p_93232_, (float)p_93233_, p_93234_, p_93235_, 256, 256);
    }

    public static void blit(MatrixStack p_93144_, int p_93145_, int p_93146_, int p_93147_, float p_93148_, float p_93149_, int p_93150_, int p_93151_, int p_93152_, int p_93153_) {
        innerBlit(p_93144_, p_93145_, p_93145_ + p_93150_, p_93146_, p_93146_ + p_93151_, p_93147_, p_93150_, p_93151_, p_93148_, p_93149_, p_93153_, p_93152_);
    }

    public static void blit(MatrixStack p_93161_, int p_93162_, int p_93163_, int p_93164_, int p_93165_, float p_93166_, float p_93167_, int p_93168_, int p_93169_, int p_93170_, int p_93171_) {
        innerBlit(p_93161_, p_93162_, p_93162_ + p_93164_, p_93163_, p_93163_ + p_93165_, 0, p_93168_, p_93169_, p_93166_, p_93167_, p_93170_, p_93171_);
    }

    public static void blit(MatrixStack p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_) {
        blit(p_93134_, p_93135_, p_93136_, p_93139_, p_93140_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
    }

    private static void innerBlit(MatrixStack p_93188_, int p_93189_, int p_93190_, int p_93191_, int p_93192_, int p_93193_, int p_93194_, int p_93195_, float p_93196_, float p_93197_, int p_93198_, int p_93199_) {
        innerBlit(p_93188_.peek().getPositionMatrix(), p_93189_, p_93190_, p_93191_, p_93192_, p_93193_, (p_93196_ + 0.0F) / (float)p_93198_, (p_93196_ + (float)p_93194_) / (float)p_93198_, (p_93197_ + 0.0F) / (float)p_93199_, (p_93197_ + (float)p_93195_) / (float)p_93199_);
    }

    public static void innerBlit(Matrix4f matrix4f, int x1, int x2, int y1, int y2, int z, float uv1, float uv2, float uv3, float uv4) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();

        bufferbuilder.vertex(matrix4f, (float)x1, (float)y2, (float)z).texture(uv1, uv4).next();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y2, (float)z).texture(uv2, uv4).next();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y1, (float)z).texture(uv2, uv3).next();
        bufferbuilder.vertex(matrix4f, (float)x1, (float)y1, (float)z).texture(uv1, uv3).next();

    }

    public static void fill(MatrixStack p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        innerFill(p_93173_.peek().getPositionMatrix(), p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
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
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();

        bufferbuilder.vertex(p_93106_, (float)p_93107_, (float)p_93110_, 0.0F).color(f, f1, f2, f3).next();
        bufferbuilder.vertex(p_93106_, (float)p_93109_, (float)p_93110_, 0.0F).color(f, f1, f2, f3).next();
        bufferbuilder.vertex(p_93106_, (float)p_93109_, (float)p_93108_, 0.0F).color(f, f1, f2, f3).next();
        bufferbuilder.vertex(p_93106_, (float)p_93107_, (float)p_93108_, 0.0F).color(f, f1, f2, f3).next();

    }

    public static int drawShadow(TextRenderer font, VertexConsumerProvider bufferSource, MatrixStack p_92745_, OrderedText p_92746_, float p_92747_, float p_92748_, int p_92749_) {
        return drawInternal(font, bufferSource, p_92746_, p_92747_, p_92748_, p_92749_, p_92745_.peek().getPositionMatrix(), true);
    }

    public static int drawString(TextRenderer font, VertexConsumerProvider bufferSource, MatrixStack p_92884_, String p_92885_, float p_92886_, float p_92887_, int p_92888_) {
        return drawInternal(font, bufferSource, p_92885_, p_92886_, p_92887_, p_92888_, p_92884_.peek().getPositionMatrix(), false, font.isRightToLeft());
    }

    private static int drawInternal(TextRenderer font, VertexConsumerProvider bufferSource, OrderedText p_92727_, float p_92728_, float p_92729_, int p_92730_, Matrix4f p_92731_, boolean p_92732_) {
        int i = font.draw(p_92727_, p_92728_, p_92729_, p_92730_, p_92732_, p_92731_, bufferSource, false, 0, 15728880);

        return i;
    }

    private static int drawInternal(TextRenderer font, VertexConsumerProvider bufferSource, String p_92804_, float p_92805_, float p_92806_, int p_92807_, Matrix4f matrix4f, boolean p_92809_, boolean p_92810_) {
        if (p_92804_ == null) {
            return 0;
        } else {
            int i = font.draw(p_92804_, p_92805_, p_92806_, p_92807_, p_92809_, matrix4f, bufferSource, false, 0, 15728880, p_92810_);

            return i;
        }
    }

    public static void beginBatch(VertexFormat.DrawMode mode, VertexFormat format) {
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(mode, format);
    }

    public static void endBatch() {
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.end();
        BufferRenderer.draw(bufferbuilder);
    }
}

