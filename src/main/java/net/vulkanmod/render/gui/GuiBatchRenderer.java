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

    static BufferBuilder bufferBuilder;

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

        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, 0.0F).setColor(f, f1, f2, f3);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, 0.0F).setColor(f, f1, f2, f3);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, 0.0F).setColor(f, f1, f2, f3);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, 0.0F).setColor(f, f1, f2, f3);

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
        bufferBuilder = Tesselator.getInstance().begin(mode, format);
    }

    public static void endBatch() {
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }
}

