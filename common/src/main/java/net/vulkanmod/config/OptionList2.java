package net.vulkanmod.config;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.vulkanmod.config.widget.OptionWidget;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class OptionList2 extends ContainerObjectSelectionList<OptionList2.Entry> {

    public OptionList2(Minecraft minecraftClient, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraftClient, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;
    }

    public void addButton(OptionWidget widget) {
        this.addEntry(new net.vulkanmod.config.OptionList2.Entry(widget));
    }

    public void addAll(Option<?>[] options) {
        for (int i = 0; i < options.length; i++) {
            this.addEntry(new net.vulkanmod.config.OptionList2.Entry(options[i].createOptionWidget((int) (0.1f * width), 0, (int) (0.8f * width), 20)));
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
                this.clickedHeader((int)(mouseX - (double)(this.x0 + this.width / 2 - this.getRowWidth() / 2)), (int)(mouseY - (double)this.y0) + (int)this.getScrollAmount() - 4);
                return true;
            }

            return false;
        }
    }

    @Nullable
    protected net.vulkanmod.config.OptionList2.Entry getEntryAtPos(double x, double y) {
        int i = this.getRowWidth() / 2;
        int j = this.x0 + this.width / 2;
        int k = j - i;
        int l = j + i;
        int m = Mth.floor(y - (double)this.y0) - this.headerHeight + (int)this.getScrollAmount() - 4;
        int n = m / this.itemHeight;
        if (x < this.getScrollbarPosition() && x >= (double)k && x <= (double)l && n >= 0 && m >= 0 && n < this.getItemCount()) {
            return (net.vulkanmod.config.OptionList2.Entry)this.children().get(n);
        }
        return null;
    }

    @Override
    public void render(GuiGraphics matrices, int mouseX, int mouseY, float delta) {
        int o;
        int n;
        int m;
//        this.renderBackground(matrices);
        int i = this.getScrollbarPosition();
        int j = i + 6;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
//        this.hoveredEntry = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
//        Object v0 = this.hoveredEntry;

        //Render Background
        RenderSystem.setShaderTexture(0, Screen.BACKGROUND_LOCATION);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        float f = 32.0f;
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        int color = 45;
        bufferBuilder.vertex(this.x0, this.y1, 0.0).uv((float)this.x0 / 32.0f, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).endVertex();
        bufferBuilder.vertex(this.x1, this.y1, 0.0).uv((float)this.x1 / 32.0f, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).endVertex();
        bufferBuilder.vertex(this.x1, this.y0, 0.0).uv((float)this.x1 / 32.0f, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).endVertex();
        bufferBuilder.vertex(this.x0, this.y0, 0.0).uv((float)this.x0 / 32.0f, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, 255).endVertex();
        tesselator.end();

        int k = this.getRowLeft();
        int l = this.y0 + 4 - (int)this.getScrollAmount();

        this.renderHeader(matrices, k, l);

        this.renderList(matrices, k, l, mouseX, mouseY, delta);

        //Render horizontal shadows
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Screen.BACKGROUND_LOCATION);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(519);
        float g = 32.0f;
        m = -100;
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(this.x0, this.y0, -100.0).uv(0.0f, (float)this.y0 / 32.0f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(this.x0 + this.width, this.y0, -100.0).uv((float)this.width / 32.0f, (float)this.y0 / 32.0f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(this.x0 + this.width, 0.0, -100.0).uv((float)this.width / 32.0f, 0.0f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(this.x0, 0.0, -100.0).uv(0.0f, 0.0f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(this.x0, this.height, -100.0).uv(0.0f, (float)this.height / 32.0f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(this.x0 + this.width, this.height, -100.0).uv((float)this.width / 32.0f, (float)this.height / 32.0f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(this.x0 + this.width, this.y1, -100.0).uv((float)this.width / 32.0f, (float)this.y1 / 32.0f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(this.x0, this.y1, -100.0).uv(0.0f, (float)this.y1 / 32.0f).color(64, 64, 64, 255).endVertex();
        tesselator.end();

        RenderSystem.depthFunc(515);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        n = 4;
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(this.x0, this.y0 + 4, 0.0).color(0, 0, 0, 0).endVertex();
        bufferBuilder.vertex(this.x1, this.y0 + 4, 0.0).color(0, 0, 0, 0).endVertex();
        bufferBuilder.vertex(this.x1, this.y0, 0.0).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.x0, this.y0, 0.0).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.x0, this.y1, 0.0).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.x1, this.y1, 0.0).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.x1, this.y1 - 4, 0.0).color(0, 0, 0, 0).endVertex();
        bufferBuilder.vertex(this.x0, this.y1 - 4, 0.0).color(0, 0, 0, 0).endVertex();
        tesselator.end();


        if ((o = this.getMaxScroll()) > 0) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            m = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
            m = Mth.clamp(m, 32, this.y1 - this.y0 - 8);
            n = (int)this.getScrollAmount() * (this.y1 - this.y0 - m) / o + this.y0;
            if (n < this.y0) {
                n = this.y0;
            }
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferBuilder.vertex(i, this.y1, 0.0).color(0, 0, 0, 255).endVertex();
            bufferBuilder.vertex(j, this.y1, 0.0).color(0, 0, 0, 255).endVertex();
            bufferBuilder.vertex(j, this.y0, 0.0).color(0, 0, 0, 255).endVertex();
            bufferBuilder.vertex(i, this.y0, 0.0).color(0, 0, 0, 255).endVertex();
            bufferBuilder.vertex(i, n + m, 0.0).color(128, 128, 128, 255).endVertex();
            bufferBuilder.vertex(j, n + m, 0.0).color(128, 128, 128, 255).endVertex();
            bufferBuilder.vertex(j, n, 0.0).color(128, 128, 128, 255).endVertex();
            bufferBuilder.vertex(i, n, 0.0).color(128, 128, 128, 255).endVertex();
            bufferBuilder.vertex(i, n + m - 1, 0.0).color(192, 192, 192, 255).endVertex();
            bufferBuilder.vertex(j - 1, n + m - 1, 0.0).color(192, 192, 192, 255).endVertex();
            bufferBuilder.vertex(j - 1, n, 0.0).color(192, 192, 192, 255).endVertex();
            bufferBuilder.vertex(i, n, 0.0).color(192, 192, 192, 255).endVertex();
            tesselator.end();
        }
        this.renderDecorations(matrices, mouseX, mouseY);
        RenderSystem.disableBlend();
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPosition() {
//        return this.width / 2 + 124 + 32;
        return (int) (this.width * 0.95f);
    }

    public Optional<OptionWidget> getHoveredButton(double mouseX, double mouseY) {
        for (net.vulkanmod.config.OptionList2.Entry buttonEntry : this.children()) {
            if (!buttonEntry.button.isMouseOver(mouseX, mouseY)) continue;
            return Optional.of(buttonEntry.button);
        }
        return Optional.empty();
    }

    protected void renderList(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float delta) {
        int i = this.getItemCount();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        for (int j = 0; j < i; ++j) {
            int p;
            int k = this.getRowTop(j);
            int l = this.getRowBottom(j);
            if (l < this.y0 || k > this.y1) continue;
            int m = y + j * this.itemHeight + this.headerHeight;
            int n = this.itemHeight - 4;
            net.vulkanmod.config.OptionList2.Entry entry = this.getEntry(j);
            int o = this.getRowWidth();
            if (this.isSelectedItem(j)) {
                p = this.x0 + this.width / 2 - o / 2;
                int q = this.x0 + this.width / 2 + o / 2;

                RenderSystem.setShader(GameRenderer::getPositionShader);
                float f = this.isFocused() ? 1.0f : 0.5f;
                RenderSystem.setShaderColor(f, f, f, 1.0f);
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bufferBuilder.vertex(p, m + n + 2, 0.0).endVertex();
                bufferBuilder.vertex(q, m + n + 2, 0.0).endVertex();
                bufferBuilder.vertex(q, m - 2, 0.0).endVertex();
                bufferBuilder.vertex(p, m - 2, 0.0).endVertex();
                tesselator.end();
                RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bufferBuilder.vertex(p + 1, m + n + 1, 0.0).endVertex();
                bufferBuilder.vertex(q - 1, m + n + 1, 0.0).endVertex();
                bufferBuilder.vertex(q - 1, m - 1, 0.0).endVertex();
                bufferBuilder.vertex(p + 1, m - 1, 0.0).endVertex();
                tesselator.end();

            }
            p = this.getRowLeft();
            entry.render(guiGraphics, j, k, p, o, n, mouseX, mouseY, false, delta);
        }
    }

    protected int getRowBottom(int index) {
        return this.getRowTop(index) + this.itemHeight;
    }

    protected static class Entry
            extends ContainerObjectSelectionList.Entry<net.vulkanmod.config.OptionList2.Entry> {
        final OptionWidget button;

        private NarratableEntry focusedSelectable;

        private Entry(OptionWidget button) {
            this.button = button;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            button.y = y;
            button.render(guiGraphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.button);
        }
    }
}
