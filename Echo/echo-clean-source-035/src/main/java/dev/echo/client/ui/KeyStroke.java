package dev.echo.client.ui;

/**
 * One queued key event, retaining the three original integer components.
 * Source-complete candidate: original accessor bindings retain their applicability obligation.
 *
 * @param keyCode logical key code
 * @param scanCode physical scan code
 * @param modifiers active modifier bits
 */
public record KeyStroke(int keyCode, int scanCode, int modifiers) {
}
