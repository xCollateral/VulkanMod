package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.gui.util.GuiConstants;
import net.vulkanmod.config.gui.util.SearchFieldTextModel;
import net.vulkanmod.config.gui.util.dim.Dimension;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.glfw.GLFW;

public class SearchFieldWidget extends AbstractWidget {
    private final SearchFieldTextModel model;
    private long lastTimeBlink = 0;

    public SearchFieldWidget(Dimension<Integer> dim, SearchFieldTextModel model) {
        super(dim);
        this.model = model;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_BLACK, 0.45f);

        GuiRenderer.fill(
                getDim().x(), getDim().y(),
                getDim().xLimit(), getDim().yLimit(),
                backgroundColor);

        GuiRenderer.guiGraphics.enableScissor(
                getDim().x(), getDim().y(),
                getDim().xLimit() - 8, getDim().yLimit());

        renderQuery();
        renderCursor();
        renderSelection();

        GuiRenderer.guiGraphics.disableScissor();
    }

    private void renderQuery() {
        if (!this.isFocused() && model.getQuery().isEmpty()) {
            GuiRenderer.drawString(
                    Component.translatable("vulkanmod.options.searchFieldEmpty"),
                    getDim().x() + 8, getDim().centerY() - 4,
                    GuiConstants.COLOR_GRAY);
        } else {
            GuiRenderer.drawString(
                    Component.literal(model.getQuery()),
                    getDim().x() + 8, getDim().centerY() - 4,
                    GuiConstants.COLOR_WHITE);
        }
    }

    private void renderCursor() {
        if (!this.isFocused() || !isCursorVisible())
            return;

        String visibleQuery = model.getQuery().substring(model.getFirstCharacterIndex());

        int cursorPos = getDim().x() + 8 + GuiRenderer.font.width(visibleQuery.substring(0, model.getCursor()));
        GuiRenderer.drawString(
                Component.literal("_"),
                cursorPos, getDim().centerY() - GuiRenderer.font.lineHeight / 2,
                GuiConstants.COLOR_WHITE);
    }

    private void renderSelection() {
        if (model.getQuery().isEmpty())
            return;

        RenderSystem.enableBlend();

        int selectionStart = model.getSelectionStart() - model.getFirstCharacterIndex();
        int selectionEnd = model.getSelectionEnd() - model.getFirstCharacterIndex();

        if (selectionEnd != selectionStart) {
            int selectionColor = ColorUtil.ARGB.multiplyAlpha(GuiConstants.COLOR_WHITE, 0.45f);

            String visibleQuery = model.getQuery().substring(model.getFirstCharacterIndex());

            int x0 = getDim().x() + 8 + GuiRenderer.font.width(visibleQuery.substring(0, selectionStart));
            int x1 = getDim().x() + 8 + GuiRenderer.font.width(visibleQuery.substring(0, selectionEnd));
            int y0 = getDim().centerY() - GuiRenderer.font.lineHeight / 2;
            int y1 = getDim().centerY() + GuiRenderer.font.lineHeight / 2;

            GuiRenderer.fill(
                    Math.min(Math.min(x0, x1), getDim().xLimit()), Math.min(y0, y1),
                    Math.min(Math.max(x0, x1), getDim().xLimit()), Math.max(y0, y1),
                    selectionColor);
        }
    }

    private boolean isCursorVisible() {
        long timeSinceLastBlink = Util.getMillis() - lastTimeBlink;
        if (timeSinceLastBlink > 800 || timeSinceLastBlink < 400) {
            if (timeSinceLastBlink > 400) {
                lastTimeBlink = Util.getMillis();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.isFocused() && StringUtil.isAllowedChatCharacter(chr)) {
            model.write(Character.toString(chr));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.setFocused(isMouseOver(mouseX, mouseY));

        if (!this.isFocused())
            return false;

        model.setSelecting(Screen.hasShiftDown());

        int offsetX = Mth.floor(mouseX) - getDim().x() - 8;
        String visibleString = GuiRenderer.font.plainSubstrByWidth(model.getQuery().substring(model.getFirstCharacterIndex()), getDim().width() - 16);

        int clickPosition = GuiRenderer.font.plainSubstrByWidth(visibleString, offsetX).length();
        model.setCursor(Math.min(model.getFirstCharacterIndex() + clickPosition, model.getQuery().length()));

        return true;
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isActive() || !this.isFocused()) {
            return false;
        }

        model.setSelecting(Screen.hasShiftDown());
        if (Screen.isSelectAll(keyCode)) {
            model.setCursorToEnd();
            model.setSelectionEnd(0);
            return true;
        } else if (Screen.isCopy(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(model.getSelectedText());
            return true;
        } else if (Screen.isPaste(keyCode)) {
            model.write(Minecraft.getInstance().keyboardHandler.getClipboard());

            return true;
        } else if (Screen.isCut(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(model.getSelectedText());
            model.write("");

            return true;
        } else {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    model.setSelecting(false);
                    model.erase(-1);
                    model.setSelecting(Screen.hasShiftDown());
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    model.setSelecting(false);
                    model.erase(1);
                    model.setSelecting(Screen.hasShiftDown());
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (Screen.hasControlDown()) {
                        model.setCursor(model.getWordSkipPosition(1));
                    } else {
                        model.moveCursor(1);
                    }
                    boolean state = model.getCursor() != model.getLastCursorPosition() && model.getCursor() != model.getQuery().length() + 1;
                    model.setLastCursorPosition(model.getCursor());
                    return state;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    if (Screen.hasControlDown()) {
                        model.setCursor(model.getWordSkipPosition(-1));
                    } else {
                        model.moveCursor(-1);
                    }
                    boolean state = model.getCursor() != model.getLastCursorPosition() && model.getCursor() != 0;
                    model.setLastCursorPosition(model.getCursor());
                    return state;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    model.setCursorToStart();
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    model.setCursorToEnd();
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }
    }
}
