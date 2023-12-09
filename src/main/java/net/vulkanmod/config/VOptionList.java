package net.vulkanmod.config;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.widget.OptionWidget;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class VOptionList extends AbstractWidget {
    boolean scrolling = false;
    float scrollAmount = 0.0f;
    boolean isDragging = false;

    int headerHeight;
    int itemHeight;

    GuiEventListener focused;

    private final List<Entry> children = new ObjectArrayList<>();

    public VOptionList(Minecraft minecraftClient, int width, int height, int top, int bottom, int itemHeight) {
        super(0, top, width, height - top - bottom, Component.empty());
//        this.centerListVertically = false;

        this.itemHeight = itemHeight;
        this.headerHeight = top;
    }

    public void addButton(OptionWidget widget) {
        this.addEntry(new VOptionList.Entry(widget));
    }

    public void addAll(Option<?>[] options) {
        for (int i = 0; i < options.length; i++) {
            this.addEntry(new VOptionList.Entry(options[i].createOptionWidget((int) (0.1f * width), 0, (int) (0.8f * width), 20)));
//            this.addEntry(new Entry(options[i].createOptionWidget(width / 2 - 155, 0, 200, 20)));
        }
    }

    private void addEntry(Entry entry) {
        this.children.add(entry);
    }

    public void clearEntries() { this.children.clear(); }

    protected void updateScrollingState(double mouseX, double mouseY, int button) {
        this.scrolling = button == 0 && mouseX >= (double)this.getScrollbarPosition() && mouseX < (double)(this.getScrollbarPosition() + 6);
    }

    protected float getScrollAmount() {
        return scrollAmount;
    }

    private int getItemCount() {
        return this.children.size();
    }

    GuiEventListener getFocused() {
        return focused;
    }

    void setFocused(GuiEventListener focussed) {
        this.focused = focussed;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateScrollingState(mouseX, mouseY, button);
        if (this.isMouseOver(mouseX, mouseY)) {
            Entry entry = this.getEntryAtPos(mouseX, mouseY);
            if (entry != null) {
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    setFocused(entry);
                    entry.setFocused(true);
//                    this.setDragging(true);
                    return true;
                }
            } else if (button == 0) {
//                this.clickedHeader((int) (mouseX - (double) (this.getX() + this.width / 2 - this.getRowWidth() / 2)), (int) (mouseY - (double) this.getY()) + (int) this.getScrollAmount() - 4);
                return true;
            }

        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button)) {
            Entry entry = this.getEntryAtPos(mouseX, mouseY);
            if (entry != null) {
                if (entry.mouseReleased(mouseX, mouseY, button)) {
                    entry.setFocused(false);
                    setFocused(null);
//                    this.setDragging(true);
                    return true;
                }
            }
        }
        return false;
    }

