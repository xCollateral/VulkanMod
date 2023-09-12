package net.vulkanmod.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Objects;

@Mixin(ChatComponent.class)
public abstract class ChatComponentM {

    @Shadow @Final private Minecraft minecraft;

    @Shadow protected abstract boolean isChatHidden();

    @Shadow protected abstract boolean isChatFocused();

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getHeight();

    @Shadow public abstract int getLinesPerPage();

    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Shadow public abstract double getScale();

    @Shadow protected abstract int getLineHeight();

    @Shadow private int chatScrollbarPos;

    @Shadow protected abstract int getTagIconLeft(GuiMessage.Line line);

    @Shadow private boolean newMessageSinceScroll;

    @Shadow protected abstract int getMessageEndIndexAt(double d, double e);

    @Shadow protected abstract double screenToChatX(double d);

    @Shadow protected abstract double screenToChatY(double d);

    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/gui/chat_tags.png");

    /**
     * @author
     */
    @Overwrite
    public void render(GuiGraphics guiGraphics, int i, int j, int k) {
        PoseStack poseStack = guiGraphics.pose();

        if (!this.isChatHidden()) {
            int l = this.getLinesPerPage();
            int m = this.trimmedMessages.size();
            if (m > 0) {
                boolean bl = this.isChatFocused();
                float f = (float)this.getScale();
                int n = Mth.ceil((float)this.getWidth() / f);
                int o = this.minecraft.getWindow().getGuiScaledHeight();
                poseStack.pushPose();
                poseStack.scale(f, f, 1.0F);
                poseStack.translate(4.0F, 0.0F, 0.0F);
                int p = Mth.floor((float)(o - 40) / f);
                int q = this.getMessageEndIndexAt(this.screenToChatX((double)j), this.screenToChatY((double)k));
                double d = (Double)this.minecraft.options.chatOpacity().get() * 0.8999999761581421 + 0.10000000149011612;
                double e = (Double)this.minecraft.options.textBackgroundOpacity().get();
                double g = (Double)this.minecraft.options.chatLineSpacing().get();
                int r = this.getLineHeight();
                int s = (int)Math.round(-8.0 * (g + 1.0) + 4.0 * g);
                int t = 0;

                int w;
                int x;
                int y;
                int aa;

                poseStack.pushPose();
                poseStack.translate(0.0, 0.0, 50.0);
                Matrix4f mat1 = poseStack.last().pose();
//                poseStack.pushPose();
                poseStack.translate(0.0, 0.0, 50.0);
                Matrix4f mat2 = poseStack.last().pose();

                GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                RenderSystem.enableBlend();
                MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

                for(int u = 0; u + this.chatScrollbarPos < this.trimmedMessages.size() && u < l; ++u) {
                    int v = u + this.chatScrollbarPos;
                    GuiMessage.Line line = (GuiMessage.Line)this.trimmedMessages.get(v);
                    if (line != null) {
                        w = i - line.addedTime();
                        if (w < 200 || bl) {
                            double h = bl ? 1.0 : getTimeFactor(w);
                            x = (int)(255.0 * h * d);
                            y = (int)(255.0 * h * e);
                            ++t;
                            if (x > 3) {
                                boolean z = false;
                                aa = p - u * r;
                                int ab = aa + s;
//                                poseStack.pushPose();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
                                GuiBatchRenderer.fill(mat1, -4, aa - r, 0 + n + 4 + 4, aa, y << 24);
                                GuiMessageTag guiMessageTag = line.tag();
                                if (guiMessageTag != null) {
                                    int ac = guiMessageTag.indicatorColor() | x << 24;
                                    GuiBatchRenderer.fill(mat1, -4, aa - r, -2, aa, ac);
                                    if (v == q && guiMessageTag.icon() != null) {
                                        int ad = this.getTagIconLeft(line);
                                        Objects.requireNonNull(this.minecraft.font);
                                        int ae = ab + 9;
                                        this.drawTagIcon(mat1, ad, ae, guiMessageTag.icon());
                                    }
                                }

//                                RenderSystem.enableBlend();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
//                                this.minecraft.font.drawShadow(poseStack, line.content(), 0.0F, (float)ab, 16777215 + (x << 24));
//                                GuiBatchRenderer.drawShadow(this.minecraft.font, bufferSource, mat2, line.content(), 0.0F, (float)ab, 16777215 + (x << 24));
//                                RenderSystem.disableBlend();
//                                poseStack.popPose();
                            }
                        }
                    }
                }

                GuiBatchRenderer.endBatch();

                for(int u = 0; u + this.chatScrollbarPos < this.trimmedMessages.size() && u < l; ++u) {
                    int v = u + this.chatScrollbarPos;
                    GuiMessage.Line line = (GuiMessage.Line)this.trimmedMessages.get(v);
                    if (line != null) {
                        w = i - line.addedTime();
                        if (w < 200 || bl) {
                            double h = bl ? 1.0 : getTimeFactor(w);
                            x = (int)(255.0 * h * d);
                            y = (int)(255.0 * h * e);
                            ++t;
                            if (x > 3) {
                                boolean z = false;
                                aa = p - u * r;
                                int ab = aa + s;
//                                poseStack.pushPose();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
//                                GuiBatchRenderer.fill(mat1, -4, aa - r, 0 + n + 4 + 4, aa, y << 24);
//                                GuiMessageTag guiMessageTag = line.tag();
//                                if (guiMessageTag != null) {
//                                    int ac = guiMessageTag.indicatorColor() | x << 24;
//                                    GuiBatchRenderer.fill(mat1, -4, aa - r, -2, aa, ac);
//                                    if (v == q && guiMessageTag.icon() != null) {
//                                        int ad = this.getTagIconLeft(line);
//                                        Objects.requireNonNull(this.minecraft.font);
//                                        int ae = ab + 9;
//                                        this.drawTagIcon(mat1, ad, ae, guiMessageTag.icon());
//                                    }
//                                }

//                                RenderSystem.enableBlend();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
//                                this.minecraft.font.drawShadow(poseStack, line.content(), 0.0F, (float)ab, 16777215 + (x << 24));
                                GuiBatchRenderer.drawTextShadowed(this.minecraft.font, bufferSource, mat2, line.content(), 0.0F, (float)ab, 16777215 + (x << 24));
//                                RenderSystem.disableBlend();
//                                poseStack.popPose();
                            }
                        }
                    }
                }

                poseStack.popPose();

                long af = this.minecraft.getChatListener().queueSize();
                int ag;
                if (af > 0L) {
                    ag = (int)(128.0 * d);
                    w = (int)(255.0 * e);
                    poseStack.pushPose();
                    poseStack.translate(0.0F, (float)p, 50.0F);
                    GuiBatchRenderer.fill(poseStack, -2, 0, n + 4, 9, w << 24);
                    RenderSystem.enableBlend();
                    poseStack.translate(0.0F, 0.0F, 50.0F);
//                    this.minecraft.font.drawShadow(poseStack, Component.translatable("chat.queue", new Object[]{af}), 0.0F, 1.0F, 16777215 + (ag << 24));
                    GuiBatchRenderer.drawTextShadowed(this.minecraft.font, bufferSource, poseStack, Component.translatable("chat.queue", af).getVisualOrderText(), 0.0F, 1.0F, 16777215 + (ag << 24));
                    poseStack.popPose();
//                    RenderSystem.disableBlend();
                }

                bufferSource.endBatch();

                Tesselator.getInstance().getBuilder().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                if (bl) {
                    ag = this.getLineHeight();
                    w = m * ag;
                    int ah = t * ag;
                    int ai = this.chatScrollbarPos * ah / m - p;
                    x = ah * ah / w;
                    if (w != ah) {
                        y = ai > 0 ? 170 : 96;
                        int z = this.newMessageSinceScroll ? 13382451 : 3355562;
                        aa = n + 4;
                        GuiBatchRenderer.fill(poseStack, aa, -ai, aa + 2, -ai - x, z + (y << 24));
                        GuiBatchRenderer.fill(poseStack, aa + 2, -ai, aa + 1, -ai - x, 13421772 + (y << 24));
                    }
                }

                BufferUploader.drawWithShader(Tesselator.getInstance().getBuilder().end());

                poseStack.popPose();
            }
        }
    }

    private void drawTagIcon(Matrix4f mat4f, int i, int j, GuiMessageTag.Icon icon) {
        int k = j - icon.height - 1;
        RenderSystem.setShaderTexture(0, TEXTURE_LOCATION);

        GuiBatchRenderer.blit(mat4f, i, k, icon.u, icon.v, icon.width, icon.height, 32, 32);
    }

    private static double getTimeFactor(int i) {
        double d = (double)i / 200.0;
        d = 1.0 - d;
        d *= 10.0;
        d = Mth.clamp(d, 0.0, 1.0);
        d *= d;
        return d;
    }
}
