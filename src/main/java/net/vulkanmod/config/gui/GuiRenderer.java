package net.vulkanmod.config.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public abstract class GuiRenderer {
    public static Minecraft minecraft = Minecraft.getInstance();
    public static Font font = minecraft.font;
    public static GuiGraphics guiGraphics;
    public static PoseStack pose;

    public static BufferBuilder bufferBuilder;
    public static boolean batching = false;

    public static void fill(float x0, float y0, float x1, float y1, int color) {
        fill(x0, y0, x1, y1, 0, color);
    }

    public static void fill(float x0, float y0, float x1, float y1, float z, int color) {
        Matrix4f matrix4f = pose.last().pose();

        float a = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        float r = (float) FastColor.ARGB32.red(color) / 255.0F;
        float g = (float) FastColor.ARGB32.green(color) / 255.0F;
        float b = (float) FastColor.ARGB32.blue(color) / 255.0F;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        if (!batching)
            bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.addVertex(matrix4f, x0, y0, z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix4f, x0, y1, z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix4f, x1, y1, z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix4f, x1, y0, z).setColor(r, g, b, a);

        if (!batching)
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    public static void fillGradient(float x0, float y0, float x1, float y1, int color1, int color2) {
        fillGradient(x0, y0, x1, y1, 0, color1, color2);
    }

    public static void fillGradient(float x0, float y0, float x1, float y1, float z, int color1, int color2) {
        float a1 = (float) FastColor.ARGB32.alpha(color1) / 255.0F;
        float r1 = (float) FastColor.ARGB32.red(color1) / 255.0F;
        float g1 = (float) FastColor.ARGB32.green(color1) / 255.0F;
        float b1 = (float) FastColor.ARGB32.blue(color1) / 255.0F;
        float a2 = (float) FastColor.ARGB32.alpha(color2) / 255.0F;
        float r2 = (float) FastColor.ARGB32.red(color2) / 255.0F;
        float g2 = (float) FastColor.ARGB32.green(color2) / 255.0F;
        float b2 = (float) FastColor.ARGB32.blue(color2) / 255.0F;

        Matrix4f matrix4f = pose.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        if (!batching)
            bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.addVertex(matrix4f, x0, y0, z).setColor(r1, g1, b1, a1);
        bufferBuilder.addVertex(matrix4f, x0, y1, z).setColor(r2, g2, b2, a2);
        bufferBuilder.addVertex(matrix4f, x1, y1, z).setColor(r2, g2, b2, a2);
        bufferBuilder.addVertex(matrix4f, x1, y0, z).setColor(r1, g1, b1, a1);

        if (!batching)
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    public static void renderBorder(float x0, float y0, float x1, float y1, float width, int color) {
        renderBorder(x0, y0, x1, y1, 0, width, color);
    }

    public static void renderBorder(float x0, float y0, float x1, float y1, float z, float width, int color) {
        GuiRenderer.fill(x0, y0, x1, y0 + width, z, color);
        GuiRenderer.fill(x0, y1 - width, x1, y1, z, color);

        GuiRenderer.fill(x0, y0 + width, x0 + width, y1 - width, z, color);
        GuiRenderer.fill(x1 - width, y0 + width, x1, y1 - width, z, color);
    }

    public static void drawString(Component component, int x, int y, int color) {
        drawString(component, x, y, color, true);
    }

    public static void drawString(FormattedCharSequence formattedCharSequence, int x, int y, int color) {
        drawString(formattedCharSequence, x, y, color, true);
    }

    public static void drawString(Component component, int x, int y, int color, boolean shadow) {
        drawString(component, x, y, 0, color, shadow);
    }

    public static void drawString(FormattedCharSequence formattedCharSequence, int x, int y, int color, boolean shadow) {
        drawString(formattedCharSequence, x, y, 0, color, shadow);
    }

    public static void drawString(Component component, int x, int y, int z, int color) {
        drawString(component, x, y, z, color, true);
    }

    public static void drawString(FormattedCharSequence formattedCharSequence, int x, int y, int z, int color) {
        drawString(formattedCharSequence, x, y, z, color, true);
    }

    public static void drawString(Component component, int x, int y, int z, int color, boolean shadow) {
        drawString(component.getVisualOrderText(), x, y, z, color, shadow);
    }

    public static void drawString(FormattedCharSequence formattedCharSequence, int x, int y, int z, int color, boolean shadow) {
        if (z == 0) {
            guiGraphics.drawString(font, formattedCharSequence, x, y, color, shadow);
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, z);

        guiGraphics.drawString(font, formattedCharSequence, x, y, color, shadow);

        guiGraphics.pose().popPose();
    }

    public static void drawCenteredString(Component component, int x, int y, int color) {
        drawCenteredString(component.getVisualOrderText(), x, y, color);
    }

    public static void drawCenteredString(FormattedCharSequence formattedCharSequence, int x, int y, int color) {
        drawString(formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color);
    }

    public static void drawScrollingString(Component component, int x, int y, int maxWidth, int color) {
        drawScrollingString(component.getVisualOrderText(), x, y, maxWidth, color);
    }

    public static void drawScrollingString(FormattedCharSequence formattedCharSequence, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(formattedCharSequence);
        if (textWidth <= maxWidth) {
            drawCenteredString(formattedCharSequence, x, y, color);
        } else {
            int x0 = x - maxWidth / 2, x1 = x + maxWidth / 2;
            int scrollAmount = textWidth - maxWidth;
            double currentTimeInSeconds = (double) Util.getMillis() / 1000.0;
            double scrollSpeed = Math.max(scrollAmount * 0.5, 3.0);
            double scrollingOffset = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * currentTimeInSeconds / scrollSpeed)) / 2.0 + 0.5;
            double horizontalScroll = Mth.lerp(scrollingOffset, 0.0, scrollAmount);

            guiGraphics.enableScissor(x0 - 1, 0, x1, minecraft.getWindow().getScreenHeight());
            drawString(formattedCharSequence, (int) (x0 - horizontalScroll), y, color);
            guiGraphics.disableScissor();
        }
    }

    public static void beginBatch(VertexFormat.Mode mode, VertexFormat format) {
        bufferBuilder = Tesselator.getInstance().begin(mode, format);
        batching = true;
    }

    public static void endBatch() {
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        batching = false;
    }
}
