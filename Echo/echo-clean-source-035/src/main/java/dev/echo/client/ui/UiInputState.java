package dev.echo.client.ui;

import dev.echo.client.input.InputKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Original immediate-mode UI event accumulator.
 * Source-complete candidate: key indices, direct targets and branch domains are in needs.json.
 */
public final class UiInputState {
    /** Capacity of the original combined key/button index space. */
    public static final int INPUT_COUNT = InputKey.MOUSE_8.code() + 1;

    private final boolean[] down = new boolean[INPUT_COUNT];
    private final boolean[] pressed = new boolean[INPUT_COUNT];
    private final boolean[] released = new boolean[INPUT_COUNT];
    private final List<KeyStroke> keyEvents = new ArrayList<>();
    private final List<Integer> characters = new ArrayList<>();
    private float mouseX = -Float.MAX_VALUE;
    private float mouseY = -Float.MAX_VALUE;
    private double horizontalScroll;
    private double verticalScroll;

    /** Updates the pointer in screen coordinates. */
    public void setPointer(float x, float y) {
        mouseX = x;
        mouseY = y;
    }

    /** Records transitions without clearing earlier transitions from the same frame. */
    public void setDown(int inputCode, boolean isDown) {
        if (inputCode < 0 || inputCode >= INPUT_COUNT) {
            return;
        }
        if (isDown && !down[inputCode]) {
            pressed[inputCode] = true;
        }
        if (!isDown && down[inputCode]) {
            released[inputCode] = true;
        }
        down[inputCode] = isDown;
    }

    /** Accumulates both scroll axes until the frame is cleared. */
    public void addScroll(double horizontalAmount, double verticalAmount) {
        horizontalScroll += horizontalAmount;
        verticalScroll += verticalAmount;
    }

    /** Appends a key event without deduplication. */
    public void addKeyEvent(int keyCode, int scanCode, int modifiers) {
        keyEvents.add(new KeyStroke(keyCode, scanCode, modifiers));
    }

    /** Appends the original integer character value without narrowing to a Java char. */
    public void addCharacter(int character) {
        characters.add(character);
    }

    /** Returns the last pointer x coordinate. */
    public float mouseX() { return mouseX; }

    /** Returns the last pointer y coordinate. */
    public float mouseY() { return mouseY; }

    /** Reports current held state; invalid indices are false. */
    public boolean isDown(int inputCode) {
        return validInputCode(inputCode) && down[inputCode];
    }

    /** Reports a press edge without consuming it. */
    public boolean wasPressed(int inputCode) {
        return validInputCode(inputCode) && pressed[inputCode];
    }

    /** Reports a release edge without consuming it. */
    public boolean wasReleased(int inputCode) {
        return validInputCode(inputCode) && released[inputCode];
    }

    /** Returns accumulated horizontal scrolling. */
    public double horizontalScroll() { return horizontalScroll; }

    /** Returns accumulated vertical scrolling. */
    public double verticalScroll() { return verticalScroll; }

    /** Returns an unmodifiable live view, not an event snapshot. */
    public List<KeyStroke> keyEvents() {
        return Collections.unmodifiableList(keyEvents);
    }

    /** Returns an unmodifiable live view, not a character snapshot. */
    public List<Integer> characters() {
        return Collections.unmodifiableList(characters);
    }

    /** Clears frame-local input while preserving held keys and pointer coordinates. */
    public void endFrame() {
        Arrays.fill(pressed, false);
        Arrays.fill(released, false);
        horizontalScroll = 0.0;
        verticalScroll = 0.0;
        keyEvents.clear();
        characters.clear();
    }

    private static boolean validInputCode(int inputCode) {
        return inputCode >= 0 && inputCode < INPUT_COUNT;
    }
}
