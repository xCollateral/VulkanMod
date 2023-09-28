package net.vulkanmod.mixin.gui;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudM {

    @Shadow @Final private Minecraft minecraft;

    @Shadow
    private static long bytesToMegabytes(long bytes) {
        return 0;
    }

    @Shadow @Final private Font font;

    @Shadow protected abstract List<String> getGameInformation();

    @Shadow protected abstract List<String> getSystemInformation();

    //TODO remove
//    /**
//     * @author
//     */
//    @Overwrite
//    public void drawGameInformation(PoseStack matrices) {
//        List<String> list = this.getGameInformation();
//        list.add("");
//        boolean bl = this.minecraft.getSingleplayerServer() != null;
//        list.add("Debug: Pie [shift]: " + (this.minecraft.options.renderDebugCharts ? "visible" : "hidden") + (bl ? " FPS + TPS" : " FPS") + " [alt]: " + (this.minecraft.options.renderFpsChart ? "visible" : "hidden"));
//        list.add("For help: press F3 + Q");
//
//        RenderSystem.enableBlend();
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.font.lineHeight;
//            int k = this.font.width(string);
//            int l = 2;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.fill(matrices, 1, m - 1, 2 + k + 1, m + j - 1, -1873784752);
//        }
//        GuiBatchRenderer.endBatch();
//
//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.font.lineHeight;
//            int k = this.font.width(string);
//            int l = 2;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.drawString(this.font, bufferSource, matrices, string, 2.0f, (float)m, 0xE0E0E0);
//        }
//        bufferSource.endBatch();
//    }

//    /**
//     * @author
//     */
//    @Overwrite
//    public void drawSystemInformation(GuiGraphics guiGraphics) {
//        List<String> list = this.getSystemInformation();
//
//        RenderSystem.enableBlend();
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.font.lineHeight;
//            int k = this.font.width(string);
//            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.fill(matrices, l - 1, m - 1, l + k + 1, m + j - 1, -1873784752);
//        }
//        GuiBatchRenderer.endBatch();
//
//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.font.lineHeight;
//            int k = this.font.width(string);
//            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.drawString(this.font, bufferSource, matrices, string, (float)l, (float)m, 0xE0E0E0);
//        }
//        bufferSource.endBatch();
//    }
}
