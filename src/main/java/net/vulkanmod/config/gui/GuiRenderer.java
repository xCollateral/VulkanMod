package net.vulkanmod.config.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.List;

public abstract class GuiRenderer {

    public static Minecraft minecraft;
    public static Font font;
    public static GuiGraphics guiGraphics;
    public static PoseStack pose;

    public static BufferBuilder bufferBuilder;
    public static boolean batching = false;

    public static void setPoseStack(PoseStack poseStack) {
        pose = poseStack;
    }

    public static void disableScissor() {
        RenderSystem.disableScissor();
    }

    public static void enableScissor(int x, int y, int width, int height) {
        Window window = Minecraft.getInstance().getWindow();
        int wHeight = window.getHeight();
        double scale = window.getGuiScale();
        int xScaled = (int) (x * scale);
        int yScaled = (int) (wHeight - (y + height) * scale);
        int widthScaled = (int) (width * scale);
        int heightScaled = (int) (height * scale);
        RenderSystem.enableScissor(xScaled, yScaled, Math.max(0, widthScaled), Math.max(0, heightScaled));
    }

    public static void fillBox(float x0, float y0, float width, float height, int color) {
        fill(x0, y0, x0 + width, y0 + height, 0, color);
    }

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

    public static void renderBoxBorder(float x0, float y0, float width, float height, float borderWidth, int color) {
        renderBorder(x0, y0, x0 + width, y0 + height, borderWidth, color);
    }

    public static void renderBorder(float x0, float y0, float x1, float y1, float width, int color) {
        GuiRenderer.fill(x0, y0, x1, y0 + width, color);
        GuiRenderer.fill(x0, y1 - width, x1, y1, color);

        GuiRenderer.fill(x0, y0 + width, x0 + width, y1 - width, color);
        GuiRenderer.fill(x1 - width, y0 + width, x1, y1 - width, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color) {
        drawString(font, component.getVisualOrderText(), x, y, color);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color, boolean shadow) {
        drawString(font, component.getVisualOrderText(), x, y, color, shadow);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color, boolean shadow) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color, shadow);
    }

    public static void drawCenteredString(Font font, Component component, int x, int y, int color) {
        FormattedCharSequence formattedCharSequence = component.getVisualOrderText();
        guiGraphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color);
    }

    public static int getMaxTextWidth(Font font, List<FormattedCharSequence> list) {
        int maxWidth = 0;
        for (var text : list) {
            int width = font.width(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
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
