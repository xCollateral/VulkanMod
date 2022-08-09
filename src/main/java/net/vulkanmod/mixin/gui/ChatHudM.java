package net.vulkanmod.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Deque;
import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudM {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private List<ChatHudLine<OrderedText>> visibleMessages;
    @Shadow @Final private Deque<Text> messageQueue;
    @Shadow private int scrolledLines;
    @Shadow private boolean hasUnreadNewMessages;

    @Shadow protected abstract boolean isChatHidden();

    @Shadow public abstract void clear(boolean clearHistory);

    @Shadow protected abstract boolean isChatFocused();

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getHeight();

    @Shadow public abstract double getChatScale();

    @Shadow public abstract int getVisibleLineCount();

    @Shadow protected abstract void processMessageQueue();

    /**
     * @author
     */
    @Overwrite
    public void render(MatrixStack matrices, int tickDelta) {
        int q;
        int p;
        int n;
        int m;
        if (this.isChatHidden()) {
            return;
        }
        this.processMessageQueue();
        int i = this.getVisibleLineCount();
        int j = this.visibleMessages.size();
        if (j <= 0) {
            return;
        }
        boolean bl = false;
        if (this.isChatFocused()) {
            bl = true;
        }
        float f = (float)this.getChatScale();
        int k = MathHelper.ceil((float)this.getWidth() / f);
        matrices.push();
        matrices.translate(4.0, 8.0, 0.0);
        matrices.scale(f, f, 1.0f);
        double d = this.client.options.chatOpacity * (double)0.9f + (double)0.1f;
        double e = this.client.options.textBackgroundOpacity;
        double g = 9.0 * (this.client.options.chatLineSpacing + 1.0);
        double h = -8.0 * (this.client.options.chatLineSpacing + 1.0) + 4.0 * this.client.options.chatLineSpacing;
        int l = 0;


        GuiBatchRenderer.beginBatch(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        matrices.push();
        matrices.translate(0.0, 0.0, 50.0);

        for (m = 0; m + this.scrolledLines < this.visibleMessages.size() && m < i; ++m) {
            ChatHudLine<OrderedText> chatHudLine = this.visibleMessages.get(m + this.scrolledLines);
            if (chatHudLine == null || (n = tickDelta - chatHudLine.getCreationTick()) >= 200 && !bl) continue;
            double o = bl ? 1.0 : getMessageOpacityMultiplier(n);
            p = (int)(255.0 * o * d);
            q = (int)(255.0 * o * e);
            //++l;
            if (p <= 3) continue;
            boolean r = false;
            double s = (double)(-m) * g;

            GuiBatchRenderer.fill(matrices, -4, (int)(s - g), 0 + k + 4, (int)s, q << 24);
        }
        GuiBatchRenderer.endBatch();

        matrices.translate(0.0, 0.0, 50.0);
        VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        for (m = 0; m + this.scrolledLines < this.visibleMessages.size() && m < i; ++m) {
            ChatHudLine<OrderedText> chatHudLine = this.visibleMessages.get(m + this.scrolledLines);
            if (chatHudLine == null || (n = tickDelta - chatHudLine.getCreationTick()) >= 200 && !bl) continue;
            double o = bl ? 1.0 : getMessageOpacityMultiplier(n);
            p = (int)(255.0 * o * d);
            q = (int)(255.0 * o * e);
            ++l;
            if (p <= 3) continue;
            boolean r = false;
            double s = (double)(-m) * g;

            GuiBatchRenderer.drawShadow(this.client.textRenderer, bufferSource, matrices, chatHudLine.getText(), 0.0f, (float)((int)(s + h)), 0xFFFFFF + (p << 24));

        }
        bufferSource.draw();
        matrices.pop();
        RenderSystem.disableBlend();

        if (!this.messageQueue.isEmpty()) {
            m = (int)(128.0 * d);
            int t = (int)(255.0 * e);
            matrices.push();
            matrices.translate(0.0, 0.0, 50.0);
            ChatHud.fill(matrices, -2, 0, k + 4, 9, t << 24);
            RenderSystem.enableBlend();
            matrices.translate(0.0, 0.0, 50.0);
            this.client.textRenderer.drawWithShadow(matrices, new TranslatableText("chat.queue", this.messageQueue.size()), 0.0f, 1.0f, 0xFFFFFF + (m << 24));
            matrices.pop();
            RenderSystem.disableBlend();
        }

        if (bl) {
            m = this.client.textRenderer.fontHeight;
            int t = j * m;
            n = l * m;
            int u = this.scrolledLines * n / j;
            int v = n * n / t;
            if (t != n) {
                p = u > 0 ? 170 : 96;
                q = this.hasUnreadNewMessages ? 0xCC3333 : 0x3333AA;
                matrices.translate(-4.0, 0.0, 0.0);
                ChatHud.fill(matrices, 0, -u, 2, -u - v, q + (p << 24));
                ChatHud.fill(matrices, 2, -u, 1, -u - v, 0xCCCCCC + (p << 24));
            }
        }
        matrices.pop();
    }

    private static double getMessageOpacityMultiplier(int age) {
        double d = (double)age / 200.0;
        d = 1.0 - d;
        d *= 10.0;
        d = MathHelper.clamp(d, 0.0, 1.0);
        d *= d;
        return d;
    }
}
