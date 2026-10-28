package dev.echo.client.ui;

import dev.echo.client.input.InputKey;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Recovered immediate-mode interaction state, awaiting direct-target/cache applicability.
 * UiInteraction names the existing five-boolean result type. Original root-pop
 * exception strings are retained with their two-stage decoding evidence.
 */
public final class ImmediateUiContext {
    private static final int PRIMARY_BUTTON = InputKey.MOUSE_LEFT.code();
    private final UiInputState input = new UiInputState();
    private final Deque<UiId> ids = new ArrayDeque<>();
    private final Deque<UiRect> clips = new ArrayDeque<>();
    private CanvasMetrics canvas = CanvasMetrics.fit(1920.0f, 1080.0f, 1920.0f, 1080.0f);
    private UiId hoveredId = UiId.NONE;
    private UiId activeId = UiId.NONE;
    private UiId focusedId = UiId.NONE;
    private boolean enabled = true;
    private boolean pressConsumed;
    private boolean releaseConsumed;

    /** Begins a frame with fresh scope stacks and event-consumption flags. */
    public void beginFrame(float screenWidth, float screenHeight, float logicalWidth, float logicalHeight) {
        canvas = CanvasMetrics.fit(screenWidth, screenHeight, logicalWidth, logicalHeight);
        hoveredId = UiId.NONE;
        pressConsumed = false;
        releaseConsumed = false;
        ids.clear();
        ids.push(UiId.ROOT);
        clips.clear();
        clips.push(new UiRect(0.0f, 0.0f, logicalWidth, logicalHeight));
    }

    /** Drops an unhandled release capture, clears transient input and ends both scopes. */
    public void endFrame() {
        if (input.wasReleased(PRIMARY_BUTTON) && !releaseConsumed) {
            activeId = UiId.NONE;
        }
        input.endFrame();
        ids.clear();
        clips.clear();
    }

    /** Builds a scoped widget identity, then evaluates the existing interaction protocol. */
    public UiInteraction interact(CharSequence label, UiRect bounds) {
        return interact(id(label), bounds);
    }

    /**
     * Evaluates hover, held, press, release and click in that original constructor order.
     * Presses are captured by the first eligible submitted widget; later overlapping
     * widgets may update hover but cannot capture the same press. Release consumes
     * capture even when the pointer has left the widget.
     */
    public UiInteraction interact(UiId id, UiRect bounds) {
        if (!enabled || clips.isEmpty()) {
            return UiInteraction.NONE;
        }
        float mouseX = mouseX();
        float mouseY = mouseY();
        UiRect clipped = bounds.intersection(clips.peek());
        boolean hovered = clipped.width() > 0.0f && clipped.height() > 0.0f
                && clipped.contains(mouseX, mouseY);
        if (hovered) {
            hoveredId = id;
        }
        boolean pressed = hovered && input.wasPressed(PRIMARY_BUTTON) && !pressConsumed;
        if (pressed) {
            activeId = id;
            pressConsumed = true;
        }
        boolean held = activeId.equals(id) && input.isDown(PRIMARY_BUTTON);
        boolean released = activeId.equals(id) && input.wasReleased(PRIMARY_BUTTON) && !releaseConsumed;
        boolean clicked = released && hovered;
        if (released) {
            releaseConsumed = true;
            activeId = UiId.NONE;
        }
        return new UiInteraction(hovered, held, pressed, released, clicked);
    }

    /** Derives a child identity from the current scope and a character sequence. */
    public UiId id(CharSequence label) {
        return ids.peek().child(label);
    }

    /** Pushes a character-sequence identity scope. */
    public void pushId(CharSequence label) {
        ids.push(id(label));
    }

    /** Pushes an integer identity scope. */
    public void pushId(int value) {
        ids.push(ids.peek().child(value));
    }

    /** Pops a nested widget scope, retaining the original root-underflow error. */
    public void popId() {
        if (ids.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root widget ID");
        }
        ids.pop();
    }

    /** Pushes a clip intersected with the current logical clip. */
    public void pushClip(UiRect clip) {
        clips.push(clip.intersection(clips.peek()));
    }

    /** Pops a nested clip, retaining the original root-underflow error. */
    public void popClip() {
        if (clips.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root clip");
        }
        clips.pop();
    }

    /** Explicitly assigns focus; mouse capture itself does not assign it. */
    public void focus(UiId id) { focusedId = id; }

    /** Clears focus without clearing active mouse capture. */
    public void clearFocus() { focusedId = UiId.NONE; }

    /** Tests the focused identity. */
    public boolean isFocused(UiId id) { return focusedId.equals(id); }

    /** Tests the last submitted hovered identity. */
    public boolean isHovered(UiId id) { return hoveredId.equals(id); }

    /** Tests the active mouse-capture identity. */
    public boolean isActive(UiId id) { return activeId.equals(id); }

    /** Returns the last submitted hovered identity. */
    public UiId hoveredId() { return hoveredId; }

    /** Returns the current mouse-capture identity. */
    public UiId activeId() { return activeId; }

    /** Returns the current canvas transform. */
    public CanvasMetrics canvas() { return canvas; }

    /** Returns the original event accumulator. */
    public UiInputState input() { return input; }

    /** Returns pointer x in logical canvas coordinates. */
    public float mouseX() { return canvas.toLogicalX(input.mouseX()); }

    /** Returns pointer y in logical canvas coordinates. */
    public float mouseY() { return canvas.toLogicalY(input.mouseY()); }

    /** Resets interaction identities only on the enabled-to-disabled transition. */
    public void setEnabled(boolean enabled) {
        if (this.enabled && !enabled) {
            resetInteraction();
        }
        this.enabled = enabled;
    }

    /** Clears hover, mouse capture, focus and event-consumption flags. */
    public void resetInteraction() {
        hoveredId = UiId.NONE;
        activeId = UiId.NONE;
        focusedId = UiId.NONE;
        pressConsumed = false;
        releaseConsumed = false;
    }

    /** Returns whether widgets may participate in interaction. */
    public boolean enabled() { return enabled; }
}
