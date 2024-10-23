package net.vulkanmod.config.gui.util;

import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.vulkanmod.config.gui.GuiRenderer;

import java.util.function.Consumer;

public class SearchFieldTextModel {
    private final StringBuilder query = new StringBuilder();
    private final Consumer<String> updateConsumer;
    private final int innerWidth = 200;
    private boolean selecting;
    private int firstCharacterIndex;
    private int selectionStart;
    private int selectionEnd;
    private int lastCursorPosition = this.getCursor();

    public SearchFieldTextModel(Consumer<String> updateConsumer) {
        this.updateConsumer = updateConsumer;
    }

    public String getQuery() {
        return query.toString();
    }

    public String getSelectedText() {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return query.substring(start, end);
    }

    public void write(String text) {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        int currentWidth = GuiRenderer.font.width(query.toString());
        int remainingWidth = innerWidth - currentWidth + GuiRenderer.font.width(query.substring(start, end));

        String filteredText = StringUtil.filterText(text);
        int textWidth = GuiRenderer.font.width(filteredText);

        if (textWidth > remainingWidth) {
            int cutoffIndex = 0;
            for (int i = 0; i < filteredText.length(); i++) {
                if (GuiRenderer.font.width(filteredText.substring(0, i + 1)) > remainingWidth) {
                    break;
                }
                cutoffIndex = i + 1;
            }
            filteredText = filteredText.substring(0, cutoffIndex);
        }

        query.replace(start, end, filteredText);
        setCursor(start + filteredText.length());
        setSelectionEnd(getCursor());

        updateConsumer.accept(getQuery());
    }


    public void erase(int offset) {
        if (Screen.hasControlDown()) {
            eraseWords(offset);
        } else {
            eraseCharacters(offset);
        }

        updateConsumer.accept(getQuery());
    }

    public void eraseWords(int wordOffset) {
        if (!query.isEmpty()) {
            if (selectionStart != selectionEnd) {
                write("");
            } else {
                eraseCharacters(getWordSkipPosition(wordOffset) - selectionStart);
            }
        }
    }

    public void eraseCharacters(int characterOffset) {
        if (!query.isEmpty()) {
            if (selectionStart != selectionEnd) {
                write("");
            } else {
                int cursorPos = getCursorPosWithOffset(characterOffset);
                int start = Math.min(cursorPos, selectionStart);
                int end = Math.max(cursorPos, selectionStart);
                query.delete(start, end);
                setCursor(start);
            }
        }
    }

    public int getWordSkipPosition(int wordOffset) {
        return getWordSkipPosition(wordOffset, selectionStart);
    }

    public int getWordSkipPosition(int wordOffset, int cursorPosition) {
        int currentPosition = cursorPosition;
        boolean isMovingBackward = wordOffset < 0;
        int numberOfWordsToSkip = Math.abs(wordOffset);

        for (int skippedWords = 0; skippedWords < numberOfWordsToSkip; skippedWords++) {
            if (!isMovingBackward) {
                currentPosition = query.indexOf(" ", currentPosition);
                currentPosition = (currentPosition == -1) ? query.length() : skipSpaces(currentPosition);
            } else {
                currentPosition = skipSpacesBackward(currentPosition);
                while (currentPosition > 0 && query.charAt(currentPosition - 1) != ' ') {
                    --currentPosition;
                }
            }
        }
        return currentPosition;
    }

    private int skipSpaces(int currentPosition) {
        int queryLength = query.length();
        while (currentPosition < queryLength && query.charAt(currentPosition) == ' ') {
            ++currentPosition;
        }
        return currentPosition;
    }

    private int skipSpacesBackward(int currentPosition) {
        while (currentPosition > 0 && query.charAt(currentPosition - 1) == ' ') {
            --currentPosition;
        }
        return currentPosition;
    }

    public int getCursor() {
        return selectionStart;
    }

    public void setCursor(int cursor) {
        setSelectionStart(cursor);
        if (!isSelecting()) {
            setSelectionEnd(selectionStart);
        }
    }

    public void moveCursor(int offset) {
        setCursor(getCursorPosWithOffset(offset));
    }

    private int getCursorPosWithOffset(int offset) {
        return Util.offsetByCodepoints(getQuery(), this.selectionStart, offset);
    }

    public void setCursorToStart() {
        setCursor(0);
    }

    public void setCursorToEnd() {
        setCursor(query.length());
    }

    public boolean isSelecting() {
        return selecting;
    }

    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    public int getLastCursorPosition() {
        return lastCursorPosition;
    }

    public void setLastCursorPosition(int lastCursorPosition) {
        this.lastCursorPosition = lastCursorPosition;
    }

    public int getFirstCharacterIndex() {
        return firstCharacterIndex;
    }

    public int getSelectionStart() {
        return selectionStart;
    }

    public void setSelectionStart(int cursor) {
        selectionStart = Mth.clamp(cursor, 0, query.length());
    }

    public int getSelectionEnd() {
        return selectionEnd;
    }

    public void setSelectionEnd(int index) {
        selectionEnd = Mth.clamp(index, 0, query.length());

        if (firstCharacterIndex > query.length()) {
            firstCharacterIndex = query.length();
        }

        String visibleString = GuiRenderer.font.plainSubstrByWidth(query.substring(firstCharacterIndex), innerWidth);
        int visibleEndIndex = visibleString.length() + firstCharacterIndex;

        if (selectionEnd == firstCharacterIndex) {
            firstCharacterIndex -= GuiRenderer.font.plainSubstrByWidth(getQuery(), innerWidth, true).length();
        }

        if (selectionEnd > visibleEndIndex) {
            firstCharacterIndex += selectionEnd - visibleEndIndex;
        } else if (selectionEnd < firstCharacterIndex) {
            firstCharacterIndex = selectionEnd;
        }

        firstCharacterIndex = Mth.clamp(firstCharacterIndex, 0, query.length());
    }
}