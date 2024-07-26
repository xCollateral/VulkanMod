package net.vulkanmod.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

public class GuiBatchRenderer {

    public static void blit(PoseStack poseStack, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        innerBlit(poseStack.last().pose(), x, x + width, y, y + height, z, width, height, u, v, textureWidth, textureHeight);
    }

    public static void blit(Matrix4f matrix, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        innerBlit(matrix, x, x + width, y, y + height, 0, width, height, u, v, textureWidth, textureHeight);
    }

    private static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int z, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        float u1 = (u + 0.0F) / textureWidth;
        float u2 = (u + width) / textureWidth;
        float v1 = (v + 0.0F) / textureHeight;
        float v2 = (v + height) / textureHeight;
        renderQuad(matrix, x1, x2, y1, y2, z, u1, u2, v1, v2);
    }

    private static void renderQuad(Matrix4f matrix, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.vertex(matrix, (float) x1, (float) y2, (float) z).uv(u1, v2).color(255).endVertex();
        bufferBuilder.vertex(matrix, (float) x2, (float) y2, (float) z).uv(u2, v2).color(255).endVertex();
        bufferBuilder.vertex(matrix, (float) x2, (float) y1, (float) z).uv(u2, v1).color(255).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).uv(u1, v1).color(255).endVertex();
    }

    public static void fill(PoseStack poseStack, int x1, int y1, int x2, int y2, int color) {
        innerFill(poseStack.last().pose(), x1, y1, x2, y2, color);
    }

    public static void fill(Matrix4f matrix, int x1, int y1, int x2, int y2, int color) {
        innerFill(matrix, x1, y1, x2, y2, color);
    }

    private static void innerFill(Matrix4f matrix, int x1, int y1, int x2, int y2, int color) {
        if (x1 < x2) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }

        if (y1 < y2) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }

        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        float a = (float) (color >> 24 & 255) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.vertex(matrix, (float) x1, (float) y2, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, (float) x2, (float) y2, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, (float) x2, (float) y1, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, 0.0F).color(r, g, b, a).endVertex();
    }

    public static int drawTextShadowed(Font font, MultiBufferSource bufferSource, PoseStack poseStack, FormattedCharSequence charSequence, float x, float y, int color) {
        return drawInternal(font, bufferSource, charSequence, x, y, color, poseStack.last().pose(), true);
    }

    public static int drawTextShadowed(Font font, MultiBufferSource bufferSource, Matrix4f matrix, FormattedCharSequence charSequence, float x, float y, int color) {
        return drawInternal(font, bufferSource, charSequence, x, y, color, matrix, true);
    }

    public static int drawString(Font font, MultiBufferSource bufferSource, PoseStack poseStack, String text, float x, float y, int color) {
        return drawInternal(font, bufferSource, text, x, y, color, poseStack.last().pose(), false, font.isBidirectional());
    }

    private static int drawInternal(Font font, MultiBufferSource bufferSource, FormattedCharSequence charSequence, float x, float y, int color, Matrix4f matrix, boolean shadow) {
        return font.drawInBatch(charSequence, x, y, color, shadow, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }

    private static int drawInternal(Font font, MultiBufferSource bufferSource, String text, float x, float y, int color, Matrix4f matrix, boolean shadow, boolean bidiFlag) {
        if (text == null) {
            return 0;
        }

        return font.drawInBatch(text, x, y, color, shadow, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880, bidiFlag);
    }

    public static void beginBatch(VertexFormat.Mode mode, VertexFormat format) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(mode, format);
    }

    public static void endBatch() {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        BufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();
        BufferUploader.drawWithShader(renderedBuffer);
    }
}