//    public boolean mouseDragged(double mouseX, double mouseY, int button, double f, double g) {
//        if (this.isValidClickButton(button)) {
//            Entry entry = this.getEntryAtPos(mouseX, mouseY);
//            if (entry != null) {
//                if (entry.mouseDragged(mouseX, mouseY, button, f, g)) {
//                    entry.setFocused(true);
////                    this.setDragging(true);
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        if (this.getFocused() != null && i == 0) {
            return this.getFocused().mouseDragged(d, e, i, f, g);
        } else if (i == 0 && this.scrolling) {
            if (e < (double)this.getY()) {
                this.setScrollAmount(0.0);
            } else if (e > (double)this.getBottom()) {
                this.setScrollAmount(this.getMaxScroll());
            } else {
                double h = Math.max(1, this.getMaxScroll());
                int j = this.height;
                int k = Mth.clamp((int)((float)(j * j) / (float)this.getMaxPosition()), 32, j - 8);
                double l = Math.max(1.0, h / (double)(j - k));
                this.setScrollAmount(this.getScrollAmount() + g * l);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean mouseScrolled(double d, double e, double f, double g) {
        this.setScrollAmount(this.getScrollAmount() - g * (double)this.itemHeight / 2.0);
        return true;
    }

    public int getMaxScroll() {
        return Math.max(0, this.getMaxPosition() - (this.height - 4));
    }

    protected int getMaxPosition() {
        return this.getItemCount() * this.itemHeight;
    }

    public void setScrollAmount(double d) {
        this.scrollAmount = (float) Mth.clamp(d, 0.0, this.getMaxScroll());
    }

    public int getBottom() {
        return this.getY() + this.getHeight();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Nullable
    protected VOptionList.Entry getEntryAtPos(double x, double y) {
        int i = this.getRowWidth() / 2;
        int j = this.getX() + this.width / 2;
        int k = j - i;
        int l = j + i;
        int m = Mth.floor(y - (double)this.getY()) + (int)this.getScrollAmount() - 4;
        int n = m / this.itemHeight;
        if (x < this.getScrollbarPosition() && x >= (double)k && x <= (double)l && n >= 0 && m >= 0 && n < this.getItemCount()) {
            return this.children.get(n);
        }
        return null;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
//        this.renderBackground(guiGraphics);
        int i = this.getScrollbarPosition();
        int j = i + 6;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

//        this.hoveredEntry = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
//        Object v0 = this.hoveredEntry;

        //Render Background
        RenderSystem.setShaderTexture(0, Screen.BACKGROUND_LOCATION);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        float f = 32.0f;
//        RenderSystem.disableDepthTest();
        VRenderSystem.depthFunc(518);

        int color = 0;
//        int alpha = Minecraft.getInstance().level != null ? 80 : 255;
        int alpha = 80;
        double depth = -1.0;
        RenderSystem.enableBlend();

//        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
//        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
//        bufferBuilder.vertex(this.getX(), this.getY() +this. getHeight(), depth).uv((float)this.getX() / 32.0f, (float)(this.getY() +this. getHeight() + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, alpha).endVertex();
//        bufferBuilder.vertex(this.getX() +this. getWidth(), this.getY() +this. getHeight(), depth).uv((float)this.getX() +this. getWidth() / 32.0f, (float)(this.getY() +this. getHeight() + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, alpha).endVertex();
//        bufferBuilder.vertex(this.getX() +this. getWidth(), this.getY(), depth).uv((float)this.getX() +this. getWidth() / 32.0f, (float)(this.getY() + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, alpha).endVertex();
//        bufferBuilder.vertex(this.getX(), this.getY(), depth).uv((float)this.getX() / 32.0f, (float)(this.getY() + (int)this.getScrollAmount()) / 32.0f).color(color, color, color, alpha).endVertex();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(this.getX(), this.getY() +this. getHeight(), depth).color(color, color, color, alpha).endVertex();
        bufferBuilder.vertex(this.getX() +this. getWidth(), this.getY() +this. getHeight(), depth).color(color, color, color, alpha).endVertex();
        bufferBuilder.vertex(this.getX() +this. getWidth(), this.getY(), depth).color(color, color, color, alpha).endVertex();
        bufferBuilder.vertex(this.getX(), this.getY(), depth).color(color, color, color, alpha).endVertex();

        tesselator.end();
        VRenderSystem.depthFunc(515);

        int k = this.getRowLeft();
        int l = this.getY() + 4 - (int)this.getScrollAmount();

//        this.renderHeader(guiGraphics, k, l);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f ,0.0f, -0.5f);
        this.renderList(guiGraphics, k, l, mouseX, mouseY, delta);

        //Render darker header
//        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
//        RenderSystem.setShaderTexture(0, Screen.BACKGROUND_LOCATION);
//        RenderSystem.enableDepthTest();
//        RenderSystem.depthFunc(519);
//        float g = 32.0f;
//        m = -100;
//        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
//        bufferBuilder.vertex(this.getX(), this.getY(), -100.0).uv(0.0f, (float)this.getY() / 32.0f).color(64, 64, 64, 255).endVertex();
//        bufferBuilder.vertex(this.getX() + this.width, this.getY(), -100.0).uv((float)this.width / 32.0f, (float)this.getY() / 32.0f).color(64, 64, 64, 255).endVertex();
//        bufferBuilder.vertex(this.getX() + this.width, 0.0, -100.0).uv((float)this.width / 32.0f, 0.0f).color(64, 64, 64, 255).endVertex();
//        bufferBuilder.vertex(this.getX(), 0.0, -100.0).uv(0.0f, 0.0f).color(64, 64, 64, 255).endVertex();
//        bufferBuilder.vertex(this.getX(), this.height, -100.0).uv(0.0f, (float)this.height / 32.0f).color(64, 64, 64, 255).endVertex();
//        bufferBuilder.vertex(this.getX() + this.width, this.height, -100.0).uv((float)this.width / 32.0f, (float)this.height / 32.0f).color(64, 64, 64, 255).endVertex();
//        bufferBuilder.vertex(this.getX() + this.width, this.getY() +this. getHeight(), -100.0).uv((float)this.width / 32.0f, (float)this.getY() +this. getHeight() / 32.0f).color(64, 64, 64, 255).endVertex();
//        bufferBuilder.vertex(this.getX(), this.getY() +this. getHeight(), -100.0).uv(0.0f, (float)this.getY() +this. getHeight() / 32.0f).color(64, 64, 64, 255).endVertex();
//        tesselator.end();

        //Render horizontal shadows

//        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        n = 4;

        depth = 0.0;
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(this.getX(), this.getY() + 4, depth).color(0, 0, 0, 0).endVertex();
        bufferBuilder.vertex(this.getX() + this.getWidth(), this.getY() + 4, depth).color(0, 0, 0, 0).endVertex();
        bufferBuilder.vertex(this.getX() + this.getWidth(), this.getY(), depth).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.getX(), this.getY(), depth).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.getX(), this.getY() +this. getHeight(), depth).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.getX() + this.getWidth(), this.getY() + this.getHeight(), depth).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(this.getX() + this.getWidth(), this.getY() + this.getHeight() - 4, depth).color(0, 0, 0, 0).endVertex();
        bufferBuilder.vertex(this.getX(), this.getY() + this.getHeight() - 4, depth).color(0, 0, 0, 0).endVertex();
        tesselator.end();


        //Scroll bar
        int o;
        int n;
        int m;
        if ((o = this.getMaxScroll()) > 0) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            m = (int)((float)((this.getY() +this. getHeight() - this.getY()) * (this.getY() +this. getHeight() - this.getY())) / (float)this.getMaxPosition());
            m = Mth.clamp(m, 32, this.getY() +this. getHeight() - this.getY() - 8);
            n = (int)this.getScrollAmount() * (this.getY() +this. getHeight() - this.getY() - m) / o + this.getY();
            if (n < this.getY()) {
                n = this.getY();
            }
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferBuilder.vertex(i, this.getY() +this. getHeight(), 0.0).color(0, 0, 0, 255).endVertex();
            bufferBuilder.vertex(j, this.getY() +this. getHeight(), 0.0).color(0, 0, 0, 255).endVertex();
            bufferBuilder.vertex(j, this.getY(), 0.0).color(0, 0, 0, 255).endVertex();
            bufferBuilder.vertex(i, this.getY(), 0.0).color(0, 0, 0, 255).endVertex();
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

//        this.renderDecorations(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    public int getRowWidth() {
        return this.width;
    }

    protected int getScrollbarPosition() {
//        return this.width / 2 + 124 + 32;
        return (int) (this.width * 0.95f);
    }

    public Optional<OptionWidget> getHoveredButton(double mouseX, double mouseY) {
        for (VOptionList.Entry buttonEntry : this.children) {
            if (!buttonEntry.widget.isMouseOver(mouseX, mouseY)) continue;
            return Optional.of(buttonEntry.widget);
        }
        return Optional.empty();
    }

    protected void renderList(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float delta) {
        int itemCount = this.getItemCount();

        for (int j = 0; j < itemCount; ++j) {
//            int rowTop = this.getY() + 4 - (int)this.getScrollAmount() + (j * this.itemHeight) + this.headerHeight;
            int rowTop = this.getY() + 4 - (int)this.getScrollAmount() + (j * this.itemHeight);
            int rowBottom = this.getRowBottom(j);

            if (rowBottom < this.getY() || rowTop > this.getY() + this.getHeight())
                continue;

            int n = this.itemHeight - 4;
            VOptionList.Entry entry = this.getEntry(j);
            int rowWidth = this.getRowWidth();

            int rowLeft = this.getRowLeft();
            entry.render(guiGraphics, j, rowTop, rowLeft, rowWidth, n, mouseX, mouseY, false, delta);
        }
    }

    private Entry getEntry(int j) {
        return this.children.get(j);
    }

    public int getRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
    }

    public int getRowRight() {
        return this.getRowLeft() + this.getRowWidth();
    }

    protected int getRowTop(int i) {
        return this.getY() + 4 - (int)this.getScrollAmount() + i * this.itemHeight + this.headerHeight;
    }

    protected int getRowBottom(int i) {
        return this.getRowTop(i) + this.itemHeight;
    }

    protected static class Entry implements GuiEventListener {
        final OptionWidget widget;

        private NarratableEntry focusedSelectable;

        private Entry(OptionWidget widget) {
            this.widget = widget;
        }

        public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            widget.y = y;
            widget.render(guiGraphics, mouseX, mouseY, tickDelta);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
//            Iterator<? extends GuiEventListener> var6 = this.children().iterator();
//
//            GuiEventListener guiEventListener;
//            do {
//                if (!var6.hasNext()) {
//                    return false;
//                }
//
//                guiEventListener = (GuiEventListener)var6.next();
//            } while(!guiEventListener.mouseClicked(d, e, i));
//
//            this.setFocused(guiEventListener);
//            if (i == 0) {
//                this.setDragging(true);
//            }

            widget.mouseClicked(mouseX, mouseY, button);

            return true;
        }

        public boolean mouseReleased(double mouseX, double mouseY, int button) {
//            this.setDragging(false);
//            return this.getChildAt(d, e).filter((guiEventListener) -> {
//                return guiEventListener.mouseReleased(d, e, i);
//            }).isPresent();

            return widget.mouseReleased(mouseX, mouseY, button);
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
//            return this.getFocused() != null && this.isDragging() && i == 0 ? this.getFocused().mouseDragged(d, e, i, f, g) : false;
            return widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }

        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }

        @Override
        public void setFocused(boolean bl) {
            widget.setFocused(bl);
        }

        @Override
        public boolean isFocused() {
            return false;
        }
    }
}
