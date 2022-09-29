package net.vulkanmod.config;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.vulkanmod.config.widget.OptionWidget;
import net.vulkanmod.vulkan.util.VUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class OptionList2 extends ElementListWidget<OptionList2.Entry> {

    public OptionList2(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraftClient, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;
    }

    public void addButton(OptionWidget widget) {
        this.addEntry(new Entry(widget));
    }

    public void addAll(Option<?>[] options) {
        for (int i = 0; i < options.length; i++) {
            this.addEntry(new Entry(options[i].createOptionWidget((int) (0.1f * width), 0, (int) (0.8f * width), 20)));
//            this.addEntry(new Entry(options[i].createOptionWidget(width / 2 - 155, 0, 200, 20)));
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateScrollingState(mouseX, mouseY, button);
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        } else {
            OptionList2.Entry entry = this.getEntryAtPos(mouseX, mouseY);
            if (entry != null) {
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(entry);
                    this.setDragging(true);
                    return true;
                }
            } else if (button == 0) {
                this.clickedHeader((int)(mouseX - (double)(this.left + this.width / 2 - this.getRowWidth() / 2)), (int)(mouseY - (double)this.top) + (int)this.getScrollAmount() - 4);
                return true;
            }

            return false;
        }
    }

    @Nullable
    protected Entry getEntryAtPos(double x, double y) {
        int i = this.getRowWidth() / 2;
        int j = this.left + this.width / 2;
        int k = j - i;
        int l = j + i;
        int m = MathHelper.floor(y - (double)this.top) - this.headerHeight + (int)this.getScrollAmount() - 4;
        int n = m / this.itemHeight;
        if (x < this.getScrollbarPositionX() && x >= (double)k && x <= (double)l && n >= 0 && m >= 0 && n < this.getEntryCount()) {
            return (Entry)this.children().get(n);
        }
        return null;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int o;
        int n;
        int m;
        this.renderBackground(matrices);
        int i = this.getScrollbarPositionX();
        int j = i + 6;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
//        this.hoveredEntry = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
//        Object v0 = this.hoveredEntry;

        //Render Background
        RenderSystem.setShaderTexture(0, DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        float f = 32.0f;
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int color = 45;
        bufferBuilder.vertex(this.left, this.bottom, 0.0).texture((float)this.left / 32.0f, (float)(this.bottom + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).next();
        bufferBuilder.vertex(this.right, this.bottom, 0.0).texture((float)this.right / 32.0f, (float)(this.bottom + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).next();
        bufferBuilder.vertex(this.right, this.top, 0.0).texture((float)this.right / 32.0f, (float)(this.top + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).next();
        bufferBuilder.vertex(this.left, this.top, 0.0).texture((float)this.left / 32.0f, (float)(this.top + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).next();
        tessellator.draw();

        int k = this.getRowLeft();
        int l = this.top + 4 - (int)this.getScrollAmount();

        this.renderHeader(matrices, k, l, tessellator);

        this.renderList(matrices, k, l, mouseX, mouseY, delta);

        //Render horizontal shadows
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(519);
        float g = 32.0f;
        m = -100;
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(this.left, this.top, -100.0).texture(0.0f, (float)this.top / 32.0f).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(this.left + this.width, this.top, -100.0).texture((float)this.width / 32.0f, (float)this.top / 32.0f).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(this.left + this.width, 0.0, -100.0).texture((float)this.width / 32.0f, 0.0f).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(this.left, 0.0, -100.0).texture(0.0f, 0.0f).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(this.left, this.height, -100.0).texture(0.0f, (float)this.height / 32.0f).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(this.left + this.width, this.height, -100.0).texture((float)this.width / 32.0f, (float)this.height / 32.0f).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(this.left + this.width, this.bottom, -100.0).texture((float)this.width / 32.0f, (float)this.bottom / 32.0f).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(this.left, this.bottom, -100.0).texture(0.0f, (float)this.bottom / 32.0f).color(64, 64, 64, 255).next();
        tessellator.draw();

        RenderSystem.depthFunc(515);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.disableTexture();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        n = 4;
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(this.left, this.top + 4, 0.0).color(0, 0, 0, 0).next();
        bufferBuilder.vertex(this.right, this.top + 4, 0.0).color(0, 0, 0, 0).next();
        bufferBuilder.vertex(this.right, this.top, 0.0).color(0, 0, 0, 255).next();
        bufferBuilder.vertex(this.left, this.top, 0.0).color(0, 0, 0, 255).next();
        bufferBuilder.vertex(this.left, this.bottom, 0.0).color(0, 0, 0, 255).next();
        bufferBuilder.vertex(this.right, this.bottom, 0.0).color(0, 0, 0, 255).next();
        bufferBuilder.vertex(this.right, this.bottom - 4, 0.0).color(0, 0, 0, 0).next();
        bufferBuilder.vertex(this.left, this.bottom - 4, 0.0).color(0, 0, 0, 0).next();
        tessellator.draw();


        if ((o = this.getMaxScroll()) > 0) {
            RenderSystem.disableTexture();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            m = (int)((float)((this.bottom - this.top) * (this.bottom - this.top)) / (float)this.getMaxPosition());
            m = MathHelper.clamp(m, 32, this.bottom - this.top - 8);
            n = (int)this.getScrollAmount() * (this.bottom - this.top - m) / o + this.top;
            if (n < this.top) {
                n = this.top;
            }
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(i, this.bottom, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(j, this.bottom, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(j, this.top, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(i, this.top, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(i, n + m, 0.0).color(128, 128, 128, 255).next();
            bufferBuilder.vertex(j, n + m, 0.0).color(128, 128, 128, 255).next();
            bufferBuilder.vertex(j, n, 0.0).color(128, 128, 128, 255).next();
            bufferBuilder.vertex(i, n, 0.0).color(128, 128, 128, 255).next();
            bufferBuilder.vertex(i, n + m - 1, 0.0).color(192, 192, 192, 255).next();
            bufferBuilder.vertex(j - 1, n + m - 1, 0.0).color(192, 192, 192, 255).next();
            bufferBuilder.vertex(j - 1, n, 0.0).color(192, 192, 192, 255).next();
            bufferBuilder.vertex(i, n, 0.0).color(192, 192, 192, 255).next();
            tessellator.draw();
        }
        this.renderDecorations(matrices, mouseX, mouseY);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPositionX() {
//        return this.width / 2 + 124 + 32;
        return (int) (this.width * 0.95f);
    }

    public Optional<OptionWidget> getHoveredButton(double mouseX, double mouseY) {
        for (Entry buttonEntry : this.children()) {
            if (!buttonEntry.button.isMouseOver(mouseX, mouseY)) continue;
            return Optional.of(buttonEntry.button);
        }
        return Optional.empty();
    }

    protected void renderList(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
        int i = this.getEntryCount();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        for (int j = 0; j < i; ++j) {
            int p;
            int k = this.getRowTop(j);
            int l = this.getRowBottom(j);
            if (l < this.top || k > this.bottom) continue;
            int m = y + j * this.itemHeight + this.headerHeight;
            int n = this.itemHeight - 4;
            Entry entry = this.getEntry(j);
            int o = this.getRowWidth();
            if (this.isSelectedEntry(j)) {
                p = this.left + this.width / 2 - o / 2;
                int q = this.left + this.width / 2 + o / 2;
                RenderSystem.disableTexture();
                RenderSystem.setShader(GameRenderer::getPositionShader);
                float f = this.isFocused() ? 1.0f : 0.5f;
                RenderSystem.setShaderColor(f, f, f, 1.0f);
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                bufferBuilder.vertex(p, m + n + 2, 0.0).next();
                bufferBuilder.vertex(q, m + n + 2, 0.0).next();
                bufferBuilder.vertex(q, m - 2, 0.0).next();
                bufferBuilder.vertex(p, m - 2, 0.0).next();
                tessellator.draw();
                RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                bufferBuilder.vertex(p + 1, m + n + 1, 0.0).next();
                bufferBuilder.vertex(q - 1, m + n + 1, 0.0).next();
                bufferBuilder.vertex(q - 1, m - 1, 0.0).next();
                bufferBuilder.vertex(p + 1, m - 1, 0.0).next();
                tessellator.draw();
                RenderSystem.enableTexture();
            }
            p = this.getRowLeft();
            ((EntryListWidget.Entry)entry).render(matrices, j, k, p, o, n, mouseX, mouseY, false, delta);
        }
    }

    private int getRowBottom(int index) {
        return this.getRowTop(index) + this.itemHeight;
    }

    protected static class Entry
            extends ElementListWidget.Entry<Entry> {
        final OptionWidget button;

        private Selectable focusedSelectable;

        private Entry(OptionWidget button) {
            this.button = button;
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            button.y = y;
            button.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(this.button);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(this.button);
        }

    }
}
