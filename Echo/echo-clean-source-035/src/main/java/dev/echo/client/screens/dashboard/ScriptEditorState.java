package dev.echo.client.screens.dashboard;

import net.minecraft.client.MinecraftClient;

/** V3's original editor state, with the direct field reads needed by the full renderer. */
public final class ScriptEditorState {
    private String text;
    private String[] lines;
    private int caret;
    private int selectionAnchor = -1;
    private int firstVisibleLine;
    private float displayedFirstLine;
    private float autoScrollRemainder;
    private float characterAdvance = 7.8f;
    private boolean saved = true;

    public ScriptEditorState(String text) {
        this.text = text;
        lines = text.split("\n", -1);
        caret = text.length();
    }

    public String text() { return text; }
    public String[] lines() { return lines; }
    public int caret() { return caret; }
    public int firstVisibleLine() { return firstVisibleLine; }
    public float displayedFirstLine() { return displayedFirstLine; }
    public float characterAdvance() { return characterAdvance; }
    public void setCharacterAdvance(float value) { characterAdvance = value; }
    public void setDisplayedFirstLine(float value) { displayedFirstLine = value; }
    public boolean isSaved() { return saved; }
    public int selectionAnchor() { return selectionAnchor; }
    public void setSelectionAnchor(int value) { selectionAnchor = value; }
    public void setCaret(int value) { caret = value; }
    public void setFirstVisibleLine(int value) { firstVisibleLine = value; }
    public void setSaved(boolean value) { saved = value; }
    public void resetAutoScroll() { autoScrollRemainder = 0.0f; }
    public void replaceText(String value) { text = value; rebuildLines(); }
    public void updateMeasurements(float characterAdvance, float displayedFirstLine) {
        this.characterAdvance = characterAdvance;
        this.displayedFirstLine = displayedFirstLine;
    }

    public boolean hasSelection() { return selectionAnchor >= 0 && selectionAnchor != caret; }
    public int selectionStart() { return Math.min(selectionAnchor, caret); }
    public int selectionEnd() { return Math.max(selectionAnchor, caret); }
    public void clearSelection() { selectionAnchor = -1; }

    public void copySelection() {
        if (hasSelection()) MinecraftClient.getInstance().keyboard.setClipboard(text.substring(selectionStart(), selectionEnd()));
    }

    public void insert(String value) {
        int start = hasSelection() ? selectionStart() : caret;
        int end = hasSelection() ? selectionEnd() : caret;
        text = text.substring(0, start) + value + text.substring(end);
        rebuildLines();
        caret = start + value.length();
        clearSelection();
        saved = false;
        revealCaret();
    }

    public boolean deleteSelection() {
        if (!hasSelection()) return false;
        int start = selectionStart();
        int end = selectionEnd();
        text = text.substring(0, start) + text.substring(end);
        rebuildLines();
        caret = start;
        clearSelection();
        saved = false;
        revealCaret();
        return true;
    }

    public void prepareSelection(boolean extending) {
        if (extending) {
            if (selectionAnchor < 0) selectionAnchor = caret;
        } else clearSelection();
    }

    public void moveHorizontal(int direction, boolean extending, boolean byWord) {
        if (!extending && hasSelection()) {
            caret = direction < 0 ? selectionStart() : selectionEnd();
            clearSelection();
            revealCaret();
            return;
        }
        prepareSelection(extending);
        if (direction < 0 && caret > 0) caret = byWord ? previousWord(caret) : text.offsetByCodePoints(caret, -1);
        else if (direction > 0 && caret < text.length()) caret = byWord ? nextWord(caret) : text.offsetByCodePoints(caret, 1);
        revealCaret();
    }

    public void moveVertical(int direction) {
        int column = caret - lineStart(caret);
        int adjacent;
        if (direction < 0) {
            int start = lineStart(caret);
            if (start == 0) return;
            adjacent = start - 1;
        } else {
            int end = lineEnd(caret);
            if (end >= text.length()) return;
            adjacent = end + 1;
        }
        int start = lineStart(adjacent);
        caret = Math.min(start + column, lineEnd(adjacent));
        revealCaret();
    }

    public int previousWord(int offset) {
        int index = Math.max(0, Math.min(text.length(), offset));
        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) index--;
        if (index > 0 && isWordCharacter(text.charAt(index - 1))) {
            while (index > 0 && isWordCharacter(text.charAt(index - 1))) index--;
        } else if (index > 0) index--;
        return index;
    }

    public int nextWord(int offset) {
        int index = Math.max(0, Math.min(text.length(), offset));
        if (index < text.length() && isWordCharacter(text.charAt(index))) {
            while (index < text.length() && isWordCharacter(text.charAt(index))) index++;
        } else if (index < text.length()) index++;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        return index;
    }

    public static boolean isWordCharacter(char value) { return Character.isLetterOrDigit(value) || value == '_'; }
    public int lineStart(int offset) { return text.lastIndexOf('\n', offset - 1) + 1; }
    public int lineEnd(int offset) {
        int newline = text.indexOf('\n', offset);
        return newline < 0 ? text.length() : newline;
    }
    public int caretLine() {
        int line = 0;
        for (int index = 0; index < caret; index++) if (text.charAt(index) == '\n') line++;
        return line;
    }
    public int caretColumn() { return caret - lineStart(caret); }
    public String linePrefix() { return text.substring(lineStart(caret), caret); }

    public void placeCaret(float mouseX, float mouseY) {
        int line = (int) Math.floor(displayedFirstLine + (mouseY - 108.0f) / 23.0f);
        line = Math.max(0, Math.min(line, lines.length - 1));
        int column = Math.max(0, Math.min(lines[line].length(), Math.round((mouseX - 402.0f) / Math.max(0.01f, characterAdvance))));
        int start = 0;
        for (int index = 0; index < line; index++) start += lines[index].length() + 1;
        caret = Math.min(text.length(), start + column);
    }

    public void autoScroll(float mouseY, float deltaSeconds) {
        float direction;
        float distance;
        if (mouseY < 108.0f) { direction = -1.0f; distance = 108.0f - mouseY; }
        else if (mouseY > 917.0f) { direction = 1.0f; distance = mouseY - 917.0f; }
        else { autoScrollRemainder = 0.0f; return; }
        float speed = 5.0f + Math.min(22.0f, distance / 7.0f);
        autoScrollRemainder += direction * speed * deltaSeconds;
        int wholeLines = (int) autoScrollRemainder;
        if (wholeLines == 0) return;
        int maximum = Math.max(0, lines.length - 35);
        firstVisibleLine = Math.max(0, Math.min(maximum, firstVisibleLine + wholeLines));
        autoScrollRemainder -= wholeLines;
    }

    private void rebuildLines() { lines = text.split("\n", -1); }
    public void revealCaret() {
        int line = caretLine();
        if (line < firstVisibleLine) firstVisibleLine = line;
        else if (line >= firstVisibleLine + 35) firstVisibleLine = line - 35 + 1;
    }

    public void deleteBackward(boolean byWord) {
        if (deleteSelection()) return;
        if (caret > 0) {
            int start = byWord ? previousWord(caret) : text.offsetByCodePoints(caret, -1);
            text = text.substring(0, start) + text.substring(caret);
            rebuildLines();
            caret = start;
            saved = false;
            revealCaret();
        }
    }

    public void deleteForward(boolean byWord) {
        if (deleteSelection()) return;
        if (caret < text.length()) {
            int end = byWord ? nextWord(caret) : text.offsetByCodePoints(caret, 1);
            text = text.substring(0, caret) + text.substring(end);
            rebuildLines();
            saved = false;
        }
    }
}
