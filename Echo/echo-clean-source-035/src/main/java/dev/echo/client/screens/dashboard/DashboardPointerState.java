package dev.echo.client.screens.dashboard;

import dev.echo.client.ui.CanvasMetrics;
import dev.echo.client.ui.UiInputState;

/** The original dashboard's pointer conversion and input event accumulation. */
public final class DashboardPointerState {
    /** Actual controller capture queries, evaluated after pointer/button updates. */
    public interface CaptureState {
        /** Original selectPolicy result, including its overlay transition interval. */
        boolean overlayCapturesInput();
        /** Original selectedQueue modal-open field. */
        boolean modalOpen();
    }

    private final UiInputState input;
    private final CaptureState capture;
    private CanvasMetrics canvas;
    private DashboardTransform transform;
    private float mouseX = -1.0f;
    private float mouseY = -1.0f;

    /** Uses the existing immediate-mode input accumulator and current canvas. */
    public DashboardPointerState(UiInputState input, CanvasMetrics canvas, DashboardTransform transform,
                                 CaptureState capture) {
        this.input = input;
        this.capture = capture;
        this.canvas = canvas;
        this.transform = transform;
    }

    /** Supplies the actual per-frame layout transform; no window scale is guessed. */
    public void updateCanvas(CanvasMetrics canvas, DashboardTransform transform) {
        this.canvas = canvas;
        this.transform = transform;
    }

    /** Converts mouse coordinates independently of the render-only local animation scale. */
    public void move(double x, double y) {
        mouseX = inputX((float) x);
        mouseY = inputY((float) y);
        float screenY = canvas.toScreenY(mouseY);
        float screenX = canvas.toScreenX(mouseX);
        input.setPointer(screenX, screenY);
    }

    /** Updates pointer and button state before deciding whether the event is captured. */
    public boolean press(double x, double y, int button) {
        move(x, y);
        input.setDown(button, true);
        return capture.overlayCapturesInput() || capture.modalOpen() || canvas.contains(mouseX, mouseY);
    }

    /** Records release state even when the pointer has left the canvas. */
    public boolean release(double x, double y, int button) {
        move(x, y);
        input.setDown(button, false);
        return capture.overlayCapturesInput() || capture.modalOpen() || canvas.contains(mouseX, mouseY);
    }

    /** Updates the pointer and reports the original held-state result during a drag. */
    public boolean drag(double x, double y, int button) {
        move(x, y);
        return input.isDown(button);
    }

    /** Accumulates the original vertical-only wheel event. */
    public boolean scroll(double x, double y, double amount) {
        move(x, y);
        input.addScroll(0.0, amount);
        return capture.overlayCapturesInput() || capture.modalOpen() || canvas.contains(mouseX, mouseY);
    }

    /** Appends the key code, scan code and modifiers without collapsing repeated events. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        input.addKeyEvent(keyCode, scanCode, modifiers);
        return true;
    }

    /** Preserves the original full integer character value and unconditional consumption. */
    public boolean characterTyped(int character) {
        input.addCharacter(character);
        return true;
    }

    /** Returns the last transformed logical mouse X. */
    public float mouseX() { return mouseX; }
    /** Returns the last transformed logical mouse Y. */
    public float mouseY() { return mouseY; }

    private float inputX(float x) {
        float center = transform.viewportLeft() + 1920.0f * transform.viewportScale() * 0.5f;
        return 960.0f + (x - center) / Math.max(transform.viewportScale() * transform.zoom(), 1.0e-4f);
    }

    private float inputY(float y) {
        float center = transform.viewportTop() + 1080.0f * transform.viewportScale() * 0.5f
                + transform.verticalOffset() * transform.viewportScale();
        return 540.0f + (y - center) / Math.max(transform.viewportScale() * transform.zoom(), 1.0e-4f);
    }
}
